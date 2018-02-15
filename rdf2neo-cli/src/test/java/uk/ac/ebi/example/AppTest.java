//package uk.ac.ebi.example;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//import java.io.ByteArrayOutputStream;
//import java.io.PrintStream;
//
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import uk.ac.rothamsted.rdf.neo4j.Rdf2NeoCli;
//
///**
// * Unit test for the example CLI {@link Rdf2NeoCli}.
// */
//public class AppTest
//{
//	private static Logger log = LoggerFactory.getLogger ( AppTest.class );
//
//	@BeforeClass
//	public static void setNoExitOption ()
//	{
//		// Prevents the CLI from invoking System.exit()
//		System.setProperty ( Rdf2NeoCli.NO_EXIT_PROP, "true" );
//	}
//	
//	
//	
//	@Test
//	public void testApp()
//	{
//		// Capture the output into memory
//		PrintStream outBkp = System.out;
//		ByteArrayOutputStream outBuf = new ByteArrayOutputStream ();
//		System.setOut ( new PrintStream ( outBuf ) );
//		
//		Rdf2NeoCli.main (  "a", "b", "c" );
//
//		System.setOut ( outBkp ); // restore the original output
//		
//		log.debug ( "CLI output:\n{}", outBuf.toString () );
//		assertTrue ( "Can't find CLI output!", outBuf.toString ().contains ( "a\tb\tc" ) );
//		assertEquals ( "Bad exit code!", 0, Rdf2NeoCli.getExitCode () );
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
//		Rdf2NeoCli.main (  "--help" );
//		
//		System.setOut ( outBkp );  // restore the original output
//
//		log.debug ( "CLI output:\n{}", outBuf.toString () );
//		assertTrue ( "Can't find CLI output!", outBuf.toString ().contains ( "*** Command Line Example ***" ) );
//		assertEquals ( "Bad exit code!", 1, Rdf2NeoCli.getExitCode () );
//	}
//
//}
