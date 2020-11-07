package uk.ac.rothamsted.rdf.pg.load.neo4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.MultiConfigPGLoader;
import uk.ac.rothamsted.rdf.pg.load.SimplePGLoader;
import uk.ac.rothamsted.rdf.pg.load.support.neo4j.CyNodeLoadingHandler;
import uk.ac.rothamsted.rdf.pg.load.support.neo4j.CyNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.neo4j.CyRelationLoadingHandler;
import uk.ac.rothamsted.rdf.pg.load.support.neo4j.CyRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.neo4j.CypherIndexer;

/**
 * <h1>The simple Cypher/Neo4j loader</h1> 
 * 
 * <p>
 * 	This maps to Cypher queries that create node and relation entities.
 * </p>  
 *
 * <p><b>WARNING</b>: we assume the target graph database is initially empty. For instance, we send CREATE
 * &lt;node&gt; instructions, without checking if a node already exists.</p>
 * 
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class SimpleCyLoader extends
  SimplePGLoader<CyNodeLoadingHandler, CyRelationLoadingHandler, CyNodeLoadingProcessor, CyRelationLoadingProcessor>
{	
	private CypherIndexer cypherIndexer;
	
	@Override
	protected void loadBody ( String tdbPath, Object... opts )
	{		
		try
		{
			super.loadBody ( tdbPath, opts );
			
			// User-defined indices
			boolean doIdx = opts != null && opts.length > 2 ? (Boolean) opts [ 2 ] : true;

			if ( doIdx ) {
				CypherIndexer indexer = this.getCypherIndexer ();
				if ( indexer != null ) indexer.index ();
			}
		}
		catch ( Exception ex ) {
			throw new RuntimeException ( "Error while running the RDF/Cypher loader:" + ex.getMessage (), ex );
		}
	}
	
	
	public CypherIndexer getCypherIndexer ()
	{
		return cypherIndexer;
	}

	@Autowired
	public void setCypherIndexer ( CypherIndexer cypherIndexer )
	{
		this.cypherIndexer = cypherIndexer;
	}
}