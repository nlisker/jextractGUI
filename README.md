**Test version**

jextractGUI is a GUI wrapper for the [jextract](https://github.com/openjdk/jextract) tool written in [JavaFX](https://github.com/openjdk/jfx).
If offers several benefits over using the command line tool:
* Easy symbol inspection and filtering. No need to dump symbols into an `@argfile` and manually specifying the ones to include.
* A more detailed presentation of the header symbols.
* Working with multiple headers at once, including batch running.
* Syntax validation for arguments (you can never be sure you entered them correctly in the command line).
* Ability to generate jextract run commands for inspection before running, or for copying into a CLI.

![screenshot](doc/screenshot.png)

# Requirements

* You must have the [jextract 22 binaries](https://jdk.java.net/jextract) for your operating system on your `PATH`, i.e., `path/to/jextract/bin`. This is because jextractGUI does not ship with all the binaries needed for each operating system due to size considerations. For example, jextract relies on the [Clang](https://en.wikipedia.org/wiki/Clang) compiler whose binaries are located under the `bin` directory for Windows and the `lib` directory for Linux and MacOs. These are ~100MB each.
* jextractGUI requires the `--enable-preview` JVM argument to run. This is already included in the provided executable below.
* jextractGUI does not require the [`--enable-native-access=<module>`](https://docs.oracle.com/en/java/javase/22/core/restricted-methods.html) JVM argument itself, but an application that uses the FFM Java files produced by jextractGUI might.
* There is no need to have a JDK on your machine to run jextractGUI since it ships with its own runtime image.

# Download

An executable is created here (using jpackage).