package uk.ac.rothamsted.neo4j.utils;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Values;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;

/**
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>7 Oct 2024</dd></dl>
 *
 */
public class Neo4jUtilsIT
{
	// TODO: duplicated from rdf2neo, factorise
	
	public static final String NEO_TEST_URL = "bolt://127.0.0.1:" + System.getProperty ( "neo4j.server.boltPort" );
	public static final String NEO_TEST_USER = "neo4j";
	public static final String NEO_TEST_PWD = "testTest";
	
	private static Driver neoDriver = GraphDatabase.driver ( 
		NEO_TEST_URL, AuthTokens.basic ( NEO_TEST_USER, NEO_TEST_PWD )
	);
	
	private static int testNodesSize = 1000;
	
	@BeforeClass
	public static void init ()
	{
		try ( var session = neoDriver.session () )
		{
			session.executeWriteWithoutResult ( tx ->
			{ 
				for ( int i = 0; i < testNodesSize; i++ )
					tx.run ( 
						"MATCH (n:PagerTestNode) DELETE n"
					);				
			});

			session.executeWriteWithoutResult ( tx ->
			{ 
				for ( int i = 0; i < testNodesSize; i++ )
					tx.run ( 
						"CREATE (:PagerTestNode { idx: $idx })",
						Values.parameters ( "idx", i ) 
					);				
			});
		}
	}
	
	
	@AfterClass
	public static void close ()
	{		
		neoDriver.close ();
	}

	
	@Test
	public void testScan ()
	{
		long pageSize = 20;
		var pager = Neo4jUtils.paginatedRead (
			(tx, offset) -> tx.run (
				"MATCH ( n: PagerTestNode ) RETURN n.idx AS idx SKIP $offset LIMIT $pageSize",
				Values.parameters ( "offset", offset, "pageSize", pageSize )
			), 
			neoDriver,
			pageSize
		);
		
		int i = 0;
		while ( pager.hasNext () )
			assertEquals ( "Wrong item fetched!", i++, pager.next ().get ( "idx", -1 ) );
		
		assertEquals ( "Wrong size fetched!", testNodesSize, i );
	}
	
	@Test
	public void testScanEmpty ()
	{
		long pageSize = 20;
		var pager = Neo4jUtils.paginatedRead (
			(tx, offset) -> tx.run (
				"MATCH ( n: PagerTestNodeFoo ) RETURN n.idx AS idx SKIP $offset LIMIT $pageSize",
				Values.parameters ( "offset", offset, "pageSize", pageSize )
			), 
			neoDriver,
			pageSize
		);
		
		Assert.assertFalse ( "pager should be false against empty Cypher!", pager.hasNext () );
	}

	@Test
	public void testScanSinglePage ()
	{
		long pageSize = testNodesSize;
		var pager = Neo4jUtils.paginatedRead (
			(tx, offset) -> tx.run (
				"MATCH ( n: PagerTestNode ) RETURN n.idx AS idx SKIP $offset LIMIT $pageSize",
				Values.parameters ( "offset", offset, "pageSize", pageSize )
			), 
			neoDriver,
			pageSize
		);
		
		int i = 0;
		while ( pager.hasNext () )
			assertEquals ( "Wrong item fetched (single page query)!", i++, pager.next ().get ( "idx", -1 ) );
		
		assertEquals ( "Wrong size fetched (single page query)!", testNodesSize, i );
	}
	
}
