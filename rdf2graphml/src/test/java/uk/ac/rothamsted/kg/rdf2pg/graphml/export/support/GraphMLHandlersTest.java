package uk.ac.rothamsted.kg.rdf2pg.graphml.export.support;


import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.xpath.XPathConstants;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Resource;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;

import com.machinezoo.noexception.Exceptions;

import uk.ac.ebi.utils.xml.XPathReader;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;
import uk.ac.rothamsted.kg.rdf2pg.test.DataTestUtils;

/**
 * Tests graphML export handlers.
 *
 * @author brandizi
 * @author cbobed
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 * <dl><dt>Modified:</dt><dd>3 Nov 2020</dd></dl>
 */
public class GraphMLHandlersTest
{	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	@BeforeClass
	public static void initData () throws IOException {
		DataTestUtils.initData ();
	}
		
	/**
	 *  Does the graphML export onto the parameter and returns an {@link XPathReader} on top of such
	 *  file, ready to run verifications. 
	 */
	public GraphMLDataManager createGraphMLDataMgr ( String graphmlPath )
	{
		Stream.of ( "", GraphMLDataManager.NODE_FILE_EXTENSION, GraphMLDataManager.EDGE_FILE_EXTENSION )
		.map ( postfix -> graphmlPath + postfix )
		.map ( Paths::get )
		.forEach ( path -> Exceptions.sneak ().run ( () -> Files.deleteIfExists ( path ) ) );

		var graphmlMgr = new GraphMLDataManager (); 
		graphmlMgr.setGraphmlOutputPath ( graphmlPath );
		log.info ( "graphmlMgr set with outputPath: '{}'", graphmlMgr.getGraphmlOutputPath() ); 
		
		return graphmlMgr;
	}
	
	
	/**
	 * Test {@link GraphMLNodeExportHandler} to see if nodes are mapped from RDF and exported to GraphML
	 */
	@Test
	public void testNodes () throws Exception
	{
		XPathReader gmlxpath = null;
		
		try ( var rdfMgr = new RdfDataManager ( DataTestUtils.TDB_PATH ) )
		{
			var graphmlOutPath = "target/test-nodes.graphml";

			Set<Resource> rdfNodes = 
				Stream.of ( iri ( "ex:1" ), iri ( "ex:2" ), iri ( "ex:3" ) )
				.map ( iri -> rdfMgr.getDataSet ().getDefaultModel ().createResource ( iri ) )
				.collect ( Collectors.toSet () );
			
			var graphmlMgr = createGraphMLDataMgr ( graphmlOutPath );
			
			GraphMLNodeExportHandler handler = new GraphMLNodeExportHandler ();
			handler.setLabelsSparql ( DataTestUtils.SPARQL_NODE_LABELS );
			handler.setNodePropsSparql ( DataTestUtils.SPARQL_NODE_PROPS );

			handler.setRdfDataManager ( rdfMgr );
			handler.setGraphmlDataMgr ( graphmlMgr );

			
			handler.accept ( rdfNodes );
			graphmlMgr.writeGraphML();
						
			gmlxpath = new XPathReader ( Paths.get ( graphmlOutPath ) );
		}
	
		assertEquals ( "Wrong count for TestNode", 
			2, 
			((Double)(gmlxpath.read("count(//node[contains(@labelV, ':TestNode')])", XPathConstants.NUMBER))).longValue()
		);
		
		assertTrue ( "ex:2 not found!",
			( (NodeList) gmlxpath.read ( "//node[@id='http://www.example.com/res/2']", XPathConstants.NODESET ) )
			.getLength () != 0 
		);
		
		String value = gmlxpath.read ( "//node[@id='http://www.example.com/res/2']/data[@key='attrib3']/text()", XPathConstants.STRING );
		log.info ( "Found {} for attrib3", value ); 
		assertEquals ( "Wrong property!", "another string", value);
	}
	
	
	/**
	 * Tests {@link GraphMLRelationExportHandler} to see if relations are mapped from RDF and exported correctly to GraphML.
	 */
	@Test
	public void testRelations () throws Exception
	{
		XPathReader gmlxpath = null;
		
		try ( var rdfMgr = new RdfDataManager ( DataTestUtils.TDB_PATH ) )
		{
			var graphmlOutPath = "target/test-relations.graphml";
			var graphmlMgr = createGraphMLDataMgr ( graphmlOutPath );
			
			GraphMLRelationExportHandler handler = new GraphMLRelationExportHandler ();
			
			handler.setRdfDataManager ( rdfMgr );
			handler.setGraphmlDataMgr ( graphmlMgr );
			
			handler.setRelationTypesSparql ( DataTestUtils.SPARQL_REL_TYPES );
			handler.setRelationPropsSparql ( DataTestUtils.SPARQL_REL_PROPS  );
	
			Set<QuerySolution> relSparqlRows = new HashSet<> ();
			
			rdfMgr.processSelect (
				DataTestUtils.SPARQL_REL_TYPES,
				row -> relSparqlRows.add ( row )
			);
				
			handler.accept ( relSparqlRows );
			graphmlMgr.writeGraphML(); 
	
			// we now have to read the GraphML file to check that everything has gone right 
			gmlxpath = new XPathReader ( Paths.get ( graphmlOutPath ) );
		}
		
		// We convert the tests to XPath expressions

		assertEquals ("Wrong count for relations",
			3,
			((Double)(gmlxpath.read("count(//edge)", XPathConstants.NUMBER))).longValue()
		); 
		
		// TODO: remove?
		// This checks the edge's label as well, but currently only the relations part is generated
//			log.info("ex1:labelV :: "+xPathGML.read("//node[@id='http://www.example.com/res/1']/@labelV", XPathConstants.STRING)); 
//			log.info("ex2:labelV :: "+xPathGML.read("//node[@id='http://www.example.com/res/2']/@labelV", XPathConstants.STRING)); 
//			assertTrue("Wrong label for {ex:1}!", 
//					xPathGML.read("//node[@id='http://www.example.com/res/1']/@labelV", XPathConstants.STRING).equals("TestNode")); 
//			
//			assertTrue("Wrong label for {ex:2}!", 
//					xPathGML.read("//node[@id='http://www.example.com/res/2']/@labelV", XPathConstants.STRING).equals("TestNode"));
		
		assertEquals ( "Wrong count for {1 relatedTo 2}!",
			1, 
			((Double)(gmlxpath.read("count(//edge[@labelE='relatedTo'][@source='http://www.example.com/res/1'][@target='http://www.example.com/res/2'])", XPathConstants.NUMBER))).longValue()
		); 
				
		assertEquals( "Wrong count for {3 derivedFrom 1}!",
			1,
			((Double)(gmlxpath.read("count(//edge[@labelE='derivedFrom'][@source='http://www.example.com/res/3'][@target='http://www.example.com/res/1'])",XPathConstants.NUMBER))).longValue()
		); 

		// the relatedTo relation has the properties "Another Note", "Reified Relation".
		//
		String valueList= gmlxpath.read("//edge[@labelE='relatedTo'][@source='http://www.example.com/res/2'][@target='http://www.example.com/res/3']/data[@key='note']/text()", XPathConstants.STRING);
		
		List<String> notesGML = new ArrayList<>(); 
		if (valueList.trim().startsWith("[")) {
			valueList = valueList.replace("[", "").replace("]",""); 
			StringTokenizer tokenizer = new StringTokenizer(valueList, ",");
			tokenizer.asIterator().forEachRemaining(st -> notesGML.add(((String)st).trim()));
		}
		Collections.sort(notesGML);
		
		assertTrue("reified relation, wrong property value for 'note'!", 
			!"".equals(valueList) && notesGML.equals(Arrays.asList(new String[] {"Another Note", "Reified Relation"}))
		); 
	}	// testRelations

}
