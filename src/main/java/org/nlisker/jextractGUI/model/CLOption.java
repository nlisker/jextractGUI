/*
 * Copyright 2026, 2026 Nir Lisker
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

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

/// A command-line option for the run command. Each option can be converted to a [#command] representation.
@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum CLOption {

	MACRO(List.of("--define-macro", "-D"), "<macro>=<value>",
			"""
			Defines a C preprocessor macro, given as a name-value pair.

			Example:
			  jextract CLI: -D A=42
			  jextractGUI: A=42
			Adds a preprocessor macro defining 'A' as 42.
			"""),

	CLASS_NAME(List.of("--header-class-name"), "<name>",
			"""
			The name of the generated header class. If this option is not specified, then the header class name is derived from \
			the header file name.

			Example 1:
			  jextract CLI: --header-class-name MyClass
			  jextractGUI: MyClass
			The class name will be "MyClass".

			Example 2:
			  jextract CLI: <absent>
			  jextractGUI: <empty string>
			A class named "foo_h" will be generated for a header named "foo.h".
			"""),

	PACKAGE_NAME(List.of("--target-package", "-t"), "<package>",
			"""
			The name of the package for the generated classes. If this option is not specified, then the unnamed package is used.

			Example1:
			  jextract CLI: -t com.example.native
			  jextractGUI: com.example.native
			The package name will be "com.example.native".

			Example 2:
			  jextract CLI: <absent>
			  jextractGUI: <empty string>
			The package name will be "" (unnamed package).
			"""),

	INCLUDES_PATH(List.of("--include-dir", "-I"), "<dir>",
			"""
			Appends a directory to the include search paths. Include search paths are searched in order. The specified \
			directories are not searched recursivley.
			To add a single directory, choose it with the select button (📂) or enter its path in the text field (⮠ ).
			To add all directory paths in a directory, recuresively, drag and drop (⭳) the directory into the list.

			Example:
			  jextract CLI: -I usr/home/foo -I usr/home/bar
			  jextractGUI:
			    usr/home/foo
			    usr/home/bar
			Header files will be searched in "usr/home/foo" first, then (if nothing is found) in "usr/home/bar".
			"""),

	LIBRARY_PATH(List.of("--library", "-l"), "<name | path>",
			"""
			Specifies a shared library that should be loaded by the generated header class. If the argument starts with ':', \
			then it is interpreted as a library path. Otherwise, it denotes a library name.
			To add a single library path, use any of these methods:
			  - Drag and drop (⭳) a library file into the list.
			  - Choose a library file with the select button (📂).
			  - Enter a library path, starting with ':', in the text field (⮠ ).
			To add all library paths in a directory, recuresively, drag and drop (⭳) the directory into the list.
			To add a single library name, enter a (valid) library name in the text field (⮠ ).

			Example 1:
			  jextract CLI: -l GL
			  jextractGUI: GL
			Adds a library named "GL".

			Example 2:
			  jextract CLI: -l :libGL.so.1
			  jextractGUI: :libGL.so.1
			Adds a library path "libGL.so.1" (in the current directory).

			Example 3:
			  jextract CLI: -l :/usr/lib/libGL.so
			  jextractGUI: :/usr/lib/libGL.so
			Adds a library path "/usr/lib/libGL.so".
			"""),

	USE_SYSTEM_LOAD_LIBRARIES(List.of("--use-system-load-library"), "",
			"""
			Specifies that libraries specified using -l are loaded in the loader symbol lookup (using either System::loadLibrary, \
			or System::load). Useful if the libraries must be loaded from one of the paths in java.library.path.
			"""),

	OUTPUT_PATH(List.of("--output"), " <path>",
			"""
			Specifies where to place the generated source files.

			Example 1:
			  jextract CLI: --output /usr/dev/native
			  jextractGUI: /usr/dev/native
			The source files will be generated in "/usr/dev/native".

			Example 2:
			  jextract CLI: <absent>
			  jextractGUI: <empty string>
			The source files will be generated in the current directory.
			"""),

	INCLUDE(List.of("--include-[function,constant,struct,union,typedef,var]"), "<String>",
			"""
			Specifies the symbols to inlude in each header file for which bindings need to be generated.
			To add a single header path, choose it with the select button (📂) or enter its path in the text field (⮠ ).
			To add all header paths in a directory, recuresively, drag and drop (⭳) the directory into the list.

			Each such *main header* is parsed separately by clicking on the parse button (🔍). Multiple parsing requests can be \
			queued concurrently.
			If a header includes other headers, as with '#include <header.h>', their paths will need to be provided in the \
			Includes list. Clang searches some platform-specific directories automatically, like the includes folders of Visual \
			Studio and Windows Kits on Windows. If the included headers are not found there, they might be ignored, and a \
			notification might be shown saying that the header couldn't be found, depending on jextract's/clang's behavior.

			Parsed main headers will show all their symbols categorized by headers and kinds (functions, structs, constants...). \
			Select the "Detailed view" checkbox to see details for symbols.	Select the checkboxes of the symbols to include. \
			If all symbols are selected for a main header, no --include-[...] options is used.
			This replaces the need to use dump symbols files and filtering.

			Example 1:
			  jextract CLI: --include-struct Point2d
			  jextractGUI: <ticked Point2d checkbox>
			Only the "Point2D" struct will have bindings generated for it.

			Example 2:
			  jextract CLI: --include-function distance
			  jextractGUI: <ticked distance checkbox>
			Only the "distance" function will have bindings generated for it.

			Example:
			  jextract CLI: <absent>
			  jextractGUI: <ticked main header checkbox (ticks all subitems)>
			All symbols will be included.


			Selecting a main header or one of its sub-entries will show the options (macros, output path...) for that header. \
			These are unique for each main header entry, so each main header should be configured separately.
			Clicking on the "Print command" (🖊) button at any time will print the command that will be passed to jextract when \
			the tool is run. This can be used inspect the command before running, or copying it to the command line. Clicking on \
			the "Generate files" (▶) button will run jextract with the specified options. A notification will be shown with any \
			warnings/errors and successes of the execution.
			""");

	List<String> commands;

	/// The textual format of the argument for the command option. For example, `MACRO` is given as `<macro>=<value>`.
	String argument;

	/// The description of the command option for the user. Closely related to jextract's `--help`.
	String description;

	/// The textual representation of the command option. For example, `OUTPUT_PATH` converts to `--output`.
	public String command() {
		return commands.getFirst();
	}

	@Override
	public String toString() {
		return name().charAt(0) + name().replace("_", " ").substring(1).toLowerCase();
	}
}