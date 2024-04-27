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
package org.nlisker.jextractGUI;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.nlisker.jextractGUI.model.Displayable;
import org.nlisker.jextractGUI.model.Macro;

public class CommandTest {

	private static final String HEADER_PATH = "\\path\\to\\test.h";
	private final Displayable.Header header = new Displayable.Header(new File(HEADER_PATH));

	@Test
	public void testHeaderPath() {
		List<String> command = header.createCommand();
		assertEquals(1, command.size());
		assertEquals(HEADER_PATH, command.get(0));
	}

	@Test
	public void testOutputPath() {
		String arg = "path/to/here";
		header.outputPath().set(arg);
		List<String> command = header.createCommand();
		assertEquals(3, command.size());
		assertEquals("--output", command.get(1));
		assertEquals(arg, command.get(2));
	}

	@Test
	public void testClassName() {
		String arg = "MyClass";
		header.className().set(arg);
		List<String> command = header.createCommand();
		assertEquals(3, command.size());
		assertEquals("--header-class-name", command.get(1));
		assertEquals(arg, command.get(2));
	}

	@Test
	public void testPackageName() {
		String arg = "com.my.pack";
		header.packageName().set(arg);
		List<String> command = header.createCommand();
		assertEquals(3, command.size());
		assertEquals("--target-package", command.get(1));
		assertEquals(arg, command.get(2));
	}

	@Test
	public void testMacro() {
		String arg = "M=2";
		Macro.fromString(arg).ifPresent(header.macros()::add);
		List<String> command = header.createCommand();
		assertEquals(3, command.size());
		assertEquals("--define-macro", command.get(1));
		assertEquals(arg, command.get(2));
	}

	@Test
	public void testIncludePath() {
		String arg = "path\\to\\here";
		header.includes().add(new File(arg));
		List<String> command = header.createCommand();
		assertEquals(3, command.size());
		assertEquals("--include-dir", command.get(1));
		assertEquals(arg, command.get(2));
	}

	@Test
	public void testLibraryPath() {
		String arg = "path\\to\\here";
		header.libraries().add(new File(arg));
		List<String> command = header.createCommand();
		assertEquals(3, command.size());
		assertEquals("--library", command.get(1));
		assertEquals(arg, command.get(2));
	}

	@Test
	public void testUseSystemLoadLibraries() {
		header.useSystemLoadLibraries().set(true);
		List<String> command = header.createCommand();
		assertEquals(2, command.size());
		assertEquals("--use-system-load-library", command.get(1));
	}

//	@Test
//	public void testInclude() {
//	}
}