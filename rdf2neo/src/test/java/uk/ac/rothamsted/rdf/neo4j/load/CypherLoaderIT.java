package uk.ac.rothamsted.rdf.neo4j.load;

import static info.marcobrandizi.rdfutils.jena.elt.JenaIoUtils.getLangOrFormat;

import org.junit.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import uk.ac.ebi.utils.io.IOUtils;

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
			cyloader.setLabelSparqlQueries ( IOUtils.readResource ( "dbpedia_node_labels.sparql" ) );
			cyloader.setNodePropSparqlQueries ( IOUtils.readResource ( "dbpedia_node_props.sparql" ) );
			cyloader.setNeo4jDriver ( neoDriver );
			
			cyloader.load ( "target/test-classes/dbpedia_places.ttl", null, getLangOrFormat ( "TURTLE" ).getRight () );
		}
	}
}
