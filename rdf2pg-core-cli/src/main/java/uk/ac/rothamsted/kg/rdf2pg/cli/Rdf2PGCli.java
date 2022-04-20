package uk.ac.rothamsted.kg.rdf2pg.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.github.jsonldjava.shaded.com.google.common.base.Supplier;

import picocli.CommandLine;

/**
 * A skeleton for a typical Command Line entry point.
 * 
 * It works this way:
 * 
 * <ul>
 *   <li>This class instantiates a Spring context and populates it with components it finds on
 *       the same package it's defined or sub-packages.</li>
 *       
 * 	 <li>Later, it expects to find exactly one instance of {@link CliCommand}, which is used with
 *       the picocli API, using {@link CommandLine} (see their documentation for details).
 *       That command is the one that is invoked, through wrappers like {@link #wrapMain(Supplier)}, which deal with
 *       exceptions and quitting the JVM with a proper exit code.</li>
 * </ul>
 * 
 * Note that the Spring context loaded from configuration files is different than the one loaded for the process
 * described above. The former usually doesn't have classes in the hereby *.cli package (the scan package directive
 * in the XML excludes it).
 * 
 */
@Configuration
@ComponentScan ( basePackages = "uk.ac.rothamsted.kg.rdf2pg.**.cli" )
public class Rdf2PGCli
{
	/**
	 * If you set this to true, main() will not invoke {@link System#exit(int)}. This is useful in unit tests.
	 */
	public static final String NO_EXIT_PROP = "rdf2pg.no_jvm_exit"; 
	
	@Autowired
	protected Rdf2PgCommand<?> rdf2PgCommand;
		
	protected static int exitCode = 0;
	
	/**
	 * Likely, you don't want to change this method.
	 * 
	 * It {@link #getInstance() gets an instance} of 
	 * {@link Rdf2PGCli myself} that is managed by Spring. Then, it gets the package-specific 
	 * {@link #command command} that is set via Spring and runs it (via {@link CommandLine picocli}).
	 * 
	 * Finally, if {@link #NO_EXIT_PROP} isn't "true", it invokes {@link System#exit(int)} with the
	 * code returned by the command. If the {@link #NO_EXIT_PROP} is set, we don't shutdown and the 
	 * returned exit code {@link #getExitCode() is available} for testing.
	 * 
	 */
	public static void main ( String... args )
	{
		try {
			var cli = getInstance ();
			var cmd = new CommandLine ( cli.rdf2PgCommand );
			exitCode = cmd.execute ( args );
		}
		catch ( Throwable ex ) 
		{
			ex.printStackTrace ( System.err );
			exitCode = 1;
		}
		finally 
		{
			if ( !"true".equals ( System.getProperty ( NO_EXIT_PROP ) ) )
				System.exit ( exitCode );
		}			
	}
	
	/**
	 * Instantiates an {@link AnnotationConfigApplicationContext} that scans the current package and its sub-packages.
	 * @see {@link #main(String...)}.
	 */
	protected static Rdf2PGCli getInstance ()
	{
		try ( var ctx = new AnnotationConfigApplicationContext ( Rdf2PGCli.class ) )
		{
			Rdf2PGCli cli = ctx.getBean ( Rdf2PGCli.class );
			return cli;
		}
	}

	/**
	 * The last exit code, returned by {@link #main(String...)} before exiting. It's used for testing purposes and it's 
	 * usually accessible only if NO_EXIT_PROP is set.
	 */
	public static int getExitCode ()
	{
		return exitCode;
	}	
}
