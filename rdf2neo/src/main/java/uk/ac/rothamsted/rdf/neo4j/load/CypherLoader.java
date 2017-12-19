package uk.ac.rothamsted.rdf.neo4j.load;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.neo4j.driver.v1.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.marcobrandizi.rdfutils.jena.SparqlUtils;
import info.marcobrandizi.rdfutils.jena.elt.RDFProcessor;
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
	private List<String> labelSparqlQueries, nodePropSparqlQueries;
	private Driver neo4jDriver;
	
	private RDFProcessor<RS> rdfProcessor;
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );

	public void process ( RS rdfSource, Object... opts )
	{
		log.info ( "Indexing RDF input" );
		
		NeoDataManager dataMgr = new NeoDataManager ();
		dataMgr.openIdxWriter ();
		
		long ct[] = new long [] { 0 };
		
		rdfProcessor.setConsumer ( model -> 
		{
			
			if ( log.isTraceEnabled () )
			{
				try {
					// TODO: get current tmp location
					model.write ( new FileWriter ( "/tmp/cyloader_test_model_" + (++ct [ 0 ]) + ".ttl" ), "TURTLE" );
				}
				catch ( IOException ex ) {
					throw new RuntimeException ( "Internal error: " + ex.getMessage (), ex );
				}
			}
			
			for ( String labelSparqlQuery: this.labelSparqlQueries )
				dataMgr.indexNodeLabelRows ( SparqlUtils.select ( labelSparqlQuery, model ) );
			for ( String nodePropSparqlQuery: this.nodePropSparqlQueries )
				dataMgr.indexNodePropertyRows ( SparqlUtils.select ( nodePropSparqlQuery, model ) );
		});

		rdfProcessor.process ( rdfSource, opts );
		dataMgr.closeIdxWriter ();
		
		log.info ( "Sending indexed data to Cypher" );
		CypherLoadingProcessor cyLoader = new CypherLoadingProcessor ();
		cyLoader.setDestinationMaxSize ( this.rdfProcessor.getDestinationMaxSize () );
		cyLoader.setConsumer ( new CypherNodeHandler ( dataMgr, neo4jDriver ) );
		cyLoader.process ( dataMgr );
		
		log.info ( "RDF-Cypher conversion finished" );
	}


	public List<String> getLabelSparqlQueries ()
	{
		return labelSparqlQueries;
	}

	public void setLabelSparqlQueries ( List<String> labelSparqlQueries )
	{
		this.labelSparqlQueries = labelSparqlQueries;
	}

	public void setLabelSparqlQueries ( String... labelSparqlQueries )
	{
		this.setLabelSparqlQueries ( Arrays.asList ( labelSparqlQueries ) );
	}

	
	public List<String> getNodePropSparqlQueries ()
	{
		return nodePropSparqlQueries;
	}

	public void setNodePropSparqlQueries ( List<String> nodePropSparqlQueries )
	{
		this.nodePropSparqlQueries = nodePropSparqlQueries;
	}

	public void setNodePropSparqlQueries ( String... nodePropSparqlQueries )
	{
		this.setNodePropSparqlQueries ( Arrays.asList ( nodePropSparqlQueries ) );
	}

	public Driver getNeo4jDriver ()
	{
		return neo4jDriver;
	}

	public void setNeo4jDriver ( Driver neo4jDriver )
	{
		this.neo4jDriver = neo4jDriver;
	}

	public RDFProcessor<RS> getRdfProcessor ()
	{
		return rdfProcessor;
	}

	public void setRdfProcessor ( RDFProcessor<RS> rdfProcessor )
	{
		this.rdfProcessor = rdfProcessor;
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
