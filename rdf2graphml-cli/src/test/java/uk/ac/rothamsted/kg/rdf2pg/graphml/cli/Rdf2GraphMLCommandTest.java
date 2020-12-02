package uk.ac.rothamsted.kg.rdf2pg.graphml.cli;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import uk.ac.rothamsted.kg.rdf2pg.cli.Rdf2PGCli;
import uk.ac.rothamsted.kg.rdf2pg.test.DataTestUtils;

/**
 * The test for the ML exporter.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>30 Nov 2020</dd></dl>
 *
 */
public class Rdf2GraphMLCommandTest
{

	@BeforeClass
	public static void initTDB ()
	{
		DataTestUtils.initDBpediaDataSet ();
	}
		
	
	@Test
	public void testCliInvocation ()
	{
		var outPath = "target/test-cli.graphml";
		
		Rdf2PGCli.main ( 
			"--config", "src/main/assembly/resources/examples/dbpedia/config.xml", 
			DataTestUtils.TDB_PATH,
			outPath
		);
		// TODO: test!
		assertEquals ( "Bad exit code!", 0, Rdf2PGCli.getExitCode () );
	}
}
