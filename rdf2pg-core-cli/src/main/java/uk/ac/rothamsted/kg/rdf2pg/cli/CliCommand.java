package uk.ac.rothamsted.kg.rdf2pg.cli;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;

/**
 * A line command template to run rdfpg CLI functions.
 * 
 * TODO: review this comment.
 * It is expected to have one concrete implementation of this class per each *-cli package available in rdf2pg 
 * (eg, rdf2neo-cli, rdf2graphml-cli). Each command will do something specific of the particular RDF2PG converter. 
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
