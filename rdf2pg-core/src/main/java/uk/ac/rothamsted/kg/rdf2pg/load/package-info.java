/**
 * <h1>The RDF to PropertyGraph Converter</h1>
 *
 * <p>The core of the RDF/PropertyGraph converter starts from a Jena TDB database and reads data to be converted into a property graph 
 * by means of a set of configured SPARQL queries. To populate the property graph, essentially you need nodes and their attributes, 
 * then relations between nodes and relation attributes. So, for a given configuration,
 * <ul>
 *   <li>there is a SPARQL query to list IRIs of RDF resources that are nodes,</lI>
 *   <li>another query that is parameterised on node IRIs and fetches PG label strings (ie, types),</li>
 *   <li>a third query tells the PG property names and values to be set for a node. This is parameterised on the 
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
 * Modifified by cbobed for refactoring purposes  
 * <dl><dt>Date:</dt><dd>28 Apr 2020</dd></dl>
 */
package uk.ac.rothamsted.kg.rdf2pg.load;