package uk.ac.rothamsted.kg.rdf2pg.neo4j.load;

import uk.ac.rothamsted.kg.rdf2pg.pgmaker.ConfigItem;

/**
 * Extends the parent with the indexing stuff.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Jun 2020</dd></dl>
 *
 */
 public class Neo4jConfigItem extends ConfigItem<SimpleCyLoader>
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


	@Override
	public void configureMaker ( SimpleCyLoader cypherLoader )
	{
		super.configureMaker ( cypherLoader );
		if ( this.indexesSparql == null ) return;
		cypherLoader.getCypherIndexer ().setIndexesSparql ( indexesSparql );
	}
}
