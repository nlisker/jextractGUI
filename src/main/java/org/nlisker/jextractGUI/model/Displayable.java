/*
 * Copyright 2024-2026 Nir Lisker
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.ClangAttributes;

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
	@EqualsAndHashCode(of = "path")
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	final class MainHeader implements Displayable {

		/// The header file.
		Path path;

		StringProperty packageName = new SimpleStringProperty("");
		StringProperty className = new SimpleStringProperty("");
		StringProperty outputPath = new SimpleStringProperty("");
		ObservableList<File> includes = FXCollections.observableArrayList();
		ObservableList<File> libraries = FXCollections.observableArrayList();
		ObservableList<Macro> macros = FXCollections.observableArrayList();
		BooleanProperty useSystemLoadLibraries = new SimpleBooleanProperty();

		@Override
		public String simple() {
			return path.getFileName().toString();
		}

		@Override
		public String detailed() {
			return path.toString();
		}

		@Override
		public String asOption() {
			return detailed();
		}

		/// Creates the jextract command as segments, including all the header-specific options like class and package names,
		/// includes, macros etc., but not included symbols.
		public List<String> createCommandOptions() {
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

			return args;
		}
	}

	/// Representation of a header used in the 2nd level of the tree. Shown as its file path.
	record Header(Path path) implements Displayable {

		@Override
		public String simple() {
			return path.getFileName().toString();
		}

		@Override
		public String detailed() {
			return path.toString();
		}

		@Override
		public String asOption() {
			return "";
		}
	}

	/// Valid types to use for the `--include-[function,constant,struct,union,typedef,var]` option. Used in the 3rd level of the
	/// tree. Shown as its name and number of leaves.
	public enum IncludeKind implements Displayable {

		FUNCTION,
		/// Macro or enum constant
		CONSTANT,
		TYPEDEF,
		STRUCT,
		UNION,
		// Includes bitfield
		VAR;

		@Override
		public String simple() {
			return name().toLowerCase();
		}

		@Override
		public String detailed() {
			return simple() /*+ " (" + includeDeclarations.size() + ")"*/;
		}

		@Override
		public String asOption() {
			return optionName();
		}

		private String optionName() {
			return "--include-" + name().toLowerCase();
		}

		public static IncludeKind fromDeclaration(Declaration decl) {
			return switch (decl) {
				case Declaration.Function _ -> FUNCTION;
				case Declaration.Typedef _ -> TYPEDEF;
				case Declaration.Variable _ -> VAR;
				case Declaration.Constant _ -> CONSTANT;
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

	/// Representation of a [Declaration] used in the 4th level of the tree. Shown as its name with additional info.
	record IncludeDeclaration(Declaration declaration) implements Displayable {

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
				case Declaration.Constant c -> addEnumInfo() + c.type() + " " + c.name() + " = " + c.value();
				case Declaration.Variable v -> v.kind() + " " + v.type() + " " + v.name();
				case Declaration.Typedef t -> t.name() + " " + t.type();
				case Declaration.Scoped s -> s.kind() + " " + s.name() + s.members().stream()
						.map(IncludeDeclaration::new)
						.map(IncludeDeclaration::detailedWithoutLocation)
						.collect(joining(", ", " { ", " }"));
				default -> throw new IllegalStateException("Cannot get here!");
			};
		}

		private static final Pattern ENUM_PATTERN = Pattern.compile("(enum .*)\\.");

		private String addEnumInfo() {
			for (var att : declaration.attributes()) {
				Matcher matcher = ENUM_PATTERN.matcher(att.toString());
				if (matcher.find()) {
					return matcher.group(1) + " ";
				}
			}
			return "";
		}

		@Override
		public String asOption() {
			return simple();
		}
	}
}