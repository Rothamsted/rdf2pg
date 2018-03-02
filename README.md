# The RDF->Neo4J Converter

This is a Java-based project providing configurable components to convert RDF data into Cypher commands that 
can populate a Neo4J graph database.

The core of the project is the [rdf2neo](rdf2neo) library, while [rdf2neo-cli](rdf2neo-cli) module is a command line tool to manage Neo4J imports.

You can configure the way RDF is mapped into Neo4J entities (nodes, node properties, relations and relation properties) by means of SPARQL queries, which are listed in Spring Beans configuration files.

See the [DBPedia example](rdf2neo/src/test/resources/multi_config_indexing.xml) to get a first idea.
See also [integration tests](rdf2neo/src/test/java/uk/ac/rothamsted/rdf/neo4j/load/CypherLoaderIT.java) for programmatic invocation examples. 

 