package uk.ac.rothamsted.kg.rdf2pg.cli;

import org.apache.jena.tdb2.TDB2Factory;
import org.apache.jena.tdb2.loader.Loader;

import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Dec 2020</dd></dl>
 *
 */
public abstract class Rdf2PgCommand<MM extends MultiConfigPGMaker<?, ?>> extends CliCommand
{
	@Option ( 
		names = { "-c", "--config" }, 
		description = "Configuration file (see examples/). "
								+ "WARNING! use 'file:///...' to specify absolute paths (Spring requirement).",
    required = true
	)
	protected String xmlConfigPath = "";

	@Option ( 
		names = { "-t", "--tdb" }, 
		paramLabel = "<TDB path>", 
		description = "The path to the Jena TDB triple store to load from. The default is presumably empty and to be used with -r.",
		required = false,
		showDefaultValue = Visibility.ALWAYS
	)
	protected String tdbPath = "/tmp/rdf2pg-tdb";
	
	
	@Option ( 
		names = { "-r", "--rdf" },
		paramLabel = "<RDF path>", 
		description = "Files to be uses as input. They're first uploaded on the support TDB."
		  + " If none, the --tdb store is taken with its current contents.",
		required = false 
	)
	protected String [] rdfFiles;
	 
	/** TODO: comment me */
	protected final Class<MM> makerClass;
	
	protected Rdf2PgCommand ( Class<MM> makerClass )
	{
		this.makerClass = makerClass;
	}
	
	/**
	 * Helper that calls {@link MultiConfigPGMaker#getSpringInstance(String, Class)} using {@link #xmlConfigPath}
	 * as file parameter.
	 * 
	 */
	protected MM getMakerFromSpringConfig ()
	{
		return MultiConfigPGMaker.getSpringInstance ( xmlConfigPath, makerClass );
	}

	
	/**
	 * This is a skeleton of how we want an Rdf2Pg command to work, that's why it's final. 
	 * Here, we check if we have {@link #rdfFiles} to load into the {@link #tdbPath support TDB}, possibly 
	 * {@link #load2Tdb() we do it} and then we invoke the {@link #makePropertyGraph() actual PG conversion from TDB}.
	 * 
	 * So, implement {@link #makePropertyGraph()} for your specific converter.
	 * 
	 */
	@Override
	public final Integer call () throws Exception
	{
		if ( this.rdfFiles != null && rdfFiles.length > 0 ) this.load2Tdb ();
		return this.makePropertyGraph ();
	}


	public abstract int makePropertyGraph () throws Exception;


	protected void load2Tdb ()
	{
		log.info ( "Starting TDB Loading" );
		var dataset = TDB2Factory.connectDataset ( this.tdbPath );
		Loader.load ( dataset.asDatasetGraph (), true, this.rdfFiles );
		log.info ( "TDB Loading ended" );
	}
}
