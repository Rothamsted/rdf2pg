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
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeLoadingHandler;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLRelationLoadingHandler;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManager;
import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManagerTestBase;

/**
 * Basic tests for {@link SimpleGraphMLLoader} and {@link MultiConfigPGLoader}.
 *
 * As developer user, you're probably interested in invoking the converter using Spring configuration,
 * @see {@link #testSpringMultiConfig()}. 
 *   
 * @author brandizi
 * @author cbobed
 * <dl><dt>Date:</dt><dd>14 Dec 2017</dd></dl>
 * <dl><dt>Modified:</dt><dd>2 Nov 2020</dd></dl>
 */
public class GraphMLLoaderIT
{
	@BeforeClass
	public static void initTDB ()
	{
		try (
			RdfDataManager rdfMgr = new RdfDataManager ( RdfDataManagerTestBase.TDB_PATH );
	  )
		{
			Dataset ds = rdfMgr.getDataSet ();
			for ( String ttlPath: new String [] { "dbpedia_places.ttl", "dbpedia_people.ttl" } )
			Txn.executeWrite ( ds, () -> 
				ds.getDefaultModel ().read ( 
					"file:target/test-classes/" + ttlPath, 
					null, 
					"TURTLE" 
			));
		}	
	}
	
	@Test
	public void testLoading () throws Exception
	{
		try (
			RdfDataManager rdfMgr = new RdfDataManager ( RdfDataManagerTestBase.TDB_PATH );
			SimpleGraphMLLoader gmlLoader = new SimpleGraphMLLoader(); 
		)
		{
			// You don't want to do this, see #testSpring()

			GraphMLDataManager gmlMgr = new GraphMLDataManager ();
			
			GraphMLNodeLoadingHandler gmlNodeHandler = new GraphMLNodeLoadingHandler ();
			GraphMLRelationLoadingHandler gmlRelHandler = new GraphMLRelationLoadingHandler ();
	
			gmlNodeHandler.setLabelsSparql ( IOUtils.readResource ( "dbpedia_node_labels.sparql" ) );
			gmlNodeHandler.setNodePropsSparql ( IOUtils.readResource ( "dbpedia_node_props.sparql" ) );
			gmlNodeHandler.setRdfDataManager ( rdfMgr );
			gmlNodeHandler.setGraphMLDataManager(gmlMgr);
			
			
			gmlRelHandler.setRelationTypesSparql ( IOUtils.readResource ( "dbpedia_rel_types.sparql" ) );
			gmlRelHandler.setRelationPropsSparql ( IOUtils.readResource ( "dbpedia_rel_props.sparql" ) );
			gmlRelHandler.setRdfDataManager ( rdfMgr );
			gmlRelHandler.setGraphMLDataManager(gmlMgr);
			
			GraphMLNodeLoadingProcessor gmlNodeProc = new  GraphMLNodeLoadingProcessor();
			gmlNodeProc.setNodeIrisSparql ( IOUtils.readResource ( "dbpedia_node_iris.sparql" ) );
			gmlNodeProc.setBatchJob ( gmlNodeHandler );
			
			GraphMLRelationLoadingProcessor gmlRelProc = new GraphMLRelationLoadingProcessor();
			gmlRelProc.setConsumer ( gmlRelHandler );

			gmlLoader.setPGNodeLoader ( gmlNodeProc );
			gmlLoader.setPGRelationLoader ( gmlRelProc );
			
			gmlLoader.load ( RdfDataManagerTestBase.TDB_PATH, "test-graphml.gml");
			// TODO: test!
			
		} // try neoDriver
	}
	

