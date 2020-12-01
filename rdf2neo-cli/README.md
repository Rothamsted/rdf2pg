# The RDF-to-Neo4j exporter, Command Line Tool

Please have a look at the [main README](https://github.com/Rothamsted/rdf2neo) for details. 

There are two scripts that can be invoked:

- `tdb2neo.sh`, which takes a Jena TDB as input. 
- `rdf2neo.sh`, which is a simple wrapper that first populates a TDB from RDF files and then invokes `tdb2neo.sh`.

In both cases you need an RDF-to-Cypher configuration, as explained in the [rdf2pg README](../README.md).

Configuration Examples are provided in the `examples/` directory.
