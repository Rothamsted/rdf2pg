package uk.ac.rothamsted.rdf.neo4j.load.support;

import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.system.Txn;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.marcobrandizi.rdfutils.jena.SparqlUtils;
import uk.ac.rothamsted.neo4j.utils.test.CypherTester;

/**
 * Runs {@link CypherLoadingHandler}-related tests.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 *
 */
public class CypherHandlersIT
{
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	@BeforeClass
	public static void initData () throws IOException {
		RdfDataManagerTest.initData ();
	}
	
	@AfterClass
	public static void closeData () throws IOException {
		RdfDataManagerTest.closeDataMgr ();
	}

	/**
	 * Loads some basic test RDF data into RDF.
	 */
	@Before
	public void initNeoData () throws IOException
	{
		initNeo ();

		try (	
				Driver neoDriver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
				RdfDataManager rdfMgr = new RdfDataManager ( RdfDataManagerTest.TDB_PATH );
			)
		{
			CyNodeLoadingHandler handler = new CyNodeLoadingHandler ();
			Neo4jDataManager neoMgr = new Neo4jDataManager ( neoDriver );
			
			// We need the same nodes in all tests
			handler.setRdfDataManager ( rdfMgr );
			handler.setNeo4jDataManager ( neoMgr );
			handler.setLabelsSparql ( RdfDataManagerTest.SPARQL_NODE_LABELS );
			handler.setNodePropsSparql ( RdfDataManagerTest.SPARQL_NODE_PROPS );
			
			Set<Resource> rdfNodes = 
				Stream.of ( iri ( "ex:1" ), iri ( "ex:2" ), iri ( "ex:3" ) )
				.map ( iri -> rdfMgr.getDataSet ().getDefaultModel ().createResource ( iri ) )
				.collect ( Collectors.toSet () );

			handler.accept ( rdfNodes );
		}
	}
	
	/**
	 * Facility to empty the Neo4j test DB. Auto-called by other tests.
	 * It's here as static method, because we call it from other clasess as well.
	 */
	public static void initNeo ()
	{
		try (	
				Driver neoDriver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
				Session session = neoDriver.session ();
			)
		{
			session.run ( "MATCH (n) DETACH DELETE n" );
		}
	}
	
	/**
	 * Test {@link CyNodeLoadingHandler} to see if nodes are mapped from RDF and loaded into Neo4J
	 */
	@Test
	public void testNodes () throws Exception
	{
		try (	
			Driver neoDriver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
		)
		{
			Session session = neoDriver.session ( AccessMode.READ );
			StatementResult cursor = session.run ( "MATCH ( n:TestNode ) RETURN COUNT ( n ) AS ct" );
			Assert.assertEquals ( "Wrong count for TestNode", 2, cursor.next ().get ( "ct" ).asLong () );
			
			cursor = session.run ( "MATCH ( n:TestNode { iri:'" + iri ( "ex:2" ) + "'} ) RETURN properties ( n ) AS props" );
			assertTrue ( "ex:2 not returned!", cursor.hasNext () );
			
			Map<String, Object> map = cursor.next ().get ( "props" ).asMap ();
			assertEquals (  "Wrong property!", "another string", map.get ( "attrib3" ) );
		}
	}
	
	
	/**
	 * Tests {@link CyRelationLoadingHandler} to see if relations are mapped from RDF and loaded into Neo4J.
	 */
	@Test
	public void testRelations () throws Exception
	{
		try (	
			Driver neoDriver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
			RdfDataManager rdfMgr = new RdfDataManager ( RdfDataManagerTest.TDB_PATH );
		)
		{
			CyRelationLoadingHandler handler = new CyRelationLoadingHandler ();
			Neo4jDataManager neoMgr = new Neo4jDataManager ( neoDriver );

			handler.setRdfDataManager ( rdfMgr );
			handler.setNeo4jDataManager ( neoMgr );
			handler.setRelationTypesSparql ( RdfDataManagerTest.SPARQL_REL_TYPES );
			handler.setRelationPropsSparql ( RdfDataManagerTest.SPARQL_REL_PROPS  );

			Set<QuerySolution> relSparqlRows = new HashSet<> ();
			Dataset dataSet = rdfMgr.getDataSet ();
			Txn.executeRead ( dataSet,  () ->
				SparqlUtils.select ( RdfDataManagerTest.SPARQL_REL_TYPES, rdfMgr.getDataSet ().getDefaultModel () )
					.forEachRemaining ( row -> relSparqlRows.add ( row ) )
			);

			handler.accept ( relSparqlRows );

			// Verify
			
			CypherTester tester = new CypherTester ( neoMgr );
			
			Assert.assertTrue (
				"Wrong count for relations",
				tester.ask ( "MATCH ()-[r]->() RETURN COUNT ( r ) = 3" )
			);

			Assert.assertTrue (
				"Wrong count for {1 relatedTo 2}!",
				tester.ask ( 
					"MATCH p = (:TestNode{ iri:$iri1 })-[:relatedTo]->(:TestNode{ iri:$iri2 }) RETURN COUNT ( p ) = 1",
					"iri1", iri ( "ex:1" ), "iri2", iri ( "ex:2" )
				)
			);
			
			Assert.assertTrue (
				"Wrong count for {3 derivedFrom 1}!",
				tester.ask ( 
					"MATCH p = (:SuperTestNode{ iri:$iri1 })-[:derivedFrom]->(:TestNode{ iri:$iri2 }) RETURN COUNT ( p ) = 1",
					"iri1", iri ( "ex:3" ), "iri2", iri ( "ex:1" )
				)
			);

			Assert.assertTrue (
				"Wrong count for {3 derivedFrom 1}!",
				tester.ask ( 
					"MATCH p = (:SuperTestNode{ iri:$iri1 })-[:derivedFrom]->(:TestNode{ iri:$iri2 }) RETURN COUNT ( p ) = 1",
					"iri1", iri ( "ex:3" ), "iri2", iri ( "ex:1" )
				)
			);

			Assert.assertTrue (
				"Wrong count for {3 derivedFrom 1}!",
				tester.ask ( 
					"MATCH p = (:SuperTestNode{ iri:$iri1 })-[:derivedFrom]->(:TestNode{ iri:$iri2 }) RETURN COUNT ( p ) = 1",
					"iri1", iri ( "ex:3" ), "iri2", iri ( "ex:1" )
				)
			);
			
			
			Assert.assertTrue (
				"reified relation, wrong property value for 'note'!",
				tester.compare (
					// Test against the Cypher result
					notesv -> {
						List<Object> notes = notesv.asList ();
						if ( notes == null || notes.isEmpty () ) return false;

						// notes collection is sorted, then compared to the sorted values in the reference
						return notes
							.stream ()
							.sorted ()
							.collect ( Collectors.toList () )
							.equals ( 
								Arrays.asList ( new String[] { "Another Note", "Reified Relation" } )
							);
					},
					// the relation containing .note
					"MATCH (:TestNode{ iri:$iri1 })-[r:relatedTo]->(:AdditionalLabel{ iri:$iri2 })\n"
					+ "RETURN r.note\n",
					"iri1", iri ( "ex:2" ), "iri2", iri ( "ex:3" )
				)
			);
		} // try
	}	// testRelations

}