	@Test
	public void testMultiConfigLoading () throws Exception
	{
		try ( var gmlMultiLoader = new MultiConfigGraphMLLoader (); )
		{
			
			GraphMLDataManager gmlMgr = new GraphMLDataManager ();
			gmlMultiLoader.setGmlDataMgr(gmlMgr);
			
			gmlMultiLoader.setPGLoaderFactory ( () -> 
			{
				// You don't want to do this, see #testSpring()
				
				RdfDataManager rdfMgr = new RdfDataManager ();
				
				GraphMLNodeLoadingHandler gmlNodeHandler = new GraphMLNodeLoadingHandler ();
				GraphMLRelationLoadingHandler gmlRelHandler = new GraphMLRelationLoadingHandler ();
				
				gmlNodeHandler.setRdfDataManager ( rdfMgr );
				gmlNodeHandler.setGraphMLDataManager(gmlMultiLoader.getGmlDataMgr());
				
				gmlRelHandler.setRdfDataManager ( rdfMgr );
				gmlRelHandler.setGraphMLDataManager(gmlMultiLoader.getGmlDataMgr()); 
				
				GraphMLNodeLoadingProcessor gmlNodeProc = new GraphMLNodeLoadingProcessor ();
				gmlNodeProc.setBatchJob ( gmlNodeHandler );
				
				GraphMLRelationLoadingProcessor gmlRelProc = new GraphMLRelationLoadingProcessor ();
				gmlRelProc.setConsumer ( gmlRelHandler );
	
				SimpleGraphMLLoader gmlLoader = new SimpleGraphMLLoader ();
				gmlLoader.setPGNodeLoader ( gmlNodeProc );
				gmlLoader.setPGRelationLoader ( gmlRelProc );
				gmlLoader.setRdfDataManager ( rdfMgr );
				
				return gmlLoader;
			});
	
			
			List<ConfigItem<SimpleGraphMLLoader>> config = new LinkedList<> ();
			{
				var cfgi = new ConfigItem<SimpleGraphMLLoader> ();
				cfgi.setName ( "places" );
				cfgi.setNodeIrisSparql ( readResource ( "dbpedia_node_iris.sparql" ) );
				cfgi.setLabelsSparql ( readResource ( "dbpedia_node_labels.sparql" ) );
				cfgi.setNodePropsSparql ( readResource ( "dbpedia_node_props.sparql" ) );
				cfgi.setRelationTypesSparql ( readResource ( "dbpedia_rel_types.sparql" ) );
				cfgi.setRelationPropsSparql ( readResource ( "dbpedia_rel_props.sparql" ) );
				config.add ( cfgi );
			}

			{
				var cfgi = new ConfigItem<SimpleGraphMLLoader> ();
				cfgi.setName ( "people" );
				cfgi.setNodeIrisSparql ( readResource ( "dbpedia_people_iris.sparql" ) );
				cfgi.setLabelsSparql ( readResource ( "dbpedia_people_labels.sparql" ) );
				cfgi.setNodePropsSparql ( readResource ( "dbpedia_people_props.sparql" ) );
				cfgi.setRelationTypesSparql ( readResource ( "dbpedia_people_rel_types.sparql" ) );
				config.add ( cfgi );
			}
			
			gmlMultiLoader.setConfigItems ( config );
	
			gmlMultiLoader.load ( RdfDataManagerTestBase.TDB_PATH, "test-multiconfig-graphml.gml");
		}
		// TODO: test!
	}	
	
	
	@Test
	public void testSpring ()
	{
		// Use ConfigurableApplicationContext to show the try() block that it's a Closeable and let Java to clean up
	  // automatically 
		try ( ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "test_config_gml.xml" ); )
		{			
			PropertyGraphLoader gmlLoader = beanCtx.getBean ( SimpleGraphMLLoader.class );
			gmlLoader.load ( RdfDataManagerTestBase.TDB_PATH );
			// TODO: test
		}
	}	

	
	@Test
	public void testSpringMultiConfig ()
	{
		try ( 
			ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "multi_config_gml.xml" );
			MultiConfigGraphMLLoader mloader = MultiConfigGraphMLLoader.getSpringInstance ( beanCtx, MultiConfigGraphMLLoader.class );				
		)
		{			
			mloader.load ( RdfDataManagerTestBase.TDB_PATH );
			// TODO: test
		}
	}	

	
}
