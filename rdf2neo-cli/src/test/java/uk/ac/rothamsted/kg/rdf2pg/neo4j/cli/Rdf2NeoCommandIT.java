package uk.ac.rothamsted.kg.rdf2pg.neo4j.cli;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import uk.ac.rothamsted.kg.rdf2pg.cli.Rdf2PGCli;
import uk.ac.rothamsted.kg.rdf2pg.load.support.rdf.DataTestUtils;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>30 Nov 2020</dd></dl>
 *
 */
public class Rdf2NeoCommandIT
{

	@BeforeClass
	public static void initTDB ()
	{
		DataTestUtils.initDBpediaDataSet ();
	}
		
	
	@Test
	public void testCliInvocation ()
	{
		Rdf2PGCli.main ( "--config", "src/main/assembly/resources/examples/dbpedia/config.xml", DataTestUtils.TDB_PATH );
		// TODO: test!
		assertEquals ( "Bad exit code!", 0, Rdf2PGCli.getExitCode () );
	}
}
