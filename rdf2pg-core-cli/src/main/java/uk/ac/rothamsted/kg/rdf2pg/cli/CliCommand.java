package uk.ac.rothamsted.kg.rdf2pg.cli;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

/**
 * A line command template to run rdfpg CLI commands.
 * 
 * While at the moment there is only {@link Rdf2PgCommand one direct subclass} of this, we keep this 
 * abstraction for possible future commands which are different than the rdf2pg pattern and still 
 * have something in common with it. 
 *   
 * @author brandizi
 * <dl><dt>Date:</dt><dd>27 Nov 2020</dd></dl>
 *
 */
@Component
@Command ( 
	name = "abstractCmd", description = "\n\nCommand Line Skeleton.\n", 
	exitCodeOnVersionHelp = ExitCode.USAGE, // else, it's 0 and you can't know about this event
	exitCodeOnUsageHelp = ExitCode.USAGE, // ditto
	mixinStandardHelpOptions = true,
	usageHelpAutoWidth = true,
	usageHelpWidth = 120
)
public abstract class CliCommand implements Callable<Integer>
{
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );	
}
