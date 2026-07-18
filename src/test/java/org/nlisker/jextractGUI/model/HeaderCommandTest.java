/*
 * Copyright 2026 Nir Lisker
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
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.nlisker.jextractGUI.ReplaceCamelCase;
import org.nlisker.jextractGUI.model.Displayable.MainHeader;

/// Tests [MainHeader#createCommandOptions()]. Includes are tested in [ExtractorTest].
@DisplayNameGeneration(ReplaceCamelCase.class)
@TestInstance(Lifecycle.PER_METHOD)
class HeaderCommandTest {

	private static final String HEADER_PATH = "\\path\\to\\test.h";

	private final MainHeader header = new MainHeader(Path.of(HEADER_PATH));

	@Test
	void containOnlyHeaderPathWhenHeaderIsEmpty() {
		List<String> command = header.createCommandOptions();

		assertThat(command).containsExactly(HEADER_PATH);
	}

	@Test
	void containOutputOptionWhenGiven() {
		var outputPath = "path/to/here";
		header.outputPath().set(outputPath);

		List<String> command = header.createCommandOptions();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.OUTPUT_PATH.command(), outputPath);
	}

	@Test
	void containClassNameOptionWhenGiven() {
		var className = "MyClass";
		header.className().set(className);

		List<String> command = header.createCommandOptions();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.CLASS_NAME.command(), className);
	}

	@Test
	void containPackageNameOptionWhenGiven() {
		var packageName = "com.my.pack";
		header.packageName().set(packageName);

		List<String> command = header.createCommandOptions();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.PACKAGE_NAME.command(), packageName);
	}

	@Test
	void containMacroOptionWhenGiven() {
		var macro = "M=2";
		Macro.fromString(macro).ifPresent(header.macros()::add);

		List<String> command = header.createCommandOptions();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.MACRO.command(), macro);
	}

	@Test
	void containIncludePathOptionWhenGiven() {
		var includePath = "path\\to\\here";
		header.includes().add(new File(includePath));

		List<String> command = header.createCommandOptions();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.INCLUDES_PATH.command(), includePath);
	}

	@Test
	void containLibraryPathOptionWhenGiven() {
		var libraryPath = "path\\to\\here";
		header.libraries().add(new File(libraryPath));

		List<String> command = header.createCommandOptions();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.LIBRARY_PATH.command(), libraryPath);
	}

	@Test
	void useSystemLoadLibrariesWhenSpecified() {
		header.useSystemLoadLibraries().set(true);
		List<String> command = header.createCommandOptions();

		assertThat(command).containsExactly(HEADER_PATH, CLOption.USE_SYSTEM_LOAD_LIBRARIES.command());
	}
}