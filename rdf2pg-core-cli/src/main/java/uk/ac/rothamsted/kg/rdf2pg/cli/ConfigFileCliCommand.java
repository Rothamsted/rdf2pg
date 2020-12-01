package uk.ac.rothamsted.kg.rdf2pg.cli;

import picocli.CommandLine.Option;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;

/**
 * Use this if your flavour of rdf2pg needs a Spring Bean config file (which typically needs).
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>27 Nov 2020</dd></dl>
 *
 */
public abstract class ConfigFileCliCommand extends CliCommand
{
	@Option ( 
		names = { "-c", "--config" }, 
		description = "Configuration file (see examples/). "
								+ "WARNING! use 'file:///...' to specify absolute paths (Spring requirement)."
	)
	protected String xmlConfigPath = "";

	/**
	 * Helper that calls {@link MultiConfigPGMaker#getSpringInstance(String, Class)} using {@link #xmlConfigPath}
	 * as file parameter.
	 * 
	 */
	protected <MM extends MultiConfigPGMaker<?, ?>> MM getMakerFromSpringConfig ( Class<? extends MM> makerClass )
	{
		return MultiConfigPGMaker.getSpringInstance ( xmlConfigPath, makerClass );
	}
}
