package uk.ac.rothamsted.rdf.pg.load;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.PGNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.PGRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManager;

@Component @Scope ( scopeName = "loadingSession" )
public class SimplePGLoader<N extends PGNodeLoadingProcessor, R extends PGRelationLoadingProcessor> implements PropertyGraphLoader, AutoCloseable
{	
	protected N nodeLoader;
	protected R relationLoader;

	protected RdfDataManager rdfDataManager = new RdfDataManager ();
	
	protected String name;
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	@Override
	public void load ( String tdbPath, Object... opts )
	{		
		try
		{
			RdfDataManager rdfMgr = this.getRdfDataManager ();

			rdfDataManager.open ( tdbPath );
			Dataset ds = rdfMgr.getDataSet ();
			
			String[] nameStr = { StringUtils.trimToEmpty ( this.getName () ) };
			if ( !nameStr [ 0 ].isEmpty () ) nameStr [ 0 ] ="[" + nameStr [ 0 ] + "] ";
			
			Txn.executeRead ( ds, () -> 
				log.info ( "{}Sending {} RDF triples to PG", nameStr [ 0 ], ds.getUnionModel().size () )
			);
			
			// Nodes
			boolean doNodes = opts != null && opts.length > 0 ? (Boolean) opts [ 0 ] : true;
			if ( doNodes ) this.getPGNodeLoader ().process ( rdfMgr, opts );
	
			// Relations
			boolean doRels = opts != null && opts.length > 1 ? (Boolean) opts [ 1 ] : true;
			if ( doRels ) this.getPGRelationLoader ().process ( rdfMgr, opts );

			log.info ( "{}RDF-PG conversion finished", nameStr [ 0 ] );
		}
		catch ( Exception ex ) {
			throw new RuntimeException ( "Error while running the RDF/PG loader:" + ex.getMessage (), ex );
		}
	}
	
	/**
	 * Closes dependency objects. It DOES NOT deal with Neo4j driver closing, since this could be reused 
	 * across multiple instantiations of this class.
	 */
	@Override
	public void close ()
	{
		try
		{
			if ( this.getRdfDataManager () != null ) this.rdfDataManager.close ();
			if ( this.getPGNodeLoader () != null ) this.nodeLoader.close ();
			if ( this.getPGRelationLoader () != null ) this.relationLoader.close ();
		}
		catch ( Exception ex ) {
			throw new RuntimeException ( "Internal error while running the Cypher Loader: " + ex.getMessage (), ex );
		}
	}


	/**
	 * The manager to access to the underlining RDF source.
	 */
	public RdfDataManager getRdfDataManager ()
	{
		return rdfDataManager;
	}

	@Autowired
	public void setRdfDataManager ( RdfDataManager rdfDataManager )
	{
		this.rdfDataManager = rdfDataManager;
	}

	/**
	 * Works out the mapping and loading of nodes. 
	 *  
	 */
	public N getPGNodeLoader ()
	{
		return nodeLoader;
	}

	@Autowired
	public void setPGNodeLoader ( N nodeLoader )
	{
		this.nodeLoader = nodeLoader;
	}

	/**
	 * Works out the mapping and loading of relations.
	 */
	public R getPGRelationLoader ()
	{
		return relationLoader;
	}

	@Autowired
	public void setPGRelationLoader ( R relationLoader )
	{
		this.relationLoader = relationLoader;
	}
		
	/**
	 * Represents the nodes/relations kind that are loaded by this loader. This is prefixed to logging messages
	 * and is primarily useful when the simple loader is used by {@link MultiConfigPGLoader}. 
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
