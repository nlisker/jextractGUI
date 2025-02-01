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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.ClangAttributes;

public sealed interface Displayable {

	String simple();

	String detailed();

	String asOption();

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

		public List<String> createCommand() {
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

	/*
	 * Representation of an {@code IncludeKind} used in the 2nd level of the tree. Shown as its name and size.
	 */
	@RequiredArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	final class IncludeKindDisplay implements Displayable {

		IncludeKind includeKind;
		int size;

		@Override
		public String simple() {
			return includeKind.name().toLowerCase();
		}

		@Override
		public String detailed() {
			return simple() + " (" + size + ")";
		}

		@Override
		public String asOption() {
			return includeKind.optionName();
		}
	}

	/*
	 * Representation of a {@code Declaration} used in the 3rd level of the tree. Shown differently for each type.
	 */
	@RequiredArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	final class DeclarationDisplay implements Displayable {

		Declaration declaration;

		@Override
		public String simple() {
			return declaration.name();
		}

		@Override
		public String detailed() {
			return detailedWithoutLocation() + " @" + declaration.pos().line() + ":" + declaration.pos().col();
		}

		private String detailedWithoutLocation() {
			String string = declaration.attributes().stream()
					.filter(ClangAttributes.class::isInstance)
					.map(ClangAttributes.class::cast)
					.flatMap(attr -> attr.attributes().entrySet().stream())
					.map(clangAttr -> clangAttr.getKey())
					.collect(Collectors.joining(" "));
			string += string.isEmpty() ? "" : " ";

			string += switch (declaration) {
				case Declaration.Function f -> f.type().returnType() + " " + f.name() + f.parameters().stream()
						.map(v -> v.type() + " " + v.name())
						.collect(Collectors.joining(", ", "(", ")"));
				case Declaration.Constant c -> c.type() + " " + c.name() + " = " + c.value();
				case Declaration.Variable v -> v.type() + " " + v.name();
				case Declaration.Typedef t -> t.name() + " " + t.type();
				case Declaration.Scoped s -> s.kind() + " " + s.name() + s.members().stream()
						.map(DeclarationDisplay::new)
						.map(DeclarationDisplay::detailedWithoutLocation)
						.collect(Collectors.joining(", ", " { ", " }"));
				default -> throw new IllegalStateException("Cannot get here!");
			};
			return string;
		}

		@Override
		public String asOption() {
			return simple();
		}
	}

	/**
	 * Valid types to use for the {@code --include-[function,constant,struct,union,typedef,var]} option.
	 */
	public enum IncludeKind {

		FUNCTION,
		/*
		 * Macro or enum constant
		 */
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