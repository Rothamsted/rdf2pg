package uk.ac.rothamsted.rdf.neo4j.load;

import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import uk.ac.ebi.utils.io.IOUtils;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyNodeLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyRelationLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.support.CypherHandlersIT;
import uk.ac.rothamsted.rdf.neo4j.load.support.NeoDataManager;
import uk.ac.rothamsted.rdf.neo4j.load.support.NeoDataManagerTest;

/**
 * Basic tests for {@link SimpleCyLoader}.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>14 Dec 2017</dd></dl>
 *
 */
public class CypherLoaderIT
{
	@Before
	public void initNeo () {
		CypherHandlersIT.initNeo ();
	}
	
	@Test
	public void testLoading () throws Exception
	{
		try (
			Driver neoDriver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
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
			
			Dataset ds = dataMgr.getDataSet ();
			Txn.executeWrite ( ds, () -> 
				ds.getDefaultModel ().read ( 
					"file:target/test-classes/dbpedia_places.ttl", 
					null, 
					"TURTLE" 
			));

			cyloader.load ( NeoDataManagerTest.TDB_PATH );
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
			NeoDataManager dataMgr = beanCtx.getBean ( NeoDataManager.class );
			dataMgr.open ( NeoDataManagerTest.TDB_PATH );
			
			Dataset ds = dataMgr.getDataSet ();
			Txn.executeWrite ( ds, () -> 
				ds.getDefaultModel ().read ( 
					"file:target/test-classes/dbpedia_places.ttl", 
					null, 
					"TURTLE" 
			));
			
			CypherLoader cyloader = beanCtx.getBean ( SimpleCyLoader.class );
			
			cyloader.load ( NeoDataManagerTest.TDB_PATH );
			// TODO: test

			MultiConfigCyLoader mloader = beanCtx.getBean ( MultiConfigCyLoader.class );
			mloader.getCypherLoaderFactory ().getObject ();
		}
	}
}
