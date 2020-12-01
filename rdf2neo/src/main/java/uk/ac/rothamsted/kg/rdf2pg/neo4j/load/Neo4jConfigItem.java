package uk.ac.rothamsted.kg.rdf2pg.neo4j.load;

import uk.ac.rothamsted.kg.rdf2pg.load.ConfigItem;
import uk.ac.rothamsted.kg.rdf2pg.load.SimplePGLoader;

/**
 * TODO: comment me!
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
	public void configureLoader ( SimpleCyLoader cypherLoader )
	{
		super.configureLoader ( cypherLoader );
		if ( indexesSparql == null ) return;
		cypherLoader.getCypherIndexer ().setIndexesSparql ( indexesSparql );
	}
}
