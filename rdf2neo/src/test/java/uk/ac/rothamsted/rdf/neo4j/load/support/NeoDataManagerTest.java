package uk.ac.rothamsted.rdf.neo4j.load.support;

import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.marcobrandizi.rdfutils.jena.SparqlUtils;
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
	private NeoDataManager dataMgr = new NeoDataManager ();
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	
	static {
		NamespaceUtils.registerNs ( "ex", "http://www.example.com/res/" );
	}
	
	@Test
	public void testLuceneNodeFunctions () throws Exception
	{
		// Data actually come from the query, via VALUES(), but the model is required anyway
		Model m = ModelFactory.createDefaultModel ();
		ResultSet rs = SparqlUtils.select ( IOUtils.readResource ( "test_nodes_labels.sparql" ), m );

		dataMgr.openIdxWriter ();

		log.info ( "Indexing nodes" );
		dataMgr.indexNodeLabelRows ( rs );

		rs = SparqlUtils.select ( IOUtils.readResource ( "test_nodes_props.sparql" ), m );
		dataMgr.indexNodePropertyRows ( rs );

		dataMgr.closeIdxWriter ();

		log.info ( "Verifying" );
		Node node = dataMgr.getNode ( iri ( "ex:1" ) );
		assertNotNull ( "Node 1 not found!", node );
		log.info ( "Got node 1" );

		assertEquals ( "Node 1's Label not found!", 1, node.getLabels ().size () );
		assertEquals ( "Node 1's Label not found!", "TestNode", node.getLabels ().iterator ().next () );
		
		assertEquals ( "Node 1's worng properties count!", 2, node.getProperties ().size () );
		assertEquals ( "Node 1's prop1 not found!", "10.0", node.getPropValue ( "attrib1" ) );
		assertEquals ( "Node 1's prop2 not found!", "a string", node.getPropValue ( "attrib2" ) );
		
		log.info ( "Searching Node IRIs" );
		assertEquals ( "Wrong nodeIri count!", 3, dataMgr.getNodeIris ().count () );
		log.info ( "End" );
	}

	
	@Test
	public void testIri2IdConversion () throws Exception
	{
		Model m = ModelFactory.createDefaultModel ();
		ResultSet rs = SparqlUtils.select ( IOUtils.readResource ( "iri_labels.sparql" ), m );
		dataMgr.openIdxWriter ();
		dataMgr.indexNodeLabelRows ( rs );
		dataMgr.closeIdxWriter ();

		Node node = dataMgr.getNode ( iri ( "ex:1" ) );
		assertNotNull ( "Node 1 not found!", node );
		assertEquals ( "Labels's size wrong for node 1!", 1, node.getLabels ().size () );
		assertEquals ( "node1's label not found!", "TestNode", node.getLabels ().iterator ().next () );

		node = dataMgr.getNode ( iri ( "ex:2" ) );
		assertNotNull ( "Node 2 not found!", node );
		assertEquals ( "Labels's size wrong for node 2!", 1, node.getLabels ().size () );
		assertEquals ( "node2's label not found!", "TestNode", node.getLabels ().iterator ().next () );

		node = dataMgr.getNode ( iri ( "ex:3" ) );
		assertNotNull ( "Node 3 not found!", node );
		assertEquals ( "Labels's size wrong for node 3!", 1, node.getLabels ().size () );
		assertEquals ( "node3's label not found!", "SuperTestNode", node.getLabels ().iterator ().next () );
	}
	
	public NeoDataManager getDataMgr ()
	{
		return dataMgr;
	}
	
}
