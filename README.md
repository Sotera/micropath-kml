micropath-kml
=============

For creating kml to visualize aggregate micro-path (AMP) output.

#### Pre-requisites

* Java
* Maven

#### Building

From the projects root directory (containing pom.xml) run "mvn install".  This will produce the micropath-kml.jar (found in /target) needed to run the program.

#### Running

To execute the program, you'll need the micropath-kml.jar and your aggregate micro path output as a CSV file.  The command to run it looks like (run from the jars location):

java -jar micropath-kml.jar <latitude resolution> <longitude resolution> <micro path csv file>

The lat/lon resolutions should match the ones used to produce the output.  They can be found in your configuration file that was passed into AMP (e.g. ais.ini).  The other input just needs to be the path to your CSV file containing the AMP output.

#### Results

What will be produced is a kml file with a name that matches your input file. To view, just load it into any program that renders kml data. 
