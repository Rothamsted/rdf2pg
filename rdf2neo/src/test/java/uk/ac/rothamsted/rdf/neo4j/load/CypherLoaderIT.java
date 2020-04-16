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
import uk.ac.rothamsted.rdf.neo4j.load.support.Neo4jDataManager;
import uk.ac.rothamsted.rdf.neo4j.load.support.RdfDataManager;
import uk.ac.rothamsted.rdf.neo4j.load.support.RdfDataManagerTest;

/**
 * Basic tests for {@link SimpleCyLoader} and {@link MultiConfigCyLoader}.
 *
 * As developer user, you're probably interested in invoking the converter using Spring configuration,
 * @see {@link #testSpringMultiConfig()} or {@link #testNeoIndexing()}. 
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
			RdfDataManager rdfMgr = new RdfDataManager ( RdfDataManagerTest.TDB_PATH );
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
	
	
	@Before
	public void initNeo () {
		CypherHandlersIT.initNeo ();
	}
	
	@Test
	public void testLoading () throws Exception
	{
		try (
			Driver neoDriver = GraphDatabase.driver ( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
			RdfDataManager rdfMgr = new RdfDataManager ( RdfDataManagerTest.TDB_PATH );
			SimpleCyLoader cyloader = new SimpleCyLoader ();
		)
		{
			// You don't want to do this, see #testSpring()

			Neo4jDataManager neoMgr = new Neo4jDataManager ( neoDriver );
			
			CyNodeLoadingHandler cyNodeHandler = new CyNodeLoadingHandler ();
			CyRelationLoadingHandler cyRelHandler = new CyRelationLoadingHandler ();
			
			cyNodeHandler.setLabelsSparql ( IOUtils.readResource ( "dbpedia_node_labels.sparql" ) );
			cyNodeHandler.setNodePropsSparql ( IOUtils.readResource ( "dbpedia_node_props.sparql" ) );
			cyNodeHandler.setRdfDataManager ( rdfMgr );
			cyNodeHandler.setNeo4jDataManager ( neoMgr );
			
			cyRelHandler.setRelationTypesSparql ( IOUtils.readResource ( "dbpedia_rel_types.sparql" ) );
			cyRelHandler.setRelationPropsSparql ( IOUtils.readResource ( "dbpedia_rel_props.sparql" ) );
			cyRelHandler.setRdfDataManager ( rdfMgr );
			cyRelHandler.setNeo4jDataManager ( neoMgr );

			CyNodeLoadingProcessor cyNodeProc = new CyNodeLoadingProcessor ();
			cyNodeProc.setNodeIrisSparql ( IOUtils.readResource ( "dbpedia_node_iris.sparql" ) );
			cyNodeProc.setBatchJob ( cyNodeHandler );
			
			CyRelationLoadingProcessor cyRelProc = new CyRelationLoadingProcessor ();
			cyRelProc.setBatchJob ( cyRelHandler );

			cyloader.setCyNodeLoader ( cyNodeProc );
			cyloader.setCyRelationLoader ( cyRelProc );
			
			cyloader.load ( RdfDataManagerTest.TDB_PATH );
			// TODO: test!
			
		} // try neoDriver
	}
	

	@Test
	public void testMultiConfigLoading () throws Exception
	{
		try ( MultiConfigCyLoader cymloader = new MultiConfigCyLoader (); )
		{
			cymloader.setCypherLoaderFactory ( () -> 
			{
				// You don't want to do this, see #testSpring()
				
				RdfDataManager rdfMgr = new RdfDataManager ();
				Driver neoDriver = GraphDatabase.driver ( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
				Neo4jDataManager neoMgr = new Neo4jDataManager ( neoDriver );			
				
				CyNodeLoadingHandler cyNodeHandler = new CyNodeLoadingHandler ();
				CyRelationLoadingHandler cyRelHandler = new CyRelationLoadingHandler ();
				
				cyNodeHandler.setRdfDataManager ( rdfMgr );
				cyNodeHandler.setNeo4jDataManager ( neoMgr );
				
				cyRelHandler.setRdfDataManager ( rdfMgr );
				cyRelHandler.setNeo4jDataManager ( neoMgr );
	
				CyNodeLoadingProcessor cyNodeProc = new CyNodeLoadingProcessor ();
				cyNodeProc.setBatchJob ( cyNodeHandler );
				
				CyRelationLoadingProcessor cyRelProc = new CyRelationLoadingProcessor ();
				cyRelProc.setBatchJob ( cyRelHandler );
	
				SimpleCyLoader cyloader = new SimpleCyLoader ();
				cyloader.setCyNodeLoader ( cyNodeProc );
				cyloader.setCyRelationLoader ( cyRelProc );
				cyloader.setRdfDataManager ( rdfMgr );
				
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
	
			cymloader.load ( RdfDataManagerTest.TDB_PATH );
		}
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
			cyloader.load ( RdfDataManagerTest.TDB_PATH );
			// TODO: test
		}
	}	

	
	@Test
	public void testSpringMultiConfig ()
	{
		try ( 
			ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "multi_config.xml" );
			MultiConfigCyLoader mloader = MultiConfigCyLoader.getSpringInstance ( beanCtx );				
		)
		{			
			mloader.load ( RdfDataManagerTest.TDB_PATH );
			// TODO: test
		}
	}	

	
	@Test
	public void testNeoIndexing ()
	{
		try ( 
			ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "multi_config_indexing.xml" );
			MultiConfigCyLoader mloader = MultiConfigCyLoader.getSpringInstance ( beanCtx );				
		)
		{			
			mloader.load ( RdfDataManagerTest.TDB_PATH );
			// TODO: test
		}
	}		
}
