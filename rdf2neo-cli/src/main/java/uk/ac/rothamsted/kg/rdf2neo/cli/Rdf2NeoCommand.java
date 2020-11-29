package uk.ac.rothamsted.kg.rdf2neo.cli;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import uk.ac.rothamsted.kg.rdf2pg.cli.ConfigFileCliCommand;
import uk.ac.rothamsted.rdf.pg.load.MultiConfigPGLoader;
import uk.ac.rothamsted.rdf.pg.load.neo4j.MultiConfigNeo4jLoader;


/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Nov 2020</dd></dl>
 *
 */
@Component
@Command (
		name = "tdb2neo", 
		description = "\n\n  *** The RDF-to-Neo4j Converter ***\n" +
		  "\nConverts RDF data from a Jena TDB database into Neo4J data.\n"
	)	
public class Rdf2NeoCommand extends ConfigFileCliCommand
{
	@Parameters ( paramLabel = "<TDB path>", description = "The path to the Jena TDB triple store to load from", arity = "1" )
	private String tdbPath = null;

	@Override
	public Integer call () throws Exception
	{
		try ( var loader = MultiConfigPGLoader.getSpringInstance ( this.xmlConfigPath, MultiConfigNeo4jLoader.class ) )
		{
			loader.load ( tdbPath );
		}
		log.info ( "The end" );
		return 0;
	}
}
