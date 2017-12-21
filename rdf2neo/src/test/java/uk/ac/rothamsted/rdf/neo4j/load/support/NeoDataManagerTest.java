package uk.ac.rothamsted.rdf.neo4j.load.support;

import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.asSPARQLProlog;
import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.marcobrandizi.rdfutils.namespaces.NamespaceUtils;
import uk.ac.ebi.utils.io.IOUtils;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.NeoDataManager;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.Node;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>8 Dec 2017</dd></dl>
 *
 */
public class NeoDataManagerTest
{
	static {
		NamespaceUtils.registerNs ( "ex", "http://www.example.com/res/" );
	}

	public final static String SPARQL_LABELS = 
		asSPARQLProlog () + "\nSELECT DISTINCT ?iri ?label { ?iri a ?label}";
	
	public final static String SPARQL_PROPS = asSPARQLProlog () +
		"\nSELECT DISTINCT ?iri ?name ?value {\n" +
		"  ?iri ?name ?value. \n" +
		"  FILTER ( STRSTARTS ( STR ( ?name ), STR ( ex: ) ) )\n" +
		"}";
	
	private NeoDataManager dataMgr = new NeoDataManager ();
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
		
	@Test
	public void testBasics () throws Exception
	{
		// Data actually come from the query, via VALUES(), but the model is required anyway
		Dataset ds = dataMgr.getDataSet ();
		Model m = ds.getDefaultModel ();
		ds.begin ( ReadWrite.WRITE );
		m.read ( IOUtils.openResourceReader ( "test_data.ttl" ), null, "TURTLE" );
		ds.commit ();
		ds.end ();
		 
		log.info ( "Verifying" );
		Node node = dataMgr.getNode ( m.getResource ( iri ( "ex:1" ) ), SPARQL_LABELS, SPARQL_PROPS );
		assertNotNull ( "Node 1 not found!", node );
		log.info ( "Got node 1" );

		assertEquals ( "Node 1's Label not found!", 1, node.getLabels ().size () );
		assertEquals ( "Node 1's Label not found!", "TestNode", node.getLabels ().iterator ().next () );
		
		assertEquals ( "Node 1's worng properties count!", 2, node.getProperties ().size () );
		assertEquals ( "Node 1's prop1 not found!", "10.0", node.getPropValue ( "attrib1" ) );
		assertEquals ( "Node 1's prop2 not found!", "a string", node.getPropValue ( "attrib2" ) );
		
		log.info ( "End" );
	}
	
	public NeoDataManager getDataMgr ()
	{
		return this.dataMgr;
	}
	
}
