package uk.ac.rothamsted.kg.rdf2pg.graphml.export;


import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLDataManager;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLNodeExportHandler;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLNodeExportProcessor;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLRelationExportHandler;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLRelationExportProcessor;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.PropertyGraphMaker;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;
import uk.ac.rothamsted.kg.rdf2pg.test.DataTestUtils;

/**
 * Basic tests for {@link SimpleGraphMLExporter} and {@link MultiConfigPGMaker}.
 *
 * As developer user, you're probably interested in invoking the converter using Spring configuration,
 * @see {@link #testSpringMultiConfig()}. 
 *   
 * @author brandizi
 * @author cbobed
 * <dl><dt>Date:</dt><dd>14 Dec 2017</dd></dl>
 * <dl><dt>Modified:</dt><dd>2 Nov 2020</dd></dl>
 */
public class GraphMLExportTest
{
	@BeforeClass
	public static void initTDB ()
	{
		try (
			RdfDataManager rdfMgr = new RdfDataManager ( DataTestUtils.TDB_PATH );
	  )
		{
			DataTestUtils.initDBpediaDataSet ();
		}	
	}
	
	@Test
	public void testExport () throws Exception
	{
		try (
			RdfDataManager rdfMgr = new RdfDataManager ( DataTestUtils.TDB_PATH );
			SimpleGraphMLExporter gmlExporter = new SimpleGraphMLExporter(); 
		)
		{
			// You don't want to do this, see #testSpring()

			var graphmlOutputPath = "target/test-simple-exporter.graphml";
			
			GraphMLDataManager gmlMgr = new GraphMLDataManager ();
			gmlMgr.setGraphmlOutputPath ( graphmlOutputPath );
			
			GraphMLNodeExportHandler gmlNodeHandler = new GraphMLNodeExportHandler ();
			GraphMLRelationExportHandler gmlRelHandler = new GraphMLRelationExportHandler ();
	
			gmlNodeHandler.setLabelsSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_LABELS );
			gmlNodeHandler.setNodePropsSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_PROPS );
			gmlNodeHandler.setRdfDataManager ( rdfMgr );
			gmlNodeHandler.setGraphmlDataMgr ( gmlMgr );
			
			
			gmlRelHandler.setRelationTypesSparql ( DataTestUtils.DBPEDIA_SPARQL_REL_TYPES );
			gmlRelHandler.setRelationPropsSparql ( DataTestUtils.DBPEDIA_SPARQL_REL_PROPS );
			gmlRelHandler.setRdfDataManager ( rdfMgr );
			gmlRelHandler.setGraphmlDataMgr ( gmlMgr );
			
			GraphMLNodeExportProcessor gmlNodeProc = new  GraphMLNodeExportProcessor();
			gmlNodeProc.setNodeIrisSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_IRIS );
			gmlNodeProc.setBatchJob ( gmlNodeHandler );
			
			GraphMLRelationExportProcessor gmlRelProc = new GraphMLRelationExportProcessor();
			gmlRelProc.setBatchJob ( gmlRelHandler );

			gmlExporter.setPGNodeMaker ( gmlNodeProc );
			gmlExporter.setPGRelationMaker ( gmlRelProc );
			
			gmlExporter.make ( DataTestUtils.TDB_PATH );

			// TODO: assertions!
			
		} // try gmlExporter
	}
	
	
	@Test
	public void testSpring ()
	{
		// Use ConfigurableApplicationContext to show the try() block that it's a Closeable and let Java to clean up
	  // automatically 
		try ( ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "test_config.xml" ); )
		{	
			var graphmlOutputPath = "target/test-simple-exporter-spring.graphml";
			PropertyGraphMaker graphmlExporter = beanCtx.getBean ( SimpleGraphMLExporter.class );
			beanCtx.getBean ( GraphMLDataManager.class ).setGraphmlOutputPath ( graphmlOutputPath );
			graphmlExporter.make ( DataTestUtils.TDB_PATH );
			// TODO: test
		}
	}	

	
	@Test
	public void testSpringMultiConfig ()
	{
		try ( 
			ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "multi_config.xml" );
			MultiConfigGraphMLExporter mmaker = MultiConfigGraphMLExporter.getSpringInstance ( beanCtx, MultiConfigGraphMLExporter.class );				
		)
		{			
			mmaker.make ( DataTestUtils.TDB_PATH, "target/test-mconfig-exporter.graphml" );
			// TODO: test
		}
	}	
	
}
