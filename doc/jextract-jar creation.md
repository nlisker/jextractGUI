To create the jextract.jar from the binaries on Windows:
1. For ease of use:
  `cd "c:\Program Files\java"`
1. Extract the modules from the image with `jimage` into a `jar` subdirectory:
  `jdk-23\bin\jimage extract --dir=jextract-22\jar jextract-22\runtime\lib\modules`
2. Create the jar from the module with `jar`:
  `jdk-23\bin\jar cfM jextract-22\jar\jextract.jar -C jextract-22\jar\org.openjdk.jextract/ .`
    * `c` creates a jar
    * `M` needed so 'jar' does not create its own manifest, overriding the existing one
    * `f` defines the filename (and dir) of the jar
    * `-C` cd's into the directory
    * `.` add all files in the directory to the jar