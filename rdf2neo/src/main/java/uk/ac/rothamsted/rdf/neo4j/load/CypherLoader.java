package uk.ac.rothamsted.rdf.neo4j.load;

import java.io.InputStream;

import org.apache.jena.riot.Lang;
import org.neo4j.driver.v1.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.marcobrandizi.rdfutils.jena.SparqlUtils;
import info.marcobrandizi.rdfutils.jena.elt.RDFImporter;
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
public class CypherLoader
{
	private String labelSparqlQuery, nodeSparqlQuery;
	private Driver neo4jDriver;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	
	public void process ( InputStream rdfInput, String base, Lang hintLang )
	{
		log.info ( "Indexing RDF input" );
		
		NeoDataManager dataMgr = new NeoDataManager ();
		// TODO: setup IRI->ID converter.
		dataMgr.openIdxWriter ();

		RDFImporter rdfLoader = new RDFImporter ();
		
		rdfLoader.setConsumer ( model -> {
			dataMgr.indexNodeLabelRows ( SparqlUtils.select ( labelSparqlQuery, model ) );
			dataMgr.indexNodePropertyRows ( SparqlUtils.select ( nodeSparqlQuery, model ) );
		});

		rdfLoader.process ( rdfInput, base, hintLang );
		dataMgr.closeIdxWriter ();

		
		log.info ( "Sending indexed data to Cypher" );
		
		CypherLoadingProcessor cyLoader = new CypherLoadingProcessor ();
		cyLoader.setConsumer ( new CypherNodeHandler ( dataMgr, neo4jDriver ) );
		cyLoader.process ( dataMgr );
		
		log.info ( "RDF-Cypher conversion finished" );
	}
	
}
