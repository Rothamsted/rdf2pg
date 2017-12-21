package uk.ac.rothamsted.rdf.neo4j.load;

import org.neo4j.driver.v1.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.marcobrandizi.rdfutils.jena.elt.RDFProcessor;
import info.marcobrandizi.rdfutils.jena.elt.TDBLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.CypherLoadingProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.CypherNodeHandler;
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
	private CypherNodeHandler cypherLoadingHandler = new CypherNodeHandler ();
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );

	public void process ( RS rdfSource, Object... opts )
	{
		log.info ( "Collecting RDF input" );
		
		try ( NeoDataManager dataMgr = new NeoDataManager (); )
		{
			TDBLoadingHandler tdbHandler = new TDBLoadingHandler ();			
			tdbHandler.setDataSet ( dataMgr.getDataSet () );
			
			rdfProcessor.setConsumer ( tdbHandler );
			rdfProcessor.process ( rdfSource, opts );
			
			log.info ( "Sending RDF data to Cypher" );
			this.cypherLoadingHandler.setDataMgr ( dataMgr );
			CypherLoadingProcessor cyLoader = new CypherLoadingProcessor ();
			cyLoader.setSparqlNodeIris ( this.nodeIrisSparql );
			cyLoader.setDestinationMaxSize ( this.rdfProcessor.getDestinationMaxSize () );
			cyLoader.setConsumer ( this.cypherLoadingHandler );
			cyLoader.process ( dataMgr );
			
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

	public CypherNodeHandler getCypherLoadingHandler ()
	{
		return cypherLoadingHandler;
	}

	public void setCypherLoadingHandler ( CypherNodeHandler cypherLoadingHandler )
	{
		this.cypherLoadingHandler = cypherLoadingHandler;
	}


	public long getRDFChunkSize ()
	{
		return rdfProcessor.getDestinationMaxSize ();
	}

	public void setRDFChunkSize ( long size )
	{
		this.rdfProcessor.setDestinationMaxSize ( size );
	}
}
