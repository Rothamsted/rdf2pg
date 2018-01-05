package uk.ac.rothamsted.rdf.neo4j.load;

import java.util.concurrent.ThreadPoolExecutor;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.marcobrandizi.rdfutils.jena.elt.RDFProcessor;
import info.marcobrandizi.rdfutils.jena.elt.TDBLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.CyNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.CyRelationLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.CyRelationLoadingProcessor;
import uk.ac.ebi.utils.threading.HackedBlockingQueue;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.CyNodeLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.NeoDataManager;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 *
 */
public class CypherLoader<RS>
{
	private String nodeIrisSparql;
	
	private RDFProcessor<RS> rdfProcessor;
	private long cypherChunkSize = 5000;
	
	private CyNodeLoadingHandler cyNodeLoadingHandler = new CyNodeLoadingHandler ();
	private CyRelationLoadingHandler cyRelationLoadingHandler = new CyRelationLoadingHandler ();
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );

	public void process ( RS rdfSource, Object... opts )
	{
		log.info ( "Collecting RDF input" );
				
		try ( NeoDataManager dataMgr = new NeoDataManager (); )
		{
			TDBLoadingHandler tdbHandler = new TDBLoadingHandler ();			
			tdbHandler.setDataSet ( dataMgr.getDataSet () );
			
			// TDB serialises all writings, so it's not worth to have many threads
			rdfProcessor.setExecutor ( HackedBlockingQueue.createExecutor ( 1, 1 ) );
			rdfProcessor.setConsumer ( tdbHandler );
			rdfProcessor.process ( rdfSource, opts );
			
			Dataset ds = dataMgr.getDataSet ();
			Txn.executeRead ( ds, () -> 
				log.info ( "Sending {} RDF triples to Cypher", ds.getDefaultModel ().size () )
			);
			
			// Nodes
			this.cyNodeLoadingHandler.setDataMgr ( dataMgr );
			CyNodeLoadingProcessor cyNodeLoader = new CyNodeLoadingProcessor ();
			cyNodeLoader.setNodeIrisSparql ( this.nodeIrisSparql );
			cyNodeLoader.setDestinationMaxSize ( this.cypherChunkSize );
			cyNodeLoader.setConsumer ( this.cyNodeLoadingHandler );
			cyNodeLoader.process ( dataMgr );

			// Relations
			this.cyRelationLoadingHandler.setDataMgr ( dataMgr );
			CyRelationLoadingProcessor cyRelLoader = new CyRelationLoadingProcessor ();
			cyRelLoader.setRelationIrisSparql ( this.cyRelationLoadingHandler.getRelationTypesSparql () );
			cyRelLoader.setDestinationMaxSize ( this.cypherChunkSize );
			cyRelLoader.setConsumer ( this.cyRelationLoadingHandler );
			cyRelLoader.process ( dataMgr );
			
			log.info ( "RDF-Cypher conversion finished" );
		}
	}

		
	public String getNodeIrisSparql ()
	{
		return nodeIrisSparql;
	}

	public void setNodeIrisSparql ( String nodeIrisSparql )
	{
		this.nodeIrisSparql = nodeIrisSparql;
	}

	
	public RDFProcessor<RS> getRdfProcessor ()
	{
		return rdfProcessor;
	}

	public void setRdfProcessor ( RDFProcessor<RS> rdfProcessor )
	{
		this.rdfProcessor = rdfProcessor;
	}

	public CyNodeLoadingHandler getCyNodeLoadingHandler ()
	{
		return cyNodeLoadingHandler;
	}

	public void setCyNodeLoadingHandler ( CyNodeLoadingHandler cyNodeLoadingHandler )
	{
		this.cyNodeLoadingHandler = cyNodeLoadingHandler;
	}
	
	
	public CyRelationLoadingHandler getCyRelationLoadingHandler ()
	{
		return cyRelationLoadingHandler;
	}

	public void setCyRelationLoadingHandler ( CyRelationLoadingHandler cyRelationLoadingHandler )
	{
		this.cyRelationLoadingHandler = cyRelationLoadingHandler;
	}


	public long getRDFChunkSize ()
	{
		return this.rdfProcessor.getDestinationMaxSize ();
	}

	public void setRDFChunkSize ( long chunkSize )
	{
		this.rdfProcessor.setDestinationMaxSize ( chunkSize );
	}
	

	public long getCypherChunkSize ()
	{
		return cypherChunkSize;
	}

	public void setCypherChunkSize ( long cypherChunkSize )
	{
		this.cypherChunkSize = cypherChunkSize;
	}	
}
