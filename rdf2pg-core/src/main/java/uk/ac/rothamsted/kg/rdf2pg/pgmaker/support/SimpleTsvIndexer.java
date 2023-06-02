package uk.ac.rothamsted.kg.rdf2pg.pgmaker.support;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import uk.ac.ebi.utils.exceptions.ExceptionUtils;

/**
 * A simple TSV indexer, which just output the {@link #getIndexesSparql() index-related definitions}
 * onto a TSV file.
 * 
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Jun 2023</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public class SimpleTsvIndexer extends PGIndexer
{
	private String outputPath = null;
	
	@Override
	protected void index ( List<IndexDef> indexDefinitions )
	{
		if ( indexDefinitions == null || indexDefinitions.isEmpty () ) return;
		
		try ( Writer w = this.outputPath == null
			? new OutputStreamWriter ( System.out )
			: new FileWriter ( outputPath );
		
			ICSVWriter tsvw = new CSVWriterBuilder ( w )
			.withSeparator ( '\t' )
			.build (); 
		)
		{
			tsvw.writeNext ( new String[] { "Type", "Property", "IsRelation" } );
			for ( var idxDef: indexDefinitions )
				tsvw.writeNext ( new String[] { 
					idxDef.getType (), idxDef.getPropertyName (), Boolean.toString ( idxDef.isRelation () ) 
			});
		}
		catch ( IOException ex )
		{
			ExceptionUtils.throwEx ( UncheckedIOException.class, ex, 
				"Error while saving PG index definitions to %s: $cause",
				Optional.ofNullable ( outputPath ).map ( o -> '"' + o + '"' ).orElse ( "<std out>" )
			);
		}
	}

	/**
	 * If this is set, the output goes here, else the {@link System#out standard output} is used. 
	 */
	public String getOutputPath ()
	{
		return outputPath;
	}

	/**
	 * You can set this with Spring, but usually we provide CLI options
	 */
	@Autowired ( required = false) @Qualifier ( "tsvIndexOutput" )
	public void setOutputPath ( String outputPath )
	{
		this.outputPath = outputPath;
	}
	
}
