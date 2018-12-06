package uk.ac.rothamsted.rdf.neo4j;

import static java.lang.System.out;

import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.rothamsted.rdf.neo4j.load.MultiConfigCyLoader;

/**
 * A skeleton for a typical Command Line entry point.
 * 
 */
public class Rdf2NeoCli
{
	/**
	 * If you set this to true, main() will not invoke {@link System#exit(int)}. This is useful in unit tests.
	 */
	public static final String NO_EXIT_PROP = "uk.ac.ebi.debug.no_jvm_exit"; 
			
	private static int exitCode = 0;
	
	private static Logger log = LoggerFactory.getLogger ( Rdf2NeoCli.class );

	
	
	public static void main ( String... args )
	{
		try
		{
			exitCode = 0;
			CommandLineParser clparser = new DefaultParser ();
			CommandLine cli = null;
			try {
				cli = clparser.parse ( getOptions(), args );
			}
			catch ( ParseException ex ) {
				// Just keep cli null and let the trigger below to pop up.
			}
			
			if ( cli != null ) args = cli.getArgs ();
			if ( cli == null || cli.hasOption ( "help" ) || args.length == 0 ) {
				printUsage ();
				return;
			}
						
			String path = args [ 0 ];
			String cfgPath = cli.getOptionValue ( "config" );
			
			try ( MultiConfigCyLoader loader = MultiConfigCyLoader.getSpringInstance ( cfgPath ) ) {
				loader.load ( path );
			}
			
			log.info ( "The end" );
		}
		catch ( Throwable ex ) {
			log.error ( "Execution failed with the error: " + ex.getMessage (), ex  );
			exitCode = 1; // TODO: proper exit codes
		}
		finally {
			if ( !"true".equals ( System.getProperty ( NO_EXIT_PROP ) ) )
				System.exit ( exitCode );
		}
	}
	
	private static Options getOptions ()
	{
		Options opts = new Options ();

		opts.addOption ( Option.builder ( "h" )
			.desc ( "Prints out this message" )
			.longOpt ( "help" )
			.build ()
		);
		
		opts.addOption ( Option.builder ( "c" ) 
			.desc ( "Configuration file (see examples/sample_cfg.xml). "
					+   "WARNING! use 'file:///...' to specify absolute paths (Spring requirement)." )
			.longOpt ( "config" )
			.argName ( "bean configuration file.xml" )
			.numberOfArgs ( 1 )
			.required ()
			.build ()
		);
		
		return opts;		
	}
	
	private static void printUsage ()
	{
		out.println ();

		out.println ( "\n\n *** Rdf2Neo, the RDF-to-Neo4j converter ***" );
		out.println ( "\nConverts data from a Jena TDB database into Neo4J data" );
		
		out.println ( "\nSyntax:" );
		out.println ( "\n\trdf2neo.sh [options] <TDB path>" );		
		
		out.println ( "\nOptions:" );
		HelpFormatter helpFormatter = new HelpFormatter ();
		PrintWriter pw = new PrintWriter ( out, true );
		helpFormatter.printOptions ( pw, 100, getOptions (), 2, 4 );
				
		out.println ( "\n\n" );
		
		exitCode = 1;
	}

	/**
	 * This can be used when {@link #NO_EXIT_PROP} is "true" and you're invoking the main() method from 
	 * a JUnit test. It tells you the OS exit code that the JVM would return upon exit.
	 */
	public static int getExitCode () {
		return exitCode;
	}
}
