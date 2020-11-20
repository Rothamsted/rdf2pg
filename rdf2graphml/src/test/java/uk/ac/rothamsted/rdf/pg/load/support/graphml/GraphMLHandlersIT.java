package uk.ac.rothamsted.rdf.pg.load.support.graphml;


import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.system.Txn;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import info.marcobrandizi.rdfutils.jena.SparqlUtils;
import uk.ac.ebi.utils.io.IOUtils;
import uk.ac.ebi.utils.xml.XPathReader;
import uk.ac.rothamsted.rdf.pg.load.graphml.SimpleGraphMLLoader;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLDataManager;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeLoadingHandler;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLRelationLoadingHandler;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManager;
import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManagerTestBase;

/**
 * Runs {@link GraphMLLoadingHandler}-related tests.
 *
 * @author brandizi
 * @author cbobed
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 * <dl><dt>Modified:</dt><dd>3 Nov 2020</dd></dl>
 */
public class GraphMLHandlersIT
{
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	@BeforeClass
	public static void initData () throws IOException {
		RdfDataManagerTestBase.initData ();
	}
	
	@AfterClass
	public static void closeData () throws IOException {
		RdfDataManagerTestBase.closeDataMgr ();
	}
	
	/**
	 * Test {@link GraphMLNodeLoadingHandler} to see if nodes are mapped from RDF and exported to GraphML
	 */
	@Test
	public void testNodes () throws Exception
	{
		try (	
			RdfDataManager rdfMgr = new RdfDataManager ( RdfDataManagerTestBase.TDB_PATH );
			SimpleGraphMLLoader gmlLoader = new SimpleGraphMLLoader(); 
		)
		{
			
			String testFilePath = "target/testNodes.gml"; 
			
			Files.deleteIfExists(Paths.get(testFilePath));
			Files.deleteIfExists(Paths.get(testFilePath+GraphMLDataManager.NODE_FILE_EXTENSION));
			Files.deleteIfExists(Paths.get(testFilePath+GraphMLDataManager.EDGE_FILE_EXTENSION));
			
			GraphMLNodeLoadingHandler handler = new GraphMLNodeLoadingHandler ();
			GraphMLDataManager gmlMgr = new GraphMLDataManager(); 
			gmlMgr.setGraphmlOutputPath(testFilePath);
			log.info("gmlMgr outputPath: "+gmlMgr.getGraphmlOutputPath()); 
			// We need the same nodes in all tests
			handler.setRdfDataManager ( rdfMgr );
			handler.setLabelsSparql ( RdfDataManagerTestBase.SPARQL_NODE_LABELS );
			handler.setNodePropsSparql ( RdfDataManagerTestBase.SPARQL_NODE_PROPS );
			handler.setGraphMLDataManager(gmlMgr);
			
			Set<Resource> rdfNodes = 
				Stream.of ( iri ( "ex:1" ), iri ( "ex:2" ), iri ( "ex:3" ) )
				.map ( iri -> rdfMgr.getDataSet ().getDefaultModel ().createResource ( iri ) )
				.collect ( Collectors.toSet () );

			handler.accept ( rdfNodes );
			gmlMgr.writeGraphML();
						
			XPathReader xPathGML = new XPathReader (Files.newInputStream(Paths.get(handler.getGraphMLDataManager().getGraphmlOutputPath()))); 
			
			//NEO tests 
//			StatementResult cursor = session.run ( "MATCH ( n:TestNode ) RETURN COUNT ( n ) AS ct" );
//			Assert.assertEquals ( "Wrong count for TestNode", 2, cursor.next ().get ( "ct" ).asLong () );
//			
//			cursor = session.run ( "MATCH ( n:TestNode { iri:'" + iri ( "ex:2" ) + "'} ) RETURN properties ( n ) AS props" );
//			assertTrue ( "ex:2 not returned!", cursor.hasNext () );
//			
//			Map<String, Object> map = cursor.next ().get ( "props" ).asMap ();
//			assertEquals (  "Wrong property!", "another string", map.get ( "attrib3" ) );		
			
			assertEquals ( "Wrong count for TestNode", 
									2, 
									((Double)(xPathGML.read("count(//node[contains(@labelV, ':TestNode')])", XPathConstants.NUMBER))).longValue());
			
			assertTrue ( "ex:2 not found!", ((NodeList) xPathGML.read("//node[@id='http://www.example.com/res/2']", XPathConstants.NODESET)).getLength() != 0 ); 
			
			String value = xPathGML.read("//node[@id='http://www.example.com/res/2']/data[@key='attrib3']/text()", XPathConstants.STRING);
			log.info("Found "+value+ " for attrib3"); 
			assertEquals ( "Wrong property!", "another string", value); 
		}
	}
	
	
	/**
	 * Tests {@link GraphMLRelationLoadingHandler} to see if relations are mapped from RDF and exported correctly to GraphML.
	 */
	@Test
	public void testRelations () throws Exception
	{
		try (	
			RdfDataManager rdfMgr = new RdfDataManager ( RdfDataManagerTestBase.TDB_PATH );
			SimpleGraphMLLoader gmlLoader = new SimpleGraphMLLoader(); 

		)
		{
			
			
			String testFilePath = "target/testRelations.gml"; 
			
			Files.deleteIfExists(Paths.get(testFilePath));
			Files.deleteIfExists(Paths.get(testFilePath+GraphMLDataManager.NODE_FILE_EXTENSION));
			Files.deleteIfExists(Paths.get(testFilePath+GraphMLDataManager.EDGE_FILE_EXTENSION));
			
			GraphMLDataManager gmlMgr = new GraphMLDataManager(); 
			gmlMgr.setGraphmlOutputPath(testFilePath);
			log.info("gmlMgr outputPath: "+gmlMgr.getGraphmlOutputPath()); 
			
			GraphMLRelationLoadingHandler handler = new GraphMLRelationLoadingHandler ();
			
			handler.setRdfDataManager ( rdfMgr );
			handler.setGraphMLDataManager(gmlMgr);
			handler.setRelationTypesSparql ( RdfDataManagerTestBase.SPARQL_REL_TYPES );
			handler.setRelationPropsSparql ( RdfDataManagerTestBase.SPARQL_REL_PROPS  );

			Set<QuerySolution> relSparqlRows = new HashSet<> ();
			
			Dataset dataSet = rdfMgr.getDataSet ();
			Txn.executeRead ( dataSet,  () ->
				SparqlUtils.select ( RdfDataManagerTestBase.SPARQL_REL_TYPES, rdfMgr.getDataSet ().getDefaultModel () )
					.forEachRemaining ( row -> relSparqlRows.add ( row ) )
			);

			handler.accept ( relSparqlRows );
			gmlMgr.writeGraphML(); 

			// we now have to read the GraphML file to check that everything has gone right 
			XPathReader xPathGML = new XPathReader (Files.newInputStream(Paths.get(handler.getGraphMLDataManager().getGraphmlOutputPath()))); 
			
			// We convert the tests to XPath expressions

			assertEquals ("Wrong count for relations",
					3,
					((Double)(xPathGML.read("count(//edge)", XPathConstants.NUMBER))).longValue()); 
			
			// we checked the edge's label as well, but currently only the relations part is generated
//			log.info("ex1:labelV :: "+xPathGML.read("//node[@id='http://www.example.com/res/1']/@labelV", XPathConstants.STRING)); 
//			log.info("ex2:labelV :: "+xPathGML.read("//node[@id='http://www.example.com/res/2']/@labelV", XPathConstants.STRING)); 
//			assertTrue("Wrong label for {ex:1}!", 
//					xPathGML.read("//node[@id='http://www.example.com/res/1']/@labelV", XPathConstants.STRING).equals("TestNode")); 
//			
//			assertTrue("Wrong label for {ex:2}!", 
//					xPathGML.read("//node[@id='http://www.example.com/res/2']/@labelV", XPathConstants.STRING).equals("TestNode"));
			
			assertEquals("Wrong count for {1 relatedTo 2}!",
					1, 
					((Double)(xPathGML.read("count(//edge[@labelE='relatedTo'][@source='http://www.example.com/res/1'][@target='http://www.example.com/res/2'])", XPathConstants.NUMBER))).longValue()); 
			
			
//			Assert.assertTrue (
//				"Wrong count for {3 derivedFrom 1}!",
//				tester.ask ( 
//					"MATCH p = (:SuperTestNode{ iri:$iri1 })-[:derivedFrom]->(:TestNode{ iri:$iri2 }) RETURN COUNT ( p ) = 1",
//					"iri1", iri ( "ex:3" ), "iri2", iri ( "ex:1" )
//				)
//			);
			
			assertEquals("Wrong count for {3 derivedFrom 1}!",
					1,
					((Double)(xPathGML.read("count(//edge[@labelE='derivedFrom'][@source='http://www.example.com/res/3'][@target='http://www.example.com/res/1'])",XPathConstants.NUMBER))).longValue()); 
			
			
			
			// in graphML you can only have one value for the label 
			// that's why we don't test the :AdditionalLabel on ex:3
			
			
//			Assert.assertTrue (
//					"reified relation, wrong property value for 'note'!",
//					tester.compare (
//						// Test against the Cypher result
//						notesv -> {
//							List<Object> notes = notesv.asList ();
//							if ( notes == null || notes.isEmpty () ) return false;
//
//							// notes collection is sorted, then compared to the sorted values in the reference
//							return notes
//								.stream ()
//								.sorted ()
//								.collect ( Collectors.toList () )
//								.equals ( 
//									Arrays.asList ( new String[] { "Another Note", "Reified Relation" } )
//								);
//						},
//						// the relation containing .note
//						"MATCH (:TestNode{ iri:$iri1 })-[r:relatedTo]->(:AdditionalLabel{ iri:$iri2 })\n"
//						+ "RETURN r.note\n",
//						"iri1", iri ( "ex:2" ), "iri2", iri ( "ex:3" )
//					)
//				);
			
			String valueList= xPathGML.read("//edge[@labelE='relatedTo'][@source='http://www.example.com/res/2'][@target='http://www.example.com/res/3']/data[@key='note']/text()", XPathConstants.STRING);
			
			List<String> notesGML = new ArrayList<String>(); 
			if (valueList.trim().startsWith("[")) {
				valueList = valueList.replace("[", "").replace("]",""); 
				StringTokenizer tokenizer = new StringTokenizer(valueList, ",");
				tokenizer.asIterator().forEachRemaining(st -> notesGML.add(((String)st).trim()));
			}
			Collections.sort(notesGML);
			
			assertTrue("reified relation, wrong property value for 'note'!", 
					!"".equals(valueList) && notesGML.equals(Arrays.asList(new String[] {"Another Note", "Reified Relation"}))
					); 
			
			
		
		} // try
	}	// testRelations

}
