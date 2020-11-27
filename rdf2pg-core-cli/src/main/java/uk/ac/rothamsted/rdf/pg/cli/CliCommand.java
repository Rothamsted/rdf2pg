package uk.ac.rothamsted.rdf.pg.cli;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;

/**
 * A line command instantiated by {@link Rdf2PGCli} and passed to picocli.
 * 
 * It is expected to have one concrete implementation of this class per each *-cli package available in rdf2pg 
 * (eg, rdf2neo-cli, rdf2graphml-cli). Each command will do something specific of the particular RDF2PG converter. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>27 Nov 2020</dd></dl>
 *
 */
@Component
@Command ( 
	name = "abstractCmd", description = "Command Line Skeleton.", 
	exitCodeOnVersionHelp = ExitCode.USAGE, // else, it's 0 and you can't know about this event
	exitCodeOnUsageHelp = ExitCode.USAGE, // ditto
	mixinStandardHelpOptions = true
)
public abstract class CliCommand implements Callable<Integer>
{
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
}
