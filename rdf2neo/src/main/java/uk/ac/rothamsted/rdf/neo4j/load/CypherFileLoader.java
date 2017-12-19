package uk.ac.rothamsted.rdf.neo4j.load;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;

import org.apache.jena.riot.Lang;

import info.marcobrandizi.rdfutils.jena.elt.RDFImporter;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 *
 */
public class CypherFileLoader extends CypherLoader<InputStream>
{
	public CypherFileLoader ()
	{
		super ();
		this.setRdfProcessor ( new RDFImporter () );
	}

	public void load ( InputStream rdfInput, String base, Lang hintLang )
	{
		this.process ( rdfInput, new Object[] { base, hintLang } );
	}

	public void load ( File rdfFile, String base, Lang hintLang )
	{
		try ( InputStream in = new BufferedInputStream ( new FileInputStream ( rdfFile ) ); ) 
		{
			this.load ( new FileInputStream ( rdfFile ), base, hintLang );
		}
		catch ( IOException ex )
		{
			throw new UncheckedIOException ( String.format ( 
				"Error while reading file '%s': %s", 
				Optional.ofNullable ( rdfFile ).map ( File::getAbsolutePath ).orElse ( "<null>" ),
				ex.getMessage () ), 
				ex 
			);
		}
	}
	
	public void load ( File rdfFile ) {
		this.load ( rdfFile, null, null );
	}

	
	public void load ( String rdfFilePath, String base, Lang hintLang ) {
		this.load ( new File ( rdfFilePath ), base, hintLang );
	}
	
	public void process ( String rdfFilePath ) {
		this.load ( rdfFilePath, null, null );
	}
	
}
