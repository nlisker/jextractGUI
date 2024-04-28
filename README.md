**VERY EARLY VERSION**

jextractGUI is a GUI wrapper for the [jextract](https://github.com/openjdk/jextract) tool written in [JavaFX](https://github.com/openjdk/jfx).
If offers several benefits over using the command line tool:
* Easy symbol inspection and filtering. No need to dump symbols into an `@argfile` and manually specifying the ones to include.
* A more detailed presentation of the header symbols.
* Working with multiple headers at once, including batch running.
* Syntax validation for arguments (the CLI is only strings).
* Ability to output jextract run commands for inspection before running, or for copying into a CLI.

![screenshot](doc/screenshot.png)

## Requirements

jextractGUI does not ship with all the binaries needed for each operating system (like the [Clang](https://en.wikipedia.org/wiki/Clang) compiler) due to size considerations. You must have the [jextract 22 binaries](https://jdk.java.net/jextract) for your operating system available locally, and add the following to the `PATH` env variable:
* Windows: `path\to\jextract\bin`.
* MacOS/Linux: `path/to/jextract/lib`.

## Download

Pre-built executables (using jpackage) for Windows, Linux, and MacOS are available under [Releases](https://github.com/nlisker/jextractGUI/releases).

## Usage

Add to the **Headers** pane the header files for which bindings need to be generated. This can be done by selecting the files in the file chooser dialog (📂), writing the path in the text field (↵), or dragging and dropping the files (directories are searched recursively). They will be passed to jextract and from there to Clang. Any error is passed back to the user.

If a header includes other headers, like `#include <header.h>`, their paths will need to be provided. Clang searches some platform-specific directories automatically, like the `includes` folders of *Visual Studio* and *Windows Kits* on Windows. If the included headers are not found there, you will see a notification saying that the header couldn't be found, but your main header will still be added to the headers list. You can then add the containing directories to the **Includes** paths and click on the "Reload header" (⟳) button of that header. The "Show error" (⚠) button will re-show the notification of the missing header.

The options are unique for each header entry (including the output path), so configure each accordingly. "Help" (?) buttons are available for each option. Selecting a header or one of its sub-entries will show the options for that header. Include symbols (functions, structs, constants...) can be selected using the checkboxes. Clicking on the "Print command" (🖊) button at any time will print the command that will be passed to jextract when the tool is run. This can be used inspect the command before running, or copying it to the command line. Finally, clicking on the "Generate files" (▶) button will run jextract with the specified options. You will be notified of any warnings/errors and successes of the execution.

## Building and running from source

* From Gradle: use the provided Gradle wrapper. The pre-built executables below were created with `jpackageImage`.
* From the IDE: compile with `--enable-preview` and run with `--enable-preview --enable-native-access=org.openjdk.jextract` (these are already configured in Gradle).

IDE developers are welcome to create an integration (e.g., via a plugin) based on this work into their IDE.