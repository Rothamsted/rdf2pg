package uk.ac.rothamsted.rdf.neo4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.rothamsted.rdf.neo4j.load.support.RdfDataManager;

/**
 * Unit test for the example CLI {@link Rdf2NeoCli}.
 */
public class Rdf2NeoCliIT
{
	public static final String TDB_PATH = "target/test_tdb";

	private static Logger log = LoggerFactory.getLogger ( Rdf2NeoCliIT.class );
	
	@BeforeClass
	public static void setNoExitOption ()
	{
		// Prevents the CLI from invoking System.exit()
		System.setProperty ( Rdf2NeoCli.NO_EXIT_PROP, "true" );
	}
	
	@BeforeClass
	public static void initTDB ()
	{
		try (
			RdfDataManager rdfMgr = new RdfDataManager ( TDB_PATH );
	  )
		{
			Dataset ds = rdfMgr.getDataSet ();
			for ( String ttlPath: new String [] { "dbpedia_places.ttl", "dbpedia_people.ttl" } )
			Txn.executeWrite ( ds, () -> 
				ds.getDefaultModel ().read ( 
					"file:target/examples/dbpedia/" + ttlPath, 
					null, 
					"TURTLE" 
			));
		}	
	}	
	
	
	@Test
	public void testInvocation ()
	{
		Rdf2NeoCli.main ( "--config", "src/main/assembly/resources/examples/dbpedia/config.xml", TDB_PATH );
		// TODO: test!
		assertEquals ( "Bad exit code!", 0, Rdf2NeoCli.getExitCode () );
	}

	@Test
	public void testHelpOption()
	{
		// Capture the output into memory
		PrintStream outBkp = System.out;
		ByteArrayOutputStream outBuf = new ByteArrayOutputStream ();
		System.setOut ( new PrintStream ( outBuf ) );

		Rdf2NeoCli.main (  "--help" );
		
		System.setOut ( outBkp );  // restore the original output

		log.debug ( "CLI output:\n{}", outBuf.toString () );
		assertTrue ( "Can't find CLI output!", outBuf.toString ().contains ( "*** Rdf2Neo, the RDF-to-Neo4j converter ***" ) );
		assertEquals ( "Bad exit code!", 1, Rdf2NeoCli.getExitCode () );
	}

}
