package uk.ac.rothamsted.rdf.pg.load.graphml;


import static uk.ac.ebi.utils.io.IOUtils.readResource;

import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import uk.ac.ebi.utils.io.IOUtils;
import uk.ac.rothamsted.rdf.pg.load.ConfigItem;
import uk.ac.rothamsted.rdf.pg.load.MultiConfigPGLoader;
import uk.ac.rothamsted.rdf.pg.load.PropertyGraphLoader;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLDataManager;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeExportHandler;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLRelationExportHandler;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManager;
import uk.ac.rothamsted.rdf.pg.load.support.rdf.DataTestUtils;

/**
 * Basic tests for {@link SimpleGraphMLExporter} and {@link MultiConfigPGLoader}.
 *
 * As developer user, you're probably interested in invoking the converter using Spring configuration,
 * @see {@link #testSpringMultiConfig()}. 
 *   
 * @author brandizi
 * @author cbobed
 * <dl><dt>Date:</dt><dd>14 Dec 2017</dd></dl>
 * <dl><dt>Modified:</dt><dd>2 Nov 2020</dd></dl>
 */
public class GraphMLLoaderTest
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
	public void testLoading () throws Exception
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
	
			gmlNodeHandler.setLabelsSparql ( IOUtils.readResource ( "dbpedia_node_labels.sparql" ) );
			gmlNodeHandler.setNodePropsSparql ( IOUtils.readResource ( "dbpedia_node_props.sparql" ) );
			gmlNodeHandler.setRdfDataManager ( rdfMgr );
			gmlNodeHandler.setGraphmlDataMgr ( gmlMgr );
			
			
			gmlRelHandler.setRelationTypesSparql ( IOUtils.readResource ( "dbpedia_rel_types.sparql" ) );
			gmlRelHandler.setRelationPropsSparql ( IOUtils.readResource ( "dbpedia_rel_props.sparql" ) );
			gmlRelHandler.setRdfDataManager ( rdfMgr );
			gmlRelHandler.setGraphmlDataMgr ( gmlMgr );
			
			GraphMLNodeLoadingProcessor gmlNodeProc = new  GraphMLNodeLoadingProcessor();
			gmlNodeProc.setNodeIrisSparql ( IOUtils.readResource ( "dbpedia_node_iris.sparql" ) );
			gmlNodeProc.setBatchJob ( gmlNodeHandler );
			
			GraphMLRelationLoadingProcessor gmlRelProc = new GraphMLRelationLoadingProcessor();
			gmlRelProc.setConsumer ( gmlRelHandler );

			gmlExporter.setPGNodeLoader ( gmlNodeProc );
			gmlExporter.setPGRelationLoader ( gmlRelProc );
			
			gmlExporter.load ( DataTestUtils.TDB_PATH );
			// TODO: test!
			
		} // try neoDriver
	}
	
	
	@Test
	public void testSpring ()
	{
		// Use ConfigurableApplicationContext to show the try() block that it's a Closeable and let Java to clean up
	  // automatically 
		try ( ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "test_config.xml" ); )
		{	
			var graphmlOutputPath = "target/test-simple-exporter-spring.graphml";
			PropertyGraphLoader gmlLoader = beanCtx.getBean ( SimpleGraphMLExporter.class );
			beanCtx.getBean ( GraphMLDataManager.class ).setGraphmlOutputPath ( graphmlOutputPath );
			gmlLoader.load ( DataTestUtils.TDB_PATH );
			// TODO: test
		}
	}	

	
	@Test
	public void testSpringMultiConfig ()
	{
		try ( 
			ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "multi_config.xml" );
			MultiConfigGraphMLLoader mloader = MultiConfigGraphMLLoader.getSpringInstance ( beanCtx, MultiConfigGraphMLLoader.class );				
		)
		{			
			mloader.load ( DataTestUtils.TDB_PATH, "target/test-mconfig-exporter.graphml" );
			// TODO: test
		}
	}	
	
}
