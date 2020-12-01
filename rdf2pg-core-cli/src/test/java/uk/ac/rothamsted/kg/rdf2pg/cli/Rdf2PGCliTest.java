package uk.ac.rothamsted.kg.rdf2pg.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

/**
 * Test the basic machinery to define/invoke rdf2pg command lines.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>27 Nov 2020</dd></dl>
 *
 */
public class Rdf2PGCliTest
{
	private static final String TEST_OPTION = "path/to/foo/file.xml";
	private static final String TEST_CMD_DESCRIPTION = "A Test Command Line";
	private static String result;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	/**
	 * Of course, this is just a probe to verify the invocations in the tests.
	 */
	@Component
	@Command (
		name = "testCmd", description = TEST_CMD_DESCRIPTION			
	)	
	public static class TestCmd extends ConfigFileCliCommand
	{
		@Override
		public Integer call () throws Exception
		{
			result = this.xmlConfigPath;
			log.info ( "Result set to: '{}'. Now returning 0", result );
			return 0;
		}
	}
	
	@Test
	public void testCliInvocation ()
	{
		Rdf2PGCli.main ( "--config", TEST_OPTION );
		Assert.assertEquals ( "Wrong result from CLI invocation!", TEST_OPTION, result );
	}	

	@Test
	public void testCliHelp ()
	{
		// Capture the output into memory
		PrintStream outBkp = System.out;
		ByteArrayOutputStream outBuf = new ByteArrayOutputStream ();
		System.setOut ( new PrintStream ( outBuf ) );

		Rdf2PGCli.main ( "-h" );
		
		System.setOut ( outBkp );  // restore the original output

		log.debug ( "CLI output:\n{}", outBuf.toString () );
		assertTrue ( "Can't find CLI usage output (--config)!", outBuf.toString ().contains ( "--config" ) );
		assertTrue ( "Can't find CLI usage output (description)!", outBuf.toString ().contains ( TEST_CMD_DESCRIPTION ) );
		
		assertEquals ( "Wrong exit code from --help invocation!", ExitCode.USAGE, Rdf2PGCli.getExitCode () );
	}	

}
