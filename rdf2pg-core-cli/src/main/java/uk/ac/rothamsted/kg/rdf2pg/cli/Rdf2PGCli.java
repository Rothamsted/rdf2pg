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
 */
@Configuration
@ComponentScan ( basePackages = "uk.ac.rothamsted.kg.rdf2pg.**.cli" )
public abstract class Rdf2PGCli
{
	/**
	 * If you set this to true, main() will not invoke {@link System#exit(int)}. This is useful in unit tests.
	 */
	public static final String NO_EXIT_PROP = "rdf2pg.no_jvm_exit"; 
	
	@Autowired
	protected CliCommand command;
		
	protected static int exitCode = 0;
	
	/**
	 * Likely, you don't want to change much of method. It does this:
	 * <ul>
	 *   <li>Gets an instance of {@link Rdf2PGCli} bean from Spring, via {@link #getInstance()}. So, override that if you 
	 *       want a different instantiation.</li>
	 *   <li>Runs {@link #command} Using {@link #run(String...)}, that is, runs whatever CLI implementation Spring found
	 *       in the current package or below.</li>
	 *   <li>Runs everything within {@link #wrapMain(Supplier)}, so the JVM termination and exit code handling happen there.</li>
	 * </ul>
	 */
	public static void main ( String... args )
	{
		wrapMain ( () -> getInstance().run ( args ) );
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
	 * Runs {@link #command} using {@link CommandLine}.
	 * @see {@link #main(String...)}.
	 */
	protected int run ( String... args )
	{
		var cmd = new CommandLine ( this.command );
		return cmd.execute ( args );
	}
	
	/**
	 * This is a facility you should use in case you want your own version of {@link #main(String...)}.
	 * It runs the action and then invokes {@link System#exit(int)}, if {@link #NO_EXIT_PROP} isn't set.
	 * 
	 * action should be what you normally put under {@link #main(String...)}
	 */
	protected static void wrapMain ( Supplier<Integer> action ) 
	{
		try {
			exitCode = action.get ();
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
	 * The last exit code, returned by {@link #main(String...)} before exiting. It's used for testing purposes and it's 
	 * usually accessible only if NO_EXIT_PROP is set.
	 */
	public static int getExitCode ()
	{
		return exitCode;
	}	
}
