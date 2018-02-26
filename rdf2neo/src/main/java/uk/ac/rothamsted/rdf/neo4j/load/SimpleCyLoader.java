package uk.ac.rothamsted.rdf.neo4j.load;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.neo4j.load.support.CyNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.support.RdfDataManager;

/**
 * <h1>The simple Cypher/Neo4j loader</h1> 
 * 
 * <p>
 * 	This works with a single set of SPARQL queries mapping to Cypher node and relation entities.
 * 	The final applications are based on {@link MultiConfigCyLoader}, which allows for defining multiple
 *  queries and deal with different node/relation types separately.
 * </p>  
 *
 * <p><b>WARNING</b>: we assume the target graph database is initially empty. For instances, we send CREATE
 * &lt;node&gt; instructions, without checking if a node already exists.</p>
 * 
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class SimpleCyLoader implements CypherLoader, AutoCloseable
{	
	private CyNodeLoadingProcessor cyNodeLoader;
	private CyRelationLoadingProcessor cyRelationLoader;
		
	private RdfDataManager dataManager = new RdfDataManager ();
	
	private String name;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	

	/**
	 * <p>Uses the {@link #getCyNodeLoader()} and {@link #getCyRelationLoader()} to map data from an RDF/TDB
	 * data source into Cypher entities and load them into a pre-configured Neo4j server.</p> 
	 * 
	 * <p>
	 * opts [ 0 ] = true, means process Cypher nodes.<br/>
	 * opts [ 1 ] = false, means process Cypher relations.<br/>
	 * </p>
	 * 
	 * <p>Nodes are always loaded before relations, this simplify relation DDL creation commands.
	 * 
	 */
	@Override
	public void load ( String tdbPath, Object... opts )
	{		
		try
		{
			RdfDataManager dataMgr = this.getDataManager ();
			CyNodeLoadingProcessor cyNodeLoader = this.getCyNodeLoader ();
			CyRelationLoadingProcessor cyRelLoader = this.getCyRelationLoader ();

			dataManager.open ( tdbPath );
			Dataset ds = dataMgr.getDataSet ();
			
			String[] nameStr = { StringUtils.trimToEmpty ( this.getName () ) };
			if ( !nameStr [ 0 ].isEmpty () ) nameStr [ 0 ] ="[" + nameStr [ 0 ] + "] ";
			
			Txn.executeRead ( ds, () -> 
				log.info ( "{}Sending {} RDF triples to Cypher", nameStr [ 0 ], ds.getDefaultModel ().size () )
			);
			
			// Nodes
			boolean doNodes = opts != null && opts.length > 0 ? (Boolean) opts [ 0 ] : true;
			if ( doNodes ) cyNodeLoader.process ( dataMgr, opts );
	
			// Relations
			boolean doRels = opts != null && opts.length > 1 ? (Boolean) opts [ 1 ] : true;
			if ( doRels ) cyRelLoader.process ( dataMgr, opts );
			
			log.info ( "{}RDF-Cypher conversion finished", nameStr [ 0 ] );
		}
		catch ( Exception ex ) {
			throw new RuntimeException ( "Error while running the RDF/Cypher loader:" + ex.getMessage (), ex );
		}
	}
	
	/**
	 * Closes dependency objects.
	 */
	@Override
	public void close ()
	{
		try
		{
			if ( this.getDataManager () != null ) this.dataManager.close ();
			if ( this.getCyNodeLoader () != null ) this.cyNodeLoader.close ();
			if ( this.getCyRelationLoader () != null ) this.cyRelationLoader.close ();
		}
		catch ( Exception ex ) {
			throw new RuntimeException ( "Internal error while running the Cypher Loader: " + ex.getMessage (), ex );
		}
	}


	/**
	 * The manager to access to the underlining RDF source.
	 */
	public RdfDataManager getDataManager ()
	{
		return dataManager;
	}

	@Autowired
	public void setDataManager ( RdfDataManager dataManager )
	{
		this.dataManager = dataManager;
	}

	/**
	 * Works out the mapping and loading of Cypher nodes. 
	 *  
	 */
	public CyNodeLoadingProcessor getCyNodeLoader ()
	{
		return cyNodeLoader;
	}

	@Autowired
	public void setCyNodeLoader ( CyNodeLoadingProcessor cyNodeLoader )
	{
		this.cyNodeLoader = cyNodeLoader;
	}

	/**
	 * Works out the mapping and loading of Cypher relations.
	 */
	public CyRelationLoadingProcessor getCyRelationLoader ()
	{
		return cyRelationLoader;
	}

	@Autowired
	public void setCyRelationLoader ( CyRelationLoadingProcessor cyRelationLoader )
	{
		this.cyRelationLoader = cyRelationLoader;
	}

	/**
	 * Represents the nodes/relations kind that are loaded by this loader. This is prefixed to logging messages
	 * and is primarily useful when the simple loader is used by {@link MultiConfigCyLoader}. 
	 */
	public String getName ()
	{
		return name;
	}

	@Autowired ( required = false ) @Qualifier ( "defaultLoaderName" )
	public void setName ( String name )
	{
		this.name = name;
	}	
}
