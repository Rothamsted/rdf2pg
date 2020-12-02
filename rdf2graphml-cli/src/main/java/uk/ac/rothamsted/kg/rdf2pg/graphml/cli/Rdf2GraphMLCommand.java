package uk.ac.rothamsted.kg.rdf2pg.graphml.cli;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import uk.ac.rothamsted.kg.rdf2pg.cli.ConfigFileCliCommand;
import uk.ac.rothamsted.kg.rdf2pg.cli.Rdf2PgCommand;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.MultiConfigGraphMLExporter;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;


/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Nov 2020</dd></dl>
 *
 */
@Component
@Command (
		name = "tdb2graphml", 
		description = "\n\n  *** The RDF/graphML Converter ***\n" +
		  "\nConverts RDF data from a Jena TDB database into graphML format.\n"
	)	
public class Rdf2GraphMLCommand extends Rdf2PgCommand
{
	@Parameters ( paramLabel = "<TDB path>", description = "The path to the Jena TDB triple store to load from", arity = "1" )
	private String tdbPath = null;

	@Parameters ( paramLabel = "<graphML out path>", description = "The output path", arity = "1" )
	private String graphmlPath = null;
	
	@Override
	public Integer call () throws Exception
	{
		try ( var exporter = MultiConfigPGMaker.getSpringInstance ( this.xmlConfigPath, MultiConfigGraphMLExporter.class ) )
		{
			exporter.export ( tdbPath, graphmlPath );
		}
		log.info ( "The end" );
		return 0;
	}
}
