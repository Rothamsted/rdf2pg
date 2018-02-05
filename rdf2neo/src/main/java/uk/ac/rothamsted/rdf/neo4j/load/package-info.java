/**
 * <h1>The RDF to Neo4j Converter</h1>
 *
 * <p>The core of the RDF/Neo4j converter starts from a Jena TDB database and reads data to be passed to Neo4j 
 * by means of a set of configured SPARQL queries. To populate Neo4j, essentially you need nodes and their attributes, 
 * then relations between nodes and relation attributes. So, for a given configuration,
 * <ul>
 *   <li>there is a SPARQL query to list IRIs of RDF resources that are nodes,</lI>
 *   <li>another query that is parameterised on node IRIs and fetches Cypher label strings (ie, types),</li>
 *   <li>a third query tells the Cypher property names and values to be set for a node. This is parameterised on the 
 *   node IRI as well.</li>
 * </ul>
 * </p>
 * 
 * <p>A similar set of queries defines how to build relations, with one query for relation node ends and relation types, 
 * another for relation properties.</p>
 * 
 * <p>The configuration is managed through Spring and Spring Beans configuration files.</p> 
 * 
 * @author brandizi
 * <dl><dt>Date:</dt><dd>5 Dec 2017</dd></dl>
 *
 */
package uk.ac.rothamsted.rdf.neo4j.load;