package uk.ac.rothamsted.rdf.pg.load;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.neo4j.CyNodeLoadingHandler;
import uk.ac.rothamsted.rdf.pg.load.support.neo4j.CyNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.neo4j.CyRelationLoadingHandler;
import uk.ac.rothamsted.rdf.pg.load.support.neo4j.CyRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.neo4j.CypherIndexer;

/**
 * <h1>The simple Cypher/Neo4j loader</h1> 
 * 
 * <p>
 * 	This works with a single set of SPARQL queries mapping to Cypher node and relation entities.
 * 	The final applications are based on {@link MultiConfigPGLoader}, which allows for defining multiple
 *  queries and deal with different node/relation types separately.
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
	
	/**
	 * 
	 * TODO: move this upwards, introduce key/value options
	 * 
	 * <p>Uses the {@link #getCyNodeLoader()} and {@link #getCyRelationLoader()} to map data from an RDF/TDB
	 * data source into Cypher entities and load them into a pre-configured Neo4j server.</p> 
	 * 
	 * <p>
	 * opts [ 0 ] = true, means process Cypher nodes.<br/>
	 * opts [ 1 ] = true, means process Cypher relations.<br/>
	 * opts [ 2 ] = true, means process Cypher indices (if {@link #getCypherIndexer()} is defined).<br/>
	 * </p>
	 * 
	 * <p>I opts is null, all the three operations above are run in the same sequence.</p>
	 *  
	 * <p>Nodes are always loaded before relations, indices are created the end of all other operations 
	 * this simplify relation DDL creation commands.</p>
	 * 
	 */
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
