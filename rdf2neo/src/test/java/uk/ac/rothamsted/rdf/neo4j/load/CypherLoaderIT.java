package uk.ac.rothamsted.rdf.neo4j.load;

import static uk.ac.ebi.utils.io.IOUtils.readResource;

import java.util.LinkedList;
import java.util.List;

import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import uk.ac.ebi.utils.io.IOUtils;
import uk.ac.rothamsted.rdf.neo4j.load.MultiConfigCyLoader.ConfigItem;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyNodeLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyRelationLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.support.CypherHandlersIT;
import uk.ac.rothamsted.rdf.neo4j.load.support.NeoDataManager;
import uk.ac.rothamsted.rdf.neo4j.load.support.NeoDataManagerTest;

/**
 * Basic tests for {@link SimpleCyLoader} and {@link MultiConfigCyLoader}.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>14 Dec 2017</dd></dl>
 *
 */
public class CypherLoaderIT
{
	@BeforeClass
	public static void initTDB ()
	{
		try (
			NeoDataManager dataMgr = new NeoDataManager ( NeoDataManagerTest.TDB_PATH );
	  )
		{
			Dataset ds = dataMgr.getDataSet ();
			for ( String ttlPath: new String [] { "dbpedia_places.ttl", "dbpedia_people.ttl" } )
			Txn.executeWrite ( ds, () -> 
				ds.getDefaultModel ().read ( 
					"file:target/test-classes/" + ttlPath, 
					null, 
					"TURTLE" 
			));
		}	
	}
	
	
	@Before
	public void initNeo () {
		CypherHandlersIT.initNeo ();
	}
	
	@Test
	public void testLoading () throws Exception
	{
		try (
			Driver neoDriver = GraphDatabase.driver ( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
			NeoDataManager dataMgr = new NeoDataManager ( NeoDataManagerTest.TDB_PATH );
		)
		{ 			
			// TODO: configurator, multiple config sets

			CyNodeLoadingHandler cyNodeHandler = new CyNodeLoadingHandler ();
			CyRelationLoadingHandler cyRelHandler = new CyRelationLoadingHandler ();
			
			cyNodeHandler.setLabelsSparql ( IOUtils.readResource ( "dbpedia_node_labels.sparql" ) );
			cyNodeHandler.setNodePropsSparql ( IOUtils.readResource ( "dbpedia_node_props.sparql" ) );
			cyNodeHandler.setDataManager ( dataMgr );
			cyNodeHandler.setNeo4jDriver ( neoDriver );
			
			cyRelHandler.setRelationTypesSparql ( IOUtils.readResource ( "dbpedia_rel_types.sparql" ) );
			cyRelHandler.setRelationPropsSparql ( IOUtils.readResource ( "dbpedia_rel_props.sparql" ) );
			cyRelHandler.setDataManager ( dataMgr );
			cyRelHandler.setNeo4jDriver ( neoDriver );

			CyNodeLoadingProcessor cyNodeProc = new CyNodeLoadingProcessor ();
			cyNodeProc.setNodeIrisSparql ( IOUtils.readResource ( "dbpedia_node_iris.sparql" ) );
			cyNodeProc.setConsumer ( cyNodeHandler );
			
			CyRelationLoadingProcessor cyRelProc = new CyRelationLoadingProcessor ();
			cyRelProc.setConsumer ( cyRelHandler );

			SimpleCyLoader cyloader = new SimpleCyLoader ();
			cyloader.setCyNodeLoader ( cyNodeProc );
			cyloader.setCyRelationLoader ( cyRelProc );
			
			cyloader.load ( NeoDataManagerTest.TDB_PATH );
			// TODO: test!
			
		} // try neoDriver
	}
	

	@Test
	public void testMultiConfigLoading () throws Exception
	{
		MultiConfigCyLoader cymloader = new MultiConfigCyLoader ();

		cymloader.setCypherLoaderFactory ( () -> 
		{
			// This will eventually be managed by Spring
			NeoDataManager dataMgr = new NeoDataManager ();
			Driver neoDriver = GraphDatabase.driver ( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
			
			CyNodeLoadingHandler cyNodeHandler = new CyNodeLoadingHandler ();
			CyRelationLoadingHandler cyRelHandler = new CyRelationLoadingHandler ();
			
			cyNodeHandler.setDataManager ( dataMgr );
			cyNodeHandler.setNeo4jDriver ( neoDriver );
			
			cyRelHandler.setDataManager ( dataMgr );
			cyRelHandler.setNeo4jDriver ( neoDriver );

			CyNodeLoadingProcessor cyNodeProc = new CyNodeLoadingProcessor ();
			cyNodeProc.setConsumer ( cyNodeHandler );
			
			CyRelationLoadingProcessor cyRelProc = new CyRelationLoadingProcessor ();
			cyRelProc.setConsumer ( cyRelHandler );

			SimpleCyLoader cyloader = new SimpleCyLoader ();
			cyloader.setCyNodeLoader ( cyNodeProc );
			cyloader.setCyRelationLoader ( cyRelProc );
			cyloader.setDataManager ( dataMgr );
			
			return cyloader;
		});

		
		List<ConfigItem> config = new LinkedList<> ();
		config.add ( new ConfigItem ( 
			"places", 
			readResource ( "dbpedia_node_iris.sparql" ), 
			readResource ( "dbpedia_node_labels.sparql" ), 
			readResource ( "dbpedia_node_props.sparql" ), 
			readResource ( "dbpedia_rel_types.sparql" ), 
			readResource ( "dbpedia_rel_props.sparql" )			 
		));
		config.add ( new ConfigItem ( 
			"people", 
			readResource ( "dbpedia_people_iris.sparql" ), 
			readResource ( "dbpedia_people_labels.sparql" ), 
			readResource ( "dbpedia_people_props.sparql" ), 
			readResource ( "dbpedia_people_rel_types.sparql" ), 
			null			 
		));
		
		cymloader.setConfigItems ( config );

		cymloader.load ( NeoDataManagerTest.TDB_PATH );
		
		// TODO: test!
	}	
	
	
	@Test
	public void testSpring ()
	{
		// Use ConfigurableApplicationContext to show the try() block that it's a Closeable and let Java to clean up
	  // automatically 
		try ( ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "test_config.xml" ); )
		{			
			CypherLoader cyloader = beanCtx.getBean ( SimpleCyLoader.class );
			cyloader.load ( NeoDataManagerTest.TDB_PATH );
			// TODO: test
		}
	}	

	
	@Test
	public void testSpringMultiConfig ()
	{
		try ( ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "multi_config.xml" ); )
		{			
			MultiConfigCyLoader mloader = beanCtx.getBean ( MultiConfigCyLoader.class );
			mloader.load ( NeoDataManagerTest.TDB_PATH );
			// TODO: test
		}
	}	
	
}
