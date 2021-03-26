package uk.ac.rothamsted.kg.rdf2pg.neo4j.cli;

import org.springframework.stereotype.Component;

import picocli.CommandLine.Command;
import uk.ac.rothamsted.kg.rdf2pg.cli.Rdf2PgCommand;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.MultiConfigNeo4jLoader;


/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Nov 2020</dd></dl>
 *
 */
@Component
@Command (
	name = "rdf2neo", 
	description = "\n\n  *** The RDF-to-Neo4j Converter ***\n" +
	  "\nConverts RDF data from a Jena TDB database into Neo4J data.\n"
)	
public class Rdf2NeoCommand extends Rdf2PgCommand<MultiConfigNeo4jLoader>
{
	public Rdf2NeoCommand ()
	{
		super ( MultiConfigNeo4jLoader.class );
	}

	@Override
	public int makePropertyGraph ()
	{
		try ( var cyloader = this.getMakerFromSpringConfig () ) {
			cyloader.load ( tdbPath );
		}
		log.info ( "The end" );
		return 0;
	}
}
