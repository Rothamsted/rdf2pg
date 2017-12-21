package uk.ac.rothamsted.rdf.neo4j.load;

import static info.marcobrandizi.rdfutils.jena.elt.JenaIoUtils.getLangOrFormat;

import org.junit.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import uk.ac.ebi.utils.io.IOUtils;
import uk.ac.rothamsted.rdf.neo4j.load.load.support.CypherNodeHandler;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>14 Dec 2017</dd></dl>
 *
 */
public class CypherLoaderIT
{
	@Test
	public void testFileLoader () throws Exception
	{
		try (
			Driver neoDriver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
		)
		{ 			
			CypherFileLoader cyloader = new CypherFileLoader ();
			CypherNodeHandler cyhandler = cyloader.getCypherLoadingHandler ();

			// TODO: configurator, multiple config sets
			cyloader.setNodeIrisSparql ( IOUtils.readResource ( "dbpedia_node_iris.sparql" ) );
			cyhandler.setLabelsSparql ( IOUtils.readResource ( "dbpedia_node_labels.sparql" ) );
			cyhandler.setNodePropsSparql ( IOUtils.readResource ( "dbpedia_node_props.sparql" ) );
			cyhandler.setNeo4jDriver ( neoDriver );
			
			cyloader.load ( "target/test-classes/dbpedia_places.ttl", null, getLangOrFormat ( "TURTLE" ).getRight () );
		}
	}
}
