**VERY EARLY VERSION**

jextractGUI is a GUI wrapper for the [jextract](https://github.com/openjdk/jextract) tool written in [JavaFX](https://github.com/openjdk/jfx).
If offers several benefits over using the command line tool:
* Symbol inspection and filtering. No need to dump symbols into an `@argfile` and manually specify the ones to include.
* Detailed presentation of the header symbols.
* Automatic command creation for inspection or copying into a CLI/script. The CLI uses error-prone unchecked strings.
* Ease of use when working with multiple headers at once.

![screenshot](doc/screenshot.png)

## Download

Pre-built executables (using [jpackage](https://dev.java/learn/jvm/tool/jpackage/)) for Windows, Linux, and MacOS are available
under [Releases](https://github.com/nlisker/jextractGUI/releases). jextract dependencies are included.

## Building and running from source

* From Gradle: use the provided Gradle wrapper.
  * `compileJava` downloads the jextract dependencies.
  * `run` starts the application.
  * `jpackageImage` builds the executables.
* From the IDE: compile with `--enable-preview` and run with `--enable-preview --enable-native-access=org.openjdk.jextract`.

---

💡Tracing of native calls can be enabled with `-Djextract.trace.downcalls=true` for both the pre-built executables and local builds.

IDE developers are welcome to create an integration (e.g., via a plugin) based on this work into their IDE.