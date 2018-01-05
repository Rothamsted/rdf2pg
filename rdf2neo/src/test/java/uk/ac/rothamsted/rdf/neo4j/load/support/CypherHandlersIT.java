package uk.ac.rothamsted.rdf.neo4j.load.support;

import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.neo4j.driver.v1.Values.parameters;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
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

import com.google.common.collect.Sets;

import info.marcobrandizi.rdfutils.namespaces.NamespaceUtils;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.CyNodeLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.CyRelationLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.NeoDataManager;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.Relation;

/**
 * TODO: comment me!
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
		NeoDataManagerTest.initData ();
	}

	@Before
	public void initNeo ()
	{
		initNeoStatic ();

		try (	
				Driver neoDriver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
			)
		{
			NeoDataManagerTest dmtest = new NeoDataManagerTest ();
			
			// We need the same nodes in all tests
			// 
			
			@SuppressWarnings ( "static-access" )
			NeoDataManager dataMgr = dmtest.getDataMgr ();
			
			CyNodeLoadingHandler handler = new CyNodeLoadingHandler ( 
				dataMgr, neoDriver, NeoDataManagerTest.SPARQL_NODE_LABELS, NeoDataManagerTest.SPARQL_NODE_PROPS 
			);
			
			Set<Resource> jnodes = Stream.of ( "ex:1", "ex:2", "ex:3" )
			.map ( NamespaceUtils::iri )
			.map ( iri -> dataMgr.getDataSet ().getDefaultModel ().getResource ( iri ) )
			.collect ( Collectors.toSet () );

			handler.accept ( jnodes );
		}
	}
	
	public static void initNeoStatic ()
	{
		try (	
				Driver neoDriver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
				Session session = neoDriver.session ();
			)
		{
			session.run ( "MATCH (n) DETACH DELETE n" );
		}
	}
	
	
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
	
	
	@Test
	public void testRelations () throws Exception
	{
		try (	
			Driver neoDriver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
		)
		{
			NeoDataManagerTest dmtest = new NeoDataManagerTest ();
			
			@SuppressWarnings ( "static-access" )
			NeoDataManager dataMgr = dmtest.getDataMgr ();
			
			CyRelationLoadingHandler handler = new CyRelationLoadingHandler ( 
				dataMgr, neoDriver, NeoDataManagerTest.SPARQL_REL_TYPES, NeoDataManagerTest.SPARQL_REL_PROPS 
			);

			Set<Relation> relations = new HashSet<> ();
			dataMgr.processRelationIris ( NeoDataManagerTest.SPARQL_REL_TYPES, 0, (long) 1E6, row -> 
			{
				Relation rel = dataMgr.getRelation ( row );
				relations.add ( rel );
			});
			handler.accept ( relations );
				
			Session session = neoDriver.session ( AccessMode.READ );

			StatementResult cursor = session.run ( "MATCH ()-[r]->() RETURN COUNT ( r ) AS ct" );
			Assert.assertEquals ( "Wrong count for relations", 3, cursor.next ().get ( "ct" ).asLong () );

			cursor = session.run ( 
				"MATCH p = (:TestNode{ iri:$iri1 })-[:relatedTo]->(:TestNode{ iri:$iri2 }) RETURN COUNT ( p ) AS ct", 
				parameters ( "iri1", iri ( "ex:1" ), "iri2", iri ( "ex:2" ) ) 
			);
			Assert.assertEquals ( "Wrong count for {1 relatedTo 2}!", 1, cursor.next ().get ( "ct" ).asLong () );

			cursor = session.run ( 
				"MATCH p = (:SuperTestNode{ iri:$iri1 })-[:derivedFrom]->(:TestNode{ iri:$iri2 }) RETURN COUNT ( p ) AS ct", 
				parameters ( "iri1", iri ( "ex:3" ), "iri2", iri ( "ex:1" ) ) 
			);
			Assert.assertEquals ( "Wrong count for {3 derivedFrom 1}!", 1, cursor.next ().get ( "ct" ).asLong () );
			
			cursor = session.run ( 
				"MATCH (:TestNode{ iri:$iri1 })-[r:relatedTo]->(:AdditionalLabel{ iri:$iri2 }) RETURN r.note AS note", 
				parameters ( "iri1", iri ( "ex:2" ), "iri2", iri ( "ex:3" ) ) 
			);
			assertTrue ( "{2 relatedTo 3} not found!", cursor.hasNext () );
			Set<String> values = cursor
				.next ()
				.get ( "note" )
				.asList ()
				.stream ()
				.map ( v -> (String) v )
				.collect ( Collectors.toSet () );
			Set<String> refValues = new HashSet<> ( Arrays.asList ( new String[] { "Reified Relation", "Another Note" } ) ) ;
			assertTrue ( 
				"reified relation, wrong property value for 'note'!", 
				Sets.difference ( values, refValues ).isEmpty () 
			);
		}
	}	

}
