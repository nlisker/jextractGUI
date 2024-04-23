**Test version**

jextractGUI is a GUI wrapper for the [jextract](https://github.com/openjdk/jextract) tool written in [JavaFX](https://github.com/openjdk/jfx).
If offers several benefits over using the command line tool:
* Easy symbol inspection and filtering. No need to dump symbols into an `@argfile` and manually specifying the ones to include.
* A more detailed presentation of the header symbols.
* Working with multiple headers at once, including batch running.
* Syntax validation for arguments (you can never be sure you entered them correctly in the command line).
* Ability to generate jextract run commands for inspection before running, or for copying into a CLI.

![screenshot](doc/screenshot.png)

# Download

An executable is created here (using jpackage).