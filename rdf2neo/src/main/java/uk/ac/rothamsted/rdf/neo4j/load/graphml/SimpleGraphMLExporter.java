package uk.ac.rothamsted.rdf.neo4j.load.graphml;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.neo4j.load.CypherLoader;
import uk.ac.rothamsted.rdf.neo4j.load.support.RdfDataManager;
import uk.ac.rothamsted.rdf.neo4j.load.support.graphml.GraphMLNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.support.graphml.GraphMLRelationLoadingProcessor;

@Component @Scope ( scopeName = "loadingSession" )
public class SimpleGraphMLExporter implements CypherLoader, AutoCloseable
{	
	private GraphMLNodeLoadingProcessor graphMLNodeLoader;
	private GraphMLRelationLoadingProcessor graphMLRelationLoader;
	
	private RdfDataManager rdfDataManager = new RdfDataManager ();
	
	private String name;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	

	/**
	 * <p>Uses the {@link #getGraphMLNodeLoader()} and {@link #getGraphMLRelationLoader()} to map data from an RDF/TDB
	 * data source into GraphML entities and write the graphML file.</p> 
	 * 
	 * <p>
	 * opts [ 0 ] = true, means process nodes.<br/>
	 * opts [ 1 ] = true, means process relations.<br/>
	 * </p>
	 * 
	 * <p>I opts is null, all the two operations above are run in the same sequence.</p>
	 *  
	 * <p>Nodes are always loaded before relations, indices are created the end of all other operations 
	 * this simplify relation DDL creation commands.</p>
	 * 
	 */
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
				log.info ( "{}Sending {} RDF triples to GraphML", nameStr [ 0 ], ds.getUnionModel().size () )
			);
			
			// Nodes
			boolean doNodes = opts != null && opts.length > 0 ? (Boolean) opts [ 0 ] : true;
			if ( doNodes ) this.getGraphMLNodeLoader ().process ( rdfMgr, opts );
	
			// Relations
			boolean doRels = opts != null && opts.length > 1 ? (Boolean) opts [ 1 ] : true;
			if ( doRels ) this.getGraphMLRelationLoader ().process ( rdfMgr, opts );

			
			// User-defined indices
			boolean doIdx = opts != null && opts.length > 2 ? (Boolean) opts [ 2 ] : true;

			if ( doIdx ) {
				log.info("{}Indexing not available for this exporter");
			}
			
			log.info ( "{}RDF-Cypher conversion finished", nameStr [ 0 ] );
		}
		catch ( Exception ex ) {
			throw new RuntimeException ( "Error while running the RDF/Cypher loader:" + ex.getMessage (), ex );
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
			if ( this.getGraphMLNodeLoader () != null ) this.graphMLNodeLoader.close ();
			if ( this.getGraphMLRelationLoader () != null ) this.graphMLRelationLoader.close ();
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
	public GraphMLNodeLoadingProcessor getGraphMLNodeLoader ()
	{
		return graphMLNodeLoader;
	}

	@Autowired
	public void setGraphMLNodeLoader ( GraphMLNodeLoadingProcessor graphMLNodeLoader )
	{
		this.graphMLNodeLoader = graphMLNodeLoader;
	}

	/**
	 * Works out the mapping and loading of relations.
	 */
	public GraphMLRelationLoadingProcessor getGraphMLRelationLoader ()
	{
		return graphMLRelationLoader;
	}

	@Autowired
	public void setGraphMLRelationLoader ( GraphMLRelationLoadingProcessor graphMLRelationLoader )
	{
		this.graphMLRelationLoader = graphMLRelationLoader;
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
