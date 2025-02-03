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

import static com.google.common.truth.Truth.*;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.nlisker.jextractGUI.model.Displayable.IncludeKindDeclaration;
import org.nlisker.jextractGUI.model.Displayable.IncludeKindGroup;
import org.nlisker.jextractGUI.model.Displayable.IncludeKindGroup.IncludeKind;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Position;
import org.openjdk.jextract.Type;

@DisplayName("Header command should")
class HeaderCommandTest {

	private static final String HEADER_PATH = "\\path\\to\\test.h";

	private final File headerFile = new File(HEADER_PATH);
	private final Displayable.Header header = new Displayable.Header(headerFile);

	@Test
	void containOnlyHeaderPathWhenHeaderisEmpty() {
		List<String> command = header.createCommandSegments();

		assertThat(command).containsExactly(HEADER_PATH);
	}

	@Test
	void containOutputOptionWhenGiven() {
		var outputPath = "path/to/here";
		header.outputPath().set(outputPath);
		List<String> command = header.createCommandSegments();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.OUTPUT_PATH.command(), outputPath);
	}

	@Test
	void containClassNameOptionWhenGiven() {
		var className = "MyClass";
		header.className().set(className);
		List<String> command = header.createCommandSegments();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.CLASS_NAME.command(), className);
	}

	@Test
	void containPackageNameOptionWhenGiven() {
		var packageName = "com.my.pack";
		header.packageName().set(packageName);
		List<String> command = header.createCommandSegments();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.PACKAGE_NAME.command(), packageName);
	}

	@Test
	void containMacroOptionWhenGiven() {
		var macro = "M=2";
		Macro.fromString(macro).ifPresent(header.macros()::add);
		List<String> command = header.createCommandSegments();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.MACRO.command(), macro);
	}

	@Test
	void containIncludePathOptionWhenGiven() {
		var includePath = "path\\to\\here";
		header.includes().add(new File(includePath));
		List<String> command = header.createCommandSegments();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.INCLUDES_PATH.command(), includePath);
	}

	@Test
	void containLibraryPathOptionWhenGiven() {
		var libraryPath = "path\\to\\here";
		header.libraries().add(new File(libraryPath));
		List<String> command = header.createCommandSegments();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.LIBRARY_PATH.command(), libraryPath);
	}

	@Test
	void testUseSystemLoadLibraries() {
		header.useSystemLoadLibraries().set(true);
		List<String> command = header.createCommandSegments();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.USE_SYSTEM_LOAD_LIBRARIES.command());
	}

	@Nested
	class Includes {

		DummyDeclaration i1Decl = new DummyDeclaration("i1", Declaration.Variable.Kind.FIELD);
		DummyDeclaration i2Decl = new DummyDeclaration("i2", Declaration.Variable.Kind.FIELD);
		IncludeKindDeclaration i1IncludeDecl = new IncludeKindDeclaration(i1Decl);
		IncludeKindDeclaration i2IncludeDecl = new IncludeKindDeclaration(i2Decl);
		IncludeKindGroup includeGroup;

		BooleanProperty inc1 = new SimpleBooleanProperty(true);
		BooleanProperty inc2 = new SimpleBooleanProperty(true);
		BooleanProperty incGroup = new SimpleBooleanProperty(true);
		BooleanProperty reqHeader = new SimpleBooleanProperty(true);

		{
			i1IncludeDecl.included(inc1);
			i2IncludeDecl.included(inc2);

			IncludeKind includeKind = IncludeKind.fromDeclaration(i1Decl);
			includeGroup = new IncludeKindGroup(includeKind, List.of(i1IncludeDecl, i2IncludeDecl));
			includeGroup.included(incGroup);

			header.includeKindGroups().add(includeGroup);
			header.requiresIncludeArgs(reqHeader);
		}

		@Test
		void notIncludeWhenHeaderDoesNotRequireIncludes() {
			reqHeader.set(false);
			List<String> command = header.createCommandSegments();

			assertThat(command).containsExactly(HEADER_PATH);
		}

		@Test
		void notIncludeWhenEverythingIsIncluded() {
			reqHeader.set(false);
			List<String> command = header.createCommandSegments();

			assertThat(command).containsExactly(HEADER_PATH);
		}

		@Test
		void notIncludeWhenGroupIsNotIncluded() {
			incGroup.set(false);
			List<String> command = header.createCommandSegments();

			assertThat(command).containsExactly(HEADER_PATH);
		}

		@Test
		void includeOnlyIncludedDeclarations() {
			inc2.set(false);
			List<String> command = header.createCommandSegments();

			assertThat(command).containsExactly(HEADER_PATH, includeGroup.asOption(), i1IncludeDecl.asOption());
		}

		@RequiredArgsConstructor
		@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
		static class DummyDeclaration implements Declaration.Variable {

			String name;
			Kind kind;

			@Override
			public <R, D> R accept(Visitor<R, D> arg0, D arg1) {
				return null;
			}

			@Override
			public <R extends Record> void addAttribute(R arg0) {}

			@Override
			public Collection<Record> attributes() {
				return null;
			}

			@Override
			public <R extends Record> Optional<R> getAttribute(Class<R> arg0) {
				return Optional.empty();
			}

			@Override
			public String name() {
				return name;
			}

			@Override
			public Position pos() {
				return null;
			}

			@Override
			public Kind kind() {
				return kind;
			}

			@Override
			public Type type() {
				return null;
			}
		}
	}
}