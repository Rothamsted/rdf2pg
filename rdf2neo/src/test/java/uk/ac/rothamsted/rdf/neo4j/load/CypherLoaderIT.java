package uk.ac.rothamsted.rdf.neo4j.load;

import static info.marcobrandizi.rdfutils.jena.elt.JenaIoUtils.getLangOrFormat;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

import uk.ac.ebi.utils.io.IOUtils;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyNodeLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyRelationLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.support.CypherHandlersIT;
import uk.ac.rothamsted.rdf.neo4j.load.support.NeoDataManager;

/**
 * TODO: comment me!
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
		NeoDataManager.setDoCleanTdbDirectory ( true );
	}
	
	@Test
	public void testFileLoader () throws Exception
	{
		try (
			Driver neoDriver = GraphDatabase.driver( "bolt://127.0.0.1:7687", AuthTokens.basic ( "neo4j", "test" ) );
		)
		{ 			
			CypherFileLoader cyloader = new CypherFileLoader ();
			
			CyNodeLoadingHandler cyNodehandler = cyloader.getCyNodeLoadingHandler ();
			CyRelationLoadingHandler cyRelhandler = cyloader.getCyRelationLoadingHandler ();
			
			// TODO: configurator, multiple config sets
			
			cyloader.setNodeIrisSparql ( IOUtils.readResource ( "dbpedia_node_iris.sparql" ) );
			cyNodehandler.setLabelsSparql ( IOUtils.readResource ( "dbpedia_node_labels.sparql" ) );
			cyNodehandler.setNodePropsSparql ( IOUtils.readResource ( "dbpedia_node_props.sparql" ) );
			cyNodehandler.setNeo4jDriver ( neoDriver );
			
			cyRelhandler.setRelationTypesSparql ( IOUtils.readResource ( "dbpedia_rel_types.sparql" ) );
			cyRelhandler.setRelationPropsSparql ( IOUtils.readResource ( "dbpedia_rel_props.sparql" ) );
			cyRelhandler.setNeo4jDriver ( neoDriver );

			cyloader.load ( "target/test-classes/dbpedia_places.ttl", null, getLangOrFormat ( "TURTLE" ).getRight () );
			
			// TODO: test!
		}
	}
}
