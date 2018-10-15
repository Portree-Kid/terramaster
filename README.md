## TerraMaster
### GUI for managing FlightGear scenery collections

This software makes use of:

1. The Java Map Projection Library
http://www.jhlabs.com/java/maps/proj/
2. JarClassLoader from JDotSoft
http://jdotsoft.com/JarClassLoader.php
3. Free icons from Aha-Soft
http://www.small-icons.com/packs/24x24-free-application-icons.htm
4. The Global Self-consistent, Hierarchical, High-resolution Geography Database (GSHHG)
https://www.ngdc.noaa.gov/mgg/shorelines/gshhs.html
5. FlightGear multiplayer map server for airport & navaid lookup
http://mpmap02.flightgear.org

#### Build instructions:
0. You need Apache Maven to build.
1. Grab the source code.
2. The required libraries are already in the LIB subdirectory.
3. 'maven' to do the work.

#### Run instructions:
1. Start the program using the command "java -jar terramaster.jar"
   (double-clicking the terramaster.jar file may also work)
2. Click the 'Settings' icon to set the path to your FlightGear scenery folder.
3. User preferences are stored in the file "terramaster.properties".
