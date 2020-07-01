package uk.ac.rothamsted.rdf.pg.load.neo4j;

import uk.ac.rothamsted.rdf.pg.load.ConfigItem;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Jun 2020</dd></dl>
 *
 */
 public class Neo4jConfigItem extends ConfigItem
{
	private String indexesSparql;
	
	public Neo4jConfigItem () {}


	/**
	 * @see CypherIndexer#getIndexesSparql(). 
	 */
	public String getIndexesSparql ()
	{
		return indexesSparql;
	}

	public void setIndexesSparql ( String indexesSparql )
	{
		this.indexesSparql = indexesSparql;
	}	
}
