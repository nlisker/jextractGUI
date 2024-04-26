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

## Building and running from source

* From Gradle: use the provided Gradle wrapper. The Ppre-built executables below were created with `jpackageImage`.
* From the IDE: compile with `--enable-preview` and run with `--enable-preview --enable-native-access=org.openjdk.jextract` (these are already configured in Gradle).

## Download

Pre-built executables (using jpackage) for Windows, Linux, and MacOS are available under [Releases](https://github.com/nlisker/jextractGUI/releases).