package uk.ac.rothamsted.rdf.neo4j.load.support;

import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		NeoDataManagerTest dmtest = new NeoDataManagerTest ();
		dmtest.testLuceneNodeFunctions ();
		
		// DB is created with an expired password, which needs to be changed.
		// TODO: move it onto a utility module
		NeoDataManager dataMgr = dmtest.getDataMgr ();
		Driver driver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
		
		
		CypherNodeHandler handler = new CypherNodeHandler ( dataMgr, driver );
		handler.accept ( dataMgr.getNodeIris ().collect ( Collectors.toSet () ) );

		Session session = driver.session ( AccessMode.READ );
		StatementResult cursor = session.run ( "MATCH ( n:TestNode ) RETURN COUNT ( n ) AS ct" );
		Assert.assertEquals ( "Wrong count for TestNode", 2, cursor.next ().get ( "ct" ).asLong () );
		
		cursor = session.run ( "MATCH ( n:TestNode { iri:'" + iri ( "ex:2" ) + "'} ) RETURN properties ( n ) AS props" );
		assertTrue ( "ex:2 not returned!", cursor.hasNext () );
		
		Map<String, Object> map = cursor.next ().get ( "props" ).asMap ();
		assertEquals (  "Wrong property!", "another string", map.get ( "attrib3" ) );
		
		driver.close ();
	}
}
