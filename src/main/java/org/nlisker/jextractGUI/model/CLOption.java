package org.nlisker.jextractGUI.model;

import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum CLOption {

	MACRO(List.of("--define-macro", "-D"), "<macro>=<value>",
			"""
			Defines a C preprocessor macro, given as a name-value pair.

			Example: -D A=42
			"""),

	CLASS_NAME(List.of("--header-class-name"), "<name>",
			"""
			The name of the generated header class. If this option is not specified, then the header class name is derived from \
			the header file name.

			Example: --header-class-name MyClass
			The class name will be "MyClass".
			Example:
			A class named "foo_h" will be generated for a header named "foo.h".
			"""),

	PACKAGE_NAME(List.of("--target-package", "-t"), "<package>",
			"""
			The name of the package for the generated classes. If this option is not specified, then the unnamed package is used.

			Example: -t com.example.native
			The package name will be "com.example.native".
			Example:
			The package name will be "" (unnamed package).
			"""),

	INCLUDES_PATH(List.of("--include-dir", "-I"), "<dir>",
			"""
			Appends a directory to the include search paths. Include search paths are searched in order.

			Example: -I foo -I bar
			Header files will be searched in "foo" first, then (if nothing is found) in "bar".
			"""),

	LIBRARY_PATH(List.of("--library", "-l"), "<name | path>",
			"""
			Specifies a shared library that should be loaded by the generated header class. If the argument starts with :, \
			then it is interpreted as a library path. Otherwise, it denotes a library name.

			Example: -l GL
			GL is a library name.
			Example: -l :libGL.so.1
			libGL.so.1 is a library path.
			Example: -l :/usr/lib/libGL.so
			/usr/lib/libGL.so is a library path.
			"""),

	USE_SYSTEM_LOAD_LIBRARIES(List.of("--use-system-load-library"), "",
			"""
			Specifies that libraries specified using -l are loaded in the loader symbol lookup (using either System::loadLibrary, \
			or System::load). Useful if the libraries must be loaded from one of the paths in java.library.path.
			"""),
	
	OUTPUT_PATH(List.of("--output"), " <path>",
			"""
			Specifies where to place the generated files.

			Example: --output /usr/dev/native
			The classes will be generated in /usr/dev/native.
			Example:
			The classes will be generated in the current directory.
			"""),

	INCLUDE(List.of("--include-[function,constant,struct,union,typedef,var]"), "<String>",
			"""
			Includes a symbol of the given name and kind in the generated bindings. When one of these options is specified, any \
			symbol that is not matched by any specified filters is omitted from the generated bindings.

			Example: --include-struct Point2d
			Only the "Point2D" struct will be included.
			Example: --include-function distance
			Only the "distance" function will be included.
			Example:
			All symbols will be included.
			""");

	List<String> commands;

	String argument;

	String description;
	
	public String command() {
		return commands.get(0);
	}

	@Override
	public String toString() {
		return name().charAt(0) + name().replace("_", " ").substring(1).toLowerCase();
	}
}