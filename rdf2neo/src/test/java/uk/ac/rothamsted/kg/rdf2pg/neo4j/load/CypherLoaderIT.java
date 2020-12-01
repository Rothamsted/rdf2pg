package uk.ac.rothamsted.kg.rdf2pg.neo4j.load;

import static uk.ac.ebi.utils.io.IOUtils.readResource;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import uk.ac.ebi.utils.io.IOUtils;
import uk.ac.rothamsted.kg.rdf2pg.load.MultiConfigPGLoader;
import uk.ac.rothamsted.kg.rdf2pg.load.PropertyGraphLoader;
import uk.ac.rothamsted.kg.rdf2pg.load.support.rdf.DataTestUtils;
import uk.ac.rothamsted.kg.rdf2pg.load.support.rdf.RdfDataManager;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.MultiConfigNeo4jLoader;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.Neo4jConfigItem;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.SimpleCyLoader;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyNodeLoadingHandler;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyNodeLoadingProcessor;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyRelationLoadingHandler;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyRelationLoadingProcessor;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CypherHandlersIT;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.Neo4jDataManager;

/**
 * Basic tests for {@link SimpleCyLoader} and {@link MultiConfigPGLoader}.
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
		DataTestUtils.initDBpediaDataSet ();
	}
	
	@Before
	public void initNeo () {
		CypherHandlersIT.initNeo ();
	}
	
	@Test
	public void testLoading () throws Exception
	{
		try (
			var neoDriver = GraphDatabase.driver ( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
			var cyloader = new SimpleCyLoader ();
			var rdfMgr = new RdfDataManager ( DataTestUtils.TDB_PATH );
		)
		{
			// You don't want to do this, see #testSpring()
			
			Neo4jDataManager neoMgr = new Neo4jDataManager ( neoDriver );
			
			CyNodeLoadingHandler cyNodeHandler = new CyNodeLoadingHandler ();
			CyRelationLoadingHandler cyRelHandler = new CyRelationLoadingHandler ();
			
			cyNodeHandler.setLabelsSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_LABELS );
			cyNodeHandler.setNodePropsSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_PROPS );
			cyNodeHandler.setRdfDataManager ( rdfMgr );
			cyNodeHandler.setNeo4jDataManager ( neoMgr );
			
			cyRelHandler.setRelationTypesSparql ( DataTestUtils.DBPEDIA_SPARQL_REL_TYPES );
			cyRelHandler.setRelationPropsSparql ( DataTestUtils.DBPEDIA_SPARQL_REL_PROPS );
			cyRelHandler.setRdfDataManager ( rdfMgr );
			cyRelHandler.setNeo4jDataManager ( neoMgr );

			CyNodeLoadingProcessor cyNodeProc = new  CyNodeLoadingProcessor();
			cyNodeProc.setNodeIrisSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_IRIS );
			cyNodeProc.setBatchJob ( cyNodeHandler );
			
			CyRelationLoadingProcessor cyRelProc = new CyRelationLoadingProcessor ();
			cyRelProc.setConsumer ( cyRelHandler );

			cyloader.setPGNodeLoader ( cyNodeProc );
			cyloader.setPGRelationLoader ( cyRelProc );
			
			cyloader.load ( DataTestUtils.TDB_PATH );
			// TODO: test!
			
		} // try neoDriver
	}
	

	@Test
	public void testMultiConfigLoading () throws Exception
	{
		try ( var cymloader = new MultiConfigNeo4jLoader (); )
		{
			cymloader.setPGLoaderFactory ( () -> 
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
				cyRelProc.setConsumer ( cyRelHandler );
	
				SimpleCyLoader cyloader = new SimpleCyLoader ();
				cyloader.setPGNodeLoader ( cyNodeProc );
				cyloader.setPGRelationLoader ( cyRelProc );
				cyloader.setRdfDataManager ( rdfMgr );
				
				return cyloader;
			});
	
			
			List<Neo4jConfigItem> config = new LinkedList<> ();
			{
				var cfgi = new Neo4jConfigItem ();
				cfgi.setName ( "places" );
				cfgi.setNodeIrisSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_IRIS );
				cfgi.setLabelsSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_LABELS );
				cfgi.setNodePropsSparql ( DataTestUtils.DBPEDIA_SPARQL_NODE_PROPS );
				cfgi.setRelationTypesSparql ( DataTestUtils.DBPEDIA_SPARQL_REL_TYPES );
				cfgi.setRelationPropsSparql ( DataTestUtils.DBPEDIA_SPARQL_REL_PROPS );
				config.add ( cfgi );
			}

			{
				var cfgi = new Neo4jConfigItem ();
				cfgi.setName ( "people" );

				cfgi.setNodeIrisSparql ( DataTestUtils.DBPEDIA_SPARQL_PEOPLE_IRIS );
				cfgi.setLabelsSparql ( DataTestUtils.DBPEDIA_SPARQL_PEOPLE_LABELS );
				cfgi.setNodePropsSparql ( DataTestUtils.DBPEDIA_SPARQL_PEOPLE_PROPS );
				cfgi.setRelationTypesSparql ( DataTestUtils.DBPEDIA_SPARQL_PEOPLE_REL_TYPES );
				config.add ( cfgi );
			}
			
			cymloader.setConfigItems ( config );
	
			cymloader.load ( DataTestUtils.TDB_PATH );
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
			PropertyGraphLoader cyloader = beanCtx.getBean ( SimpleCyLoader.class );
			cyloader.load ( DataTestUtils.TDB_PATH );
			// TODO: test
		}
	}	

	
	@Test
	public void testSpringMultiConfig ()
	{
		try ( 
			ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "multi_config.xml" );
			MultiConfigNeo4jLoader mloader = MultiConfigNeo4jLoader.getSpringInstance ( beanCtx, MultiConfigNeo4jLoader.class );				
		)
		{			
			mloader.load ( DataTestUtils.TDB_PATH );
			// TODO: test
		}
	}	

	
	@Test
	public void testNeoIndexing ()
	{
		try ( 
			ConfigurableApplicationContext beanCtx = new ClassPathXmlApplicationContext ( "multi_config_indexing.xml" );
			MultiConfigNeo4jLoader mloader = MultiConfigNeo4jLoader.getSpringInstance ( beanCtx, MultiConfigNeo4jLoader.class );				
		)
		{			
			mloader.load ( DataTestUtils.TDB_PATH );
			// TODO: test
		}
	}		
}
