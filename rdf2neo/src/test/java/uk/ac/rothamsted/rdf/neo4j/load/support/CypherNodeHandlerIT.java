package uk.ac.rothamsted.rdf.neo4j.load.support;

import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.junit.Test;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.marcobrandizi.rdfutils.namespaces.NamespaceUtils;
import junit.framework.Assert;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.CypherNodeHandler;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.NeoDataManager;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 *
 */
public class CypherNodeHandlerIT
{
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	@Test
	public void testNodes () throws Exception
	{
		try (	
			Driver neoDriver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
		)
		{
			NeoDataManagerTest dmtest = new NeoDataManagerTest ();
			dmtest.testBasics ();
			
			NeoDataManager dataMgr = dmtest.getDataMgr ();
			
			CypherNodeHandler handler = 
				new CypherNodeHandler ( dataMgr, neoDriver, NeoDataManagerTest.SPARQL_LABELS, NeoDataManagerTest.SPARQL_PROPS );
			
			Set<Resource> jnodes = Stream.of ( "ex:1", "ex:2", "ex:3" )
			.map ( NamespaceUtils::iri )
			.map ( iri -> dataMgr.getDataSet ().getDefaultModel ().getResource ( iri ) )
			.collect ( Collectors.toSet () );

			handler.accept ( jnodes );
	
			Session session = neoDriver.session ( AccessMode.READ );
			StatementResult cursor = session.run ( "MATCH ( n:TestNode ) RETURN COUNT ( n ) AS ct" );
			Assert.assertEquals ( "Wrong count for TestNode", 2, cursor.next ().get ( "ct" ).asLong () );
			
			cursor = session.run ( "MATCH ( n:TestNode { iri:'" + iri ( "ex:2" ) + "'} ) RETURN properties ( n ) AS props" );
			assertTrue ( "ex:2 not returned!", cursor.hasNext () );
			
			Map<String, Object> map = cursor.next ().get ( "props" ).asMap ();
			assertEquals (  "Wrong property!", "another string", map.get ( "attrib3" ) );
		}
	}
}
