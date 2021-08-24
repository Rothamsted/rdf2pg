package uk.ac.rothamsted.kg.rdf2pg.graphml.cli;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import uk.ac.rothamsted.kg.rdf2pg.cli.Rdf2PgCommand;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.MultiConfigGraphMLExporter;


/**
 * The rdf2graphml CLI.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Nov 2020</dd></dl>
 *
 */
@Component
@Command (
	name = "rdf2graphml", 
	description = "\n\n  *** The RDF/graphML Converter ***\n" +
		"\nConverts RDF data from a Jena TDB database into graphML format.\n"
)	
public class Rdf2GraphMLCommand extends Rdf2PgCommand<MultiConfigGraphMLExporter>
{
	// TODO: stdout?!
	
	@Option (
		names = { "-o", "--output" },
		paramLabel = "<path>", description = "The graphML output path.",
		required = true
	)
	private String graphmlPath = null;
	
	public Rdf2GraphMLCommand ()
	{
		super ( MultiConfigGraphMLExporter.class );
	}

	@Override
	public int makePropertyGraph ()
	{
		try ( var exporter = this.getMakerFromSpringConfig () ) {
			exporter.export ( tdbPath, graphmlPath );
		}
		log.info ( "The end" );
		return 0;
	}
}
