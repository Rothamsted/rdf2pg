package uk.ac.rothamsted.kg.rdf2pg.graphml.cli;

import static org.junit.Assert.assertEquals;

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
	/**
	 * From an existing TDB
	 */
	@Test
	public void testTdb2GraphML ()
	{
		var outPath = "target/tdb2graphml-test.graphml";
		
		DataTestUtils.initDBpediaDataSet ();
		
		Rdf2PGCli.main ( 
			"--config", "src/main/assembly/resources/examples/dbpedia/config.xml",
			"--tdb", DataTestUtils.TDB_PATH,
			outPath
		);
		// TODO: test!
		assertEquals ( "Bad exit code!", 0, Rdf2PGCli.getExitCode () );
	}

	/**
	 * First populates a TDB, then invokes the conversion.
	 */
	@Test
	public void testRdf2Neo ()
	{
		var outPath = "target/rdf2graphml-test.graphml";
		var dbpath = "target/test-classes/examples/dbpedia/";
		
		Rdf2PGCli.main ( 
			"--config", "src/main/assembly/resources/examples/dbpedia/config.xml", 
			"--tdb", "target/rdf2neo-test-tdb",
			"--rdf", dbpath + "dbpedia_places.ttl",
			"--rdf", dbpath + "dbpedia_people.ttl",
			outPath
		);
		// TODO: test!
		assertEquals ( "Bad exit code!", 0, Rdf2PGCli.getExitCode () );
	}	
	
}
