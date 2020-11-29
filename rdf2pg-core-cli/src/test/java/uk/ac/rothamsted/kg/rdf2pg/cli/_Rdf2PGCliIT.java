package uk.ac.rothamsted.kg.rdf2pg.cli;
//package uk.ac.rothamsted.kg.rdf2pg.cli;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//import java.io.ByteArrayOutputStream;
//import java.io.PrintStream;
//
//import org.apache.jena.query.Dataset;
//import org.apache.jena.system.Txn;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import uk.ac.rothamsted.kg.rdf2pg.cli.Rdf2PGCli;
//import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManager;
//
///**
// * Unit test for the example CLI {@link Rdf2PGCli}.
// */
//public class _Rdf2PGCliIT
//{
//	public static final String TDB_PATH = "target/test_tdb";
//
//	private static Logger log = LoggerFactory.getLogger ( _Rdf2PGCliIT.class );
//	
//	@BeforeClass
//	public static void setNoExitOption ()
//	{
//		// Prevents the CLI from invoking System.exit()
//		System.setProperty ( Rdf2PGCli.NO_EXIT_PROP, "true" );
//	}
//	
//	@BeforeClass
//	public static void initTDB ()
//	{
//		try (
//			RdfDataManager rdfMgr = new RdfDataManager ( TDB_PATH );
//	  )
//		{
//			Dataset ds = rdfMgr.getDataSet ();
//			for ( String ttlPath: new String [] { "dbpedia_places.ttl", "dbpedia_people.ttl" } )
//			Txn.executeWrite ( ds, () -> 
//				ds.getDefaultModel ().read ( 
//					"file:target/examples/dbpedia/" + ttlPath, 
//					null, 
//					"TURTLE" 
//			));
//		}	
//	}	
//	
//	
//	@Test
//	public void testInvocation ()
//	{
//		Rdf2PGCli.main ( "--config", "src/main/assembly/resources/examples/dbpedia/config.xml", TDB_PATH );
//		// TODO: test!
//		assertEquals ( "Bad exit code!", 0, Rdf2PGCli.getExitCode () );
//	}
//
//	@Test
//	public void testHelpOption()
//	{
//		// Capture the output into memory
//		PrintStream outBkp = System.out;
//		ByteArrayOutputStream outBuf = new ByteArrayOutputStream ();
//		System.setOut ( new PrintStream ( outBuf ) );
//
//		Rdf2PGCli.main (  "--help" );
//		
//		System.setOut ( outBkp );  // restore the original output
//
//		log.debug ( "CLI output:\n{}", outBuf.toString () );
//		assertTrue ( "Can't find CLI output!", outBuf.toString ().contains ( "*** Rdf2Neo, the RDF-to-Neo4j converter ***" ) );
//		assertEquals ( "Bad exit code!", 1, Rdf2PGCli.getExitCode () );
//	}
//
//}
