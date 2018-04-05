# The RDF-to-Neo4j exporter, Command Line Tool

Please have a look at the [main README](https://github.com/Rothamsted/rdf2neo) for details. 

The [command line details](https://github.com/Rothamsted/rdf2neo/blob/master/rdf2neo-cli/src/main/java/uk/ac/rothamsted/rdf/neo4j/Rdf2NeoCli.java) can be listed through the 
--help option.

There are two scripts that can be invoked, the main one is 
[tdb2neo.sh](https://github.com/Rothamsted/rdf2neo/blob/master/rdf2neo-cli/src/main/assembly/resources/tdb2neo.sh), 
which takes a Jena TDB as input, the other is 
[rdf2neo.sh](https://github.com/Rothamsted/rdf2neo/blob/master/rdf2neo-cli/src/main/assembly/resources/rdf2neo.sh), 
which is a simple wrapper that first populates a TDB from RDF files and then invokes tdb2neo.sh.
In both cases you need an RDF-to-Cypher configuration, as explained in the README.
Configuration Examples are provided in the
[examples directory](https://github.com/Rothamsted/rdf2neo/blob/master/rdf2neo-cli/src/main/assembly/resources/examples).
