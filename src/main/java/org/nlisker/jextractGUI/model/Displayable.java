/*
 * Copyright 2024 Nir Lisker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nlisker.jextractGUI.model;

import static java.util.stream.Collectors.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

import org.nlisker.jextractGUI.model.Displayable.IncludeKindGroup.IncludeKind;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.ClangAttributes;
import org.openjdk.jextract.Declaration.Scoped;

/// An item to be displayed in the symbols tree. It can be a header, a type under a header, or a declaration under a type.
/// Each item can be converted to a part of the run command using [#asOption]. For example:
/// ```
/// my/headers/header.h -> "my/headers/header.h"
///   ☑ var            -> "--include-var"
///     ☑ i            -> "i"
///     ☐ u
/// ```
/// converts to
/// `my/headers/header.h "--include-var i"`.
public sealed interface Displayable {

	/// {@return the simple textual representation of the `Displayable`}
	String simple();

	/// {@return the detailed textual representation of the `Displayable`}
	String detailed();

	/// {@return the command segment representing the `Displayable`}
	String asOption();

	/// Representation of a header used in the 1st level of the tree. Shown as its file path.
	@Getter
	@Accessors(fluent = true)
	@RequiredArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	final class Header implements Displayable {

		File file;

		StringProperty packageName = new SimpleStringProperty("");
		StringProperty className = new SimpleStringProperty("");
		StringProperty outputPath = new SimpleStringProperty("");
		ObservableList<File> includes = FXCollections.observableArrayList();
		ObservableList<File> libraries = FXCollections.observableArrayList();
		ObservableList<Macro> macros = FXCollections.observableArrayList();
		BooleanProperty useSystemLoadLibraries = new SimpleBooleanProperty();

		List<IncludeKindGroup> includeKindGroups = new ArrayList<>();

		@Setter
		@NonFinal
		ObservableBooleanValue requiresIncludeArgs;

		public void populate(Scoped header) {
			header.members().stream()
					.collect(groupingBy(IncludeKind::fromDeclaration, mapping(IncludeKindDeclaration::new, toList())))
					.forEach((includeKind, includeKindDeclarations) ->
							includeKindGroups.add(new IncludeKindGroup(includeKind, includeKindDeclarations)));
		}

		@Override
		public String simple() {
			return file.getName().toString();
		}

		@Override
		public String detailed() {
			return file.toString();
		}

		@Override
		public String asOption() {
			return detailed();
		}

		/// Creates the jextract command a text, ready to be passed to it. Spaced segments are wrapped in quotes.
		public String createCommandText() {
			return createCommandSegments().stream()
					.map(s -> s.contains(" ") ? "\"" + s + "\"" : s)
					.collect(joining(" "));
		}

		/// Creates the jextract command as segments, including all the header-specific options like class and package names,
		/// includes, macros etc.
		public List<String> createCommandSegments() {
			var args = new ArrayList<String>();

			args.add(asOption());
			if (!packageName.get().isBlank()) {
				args.add(CLOption.PACKAGE_NAME.command());
				args.add(packageName.get());
			}
			if (!className.get().isBlank()) {
				args.add(CLOption.CLASS_NAME.command());
				args.add(className.get());
			}
			includes.forEach(p -> {
				args.add(CLOption.INCLUDES_PATH.command());
				args.add(p.toString());
			});
			libraries.forEach(p -> {
				args.add(CLOption.LIBRARY_PATH.command());
				args.add(p.toString());
			});
			macros.forEach(m -> {
				args.add(CLOption.MACRO.command());
				args.add(m.toString());
			});
			if (useSystemLoadLibraries.get()) {
				args.add(CLOption.USE_SYSTEM_LOAD_LIBRARIES.command());
			}
			if (!outputPath.get().isBlank()) {
				args.add(CLOption.OUTPUT_PATH.command());
				args.add(outputPath.get());
			}

			if (requiresIncludeArgs() != null && requiresIncludeArgs().get()) {
				addIncludesArgs(args);
			}

			return args;
		}

		/// Adds the [#IncludeKind] arguments.
		private void addIncludesArgs(List<String> args) {
			for (var kindGroup : includeKindGroups()) {
				if (kindGroup.included().get()) {
					for (var kindDecl : kindGroup.includeDeclarations()) {
						if (kindDecl.included().get()) {
							args.add(kindGroup.asOption());
							args.add(kindDecl.asOption());
						}
					}
				}
			}
		}
	}

	/// Representation of an [IncludeKind] used in the 2nd level of the tree. Shown as its name and number of leaves.
	@Accessors(fluent = true)
	@RequiredArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	final class IncludeKindGroup implements Displayable {

		IncludeKind includeKind;

		@Getter
		List<IncludeKindDeclaration> includeDeclarations;

		@Getter
		@Setter
		@NonFinal
		ObservableBooleanValue included;

		@Override
		public String simple() {
			return includeKind.name().toLowerCase();
		}

		@Override
		public String detailed() {
			return simple() + " (" + includeDeclarations.size() + ")";
		}

		@Override
		public String asOption() {
			return includeKind.optionName();
		}

		/// Valid types to use for the `--include-[function,constant,struct,union,typedef,var]` option.
		enum IncludeKind {

			FUNCTION,
			/// Macro or enum constant
			CONSTANT,
			TYPEDEF,
			STRUCT,
			UNION,
			VAR;

			private String optionName() {
				return "--include-" + name().toLowerCase();
			}

			public static IncludeKind fromDeclaration(Declaration decl) {
				return switch (decl) {
					case Declaration.Function _ -> FUNCTION;
					case Declaration.Typedef _ -> TYPEDEF;
					case Declaration.Variable _ -> VAR;
					case Declaration.Constant _ -> CONSTANT;
//					case Declaration.Bitfield _ -> ?; // supported?
					case Declaration.Scoped scoped -> fromScoped(scoped);
					default -> throw new IllegalArgumentException("Unsupported Declaration: " + decl.toString());
				};
			}

			private static IncludeKind fromScoped(Declaration.Scoped scoped) {
				return switch (scoped.kind()) {
					case STRUCT -> STRUCT;
					case UNION -> UNION;
					case ENUM -> CONSTANT;
					case BITFIELDS -> throw new IllegalArgumentException("BITFIELDS not supported");
					case TOPLEVEL -> throw new IllegalArgumentException("TOPLEVEL can't be nested");
				};
			}
		}
	}

	/// Representation of a [Declaration] used in the 3rd level of the tree. Shown as its name with additional info.
	@RequiredArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	final class IncludeKindDeclaration implements Displayable {

		Declaration declaration;

		@Getter
		@Setter
		@Accessors(fluent = true)
		@NonFinal
		ObservableBooleanValue included;

		@Override
		public String simple() {
			return declaration.name();
		}

		@Override
		public String detailed() {
			return detailedWithoutLocation() + " @" + declaration.pos().line() + ":" + declaration.pos().col();
		}

		private String detailedWithoutLocation() {
			String string = createAttributesString();
			string += string.isEmpty() ? "" : " ";
			string += createDelarationString();
			return string;
		}

		private String createAttributesString() {
			return declaration.attributes().stream()
					.filter(ClangAttributes.class::isInstance)
					.map(ClangAttributes.class::cast)
					.flatMap(attr -> attr.attributes().entrySet().stream())
					.map(Entry::getKey)
					.collect(joining(" "));
		}

		private String createDelarationString() {
			return switch (declaration) {
				case Declaration.Function f -> f.type().returnType() + " " + f.name() + f.parameters().stream()
						.map(v -> v.type() + " " + v.name())
						.collect(joining(", ", "(", ")"));
				case Declaration.Constant c -> c.type() + " " + c.name() + " = " + c.value();
				case Declaration.Variable v -> v.type() + " " + v.name();
				case Declaration.Typedef t -> t.name() + " " + t.type();
				case Declaration.Scoped s -> s.kind() + " " + s.name() + s.members().stream()
						.map(IncludeKindDeclaration::new)
						.map(IncludeKindDeclaration::detailedWithoutLocation)
						.collect(joining(", ", " { ", " }"));
				default -> throw new IllegalStateException("Cannot get here!");
			};
		}

		@Override
		public String asOption() {
			return simple();
		}
	}
}