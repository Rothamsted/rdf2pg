package uk.ac.rothamsted.rdf.neo4j.load;

/**
 * The Generic interface to load data into Neo4j, after having SPARQL-fetched them from a Jena TDB triple store. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Jan 2018</dd></dl>
 *
 */
public interface CypherLoader
{
	/**
	 * Loads data into a pre-configured Cypher/Neo4j server. 
	 * 
	 * @param opts is a generic way to pass implementation-specific options 
	 */
	public void load ( String tdbPath, Object... opts );
}