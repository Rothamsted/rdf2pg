package uk.ac.rothamsted.rdf.pg.load.graphml;

import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.writeEdgeAttribHeaders;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.writeNodeAttribHeaders;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.writeXMLAttrib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import javax.annotation.Resource;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.exceptions.UncheckedFileNotFoundException;
import uk.ac.rothamsted.rdf.pg.load.ConfigItem;
import uk.ac.rothamsted.rdf.pg.load.MultiConfigPGLoader;
import uk.ac.rothamsted.rdf.pg.load.SimpleGraphMLExporter;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeExportHandler;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils;

/**
 * TODO: comment me!
 * TODO: for the moment, we only support file output path as output option, we'd better supporting
 * File or PrintStream objects too.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Jun 2020</dd></dl>
 *
 */
@Component
public class MultiConfigGraphMLLoader 
	extends MultiConfigPGLoader<ConfigItem<SimpleGraphMLExporter>, SimpleGraphMLExporter>
{
	public static final String NODE_FILE_EXTENSION = "-Nodes-tmp.graphml"; 
	public static final String EDGE_FILE_EXTENSION = "-Edges-tmp.graphml";
		
	@Resource ( type = SimpleGraphMLExporter.class ) @Override
	public void setPGLoaderFactory ( ObjectFactory<SimpleGraphMLExporter> loaderFactory )
	{
		super.setPGLoaderFactory ( loaderFactory );
	}

	@Override
	protected void loadBegin ( String tdbPath, Object... opts )
	{
		super.loadBegin ( tdbPath, opts );
		
		if ( opts == null || opts.length != 1 ) throw new IllegalArgumentException ( String.format (
			"%s needs the output file parameter", this.getClass ().getSimpleName ()
		));
	}

	@Override
	protected void loadIteration ( int mode, ConfigItem<SimpleGraphMLExporter> cfg, String tdbPath, Object... opts )
	{
		String outPath = (String) opts [ 0 ];

		try (  var graphMLExporter = this.getPGLoaderFactory ().getObject (); )
		{
			cfg.configureLoader ( graphMLExporter );
			graphMLExporter.load ( tdbPath, mode == 0, mode == 1, outPath );
		}

		writeGraphML ( outPath );
	}


	private void writeGraphML ( String outPath )
	{
		writeGraphML ( new File ( outPath ) );
	}

	private void writeGraphML ( File outFile )
	{
		try
		{
			writeGraphML ( outFile.getAbsolutePath (), new PrintStream ( new FileOutputStream ( outFile ) ) );
		}
		catch ( FileNotFoundException ex ) {
			throw new UncheckedFileNotFoundException ( 
				"Error while writing to '" + outFile.getAbsolutePath () + "': file not found" 
			);
		}
	}
	
	private void writeGraphML ( String tmpFilesBasePath, PrintStream gmlOut )
	{
		// Schema headers
		gmlOut.println ( GraphMLUtils.GRAPHML_TAG_HEADER );
		
		writeNodeAttribHeaders ( GraphMLNodeExportHandler.getGatheredNodeProperties(), gmlOut );
		writeEdgeAttribHeaders ( GraphMLNodeExportHandler.getGatheredNodeProperties(), gmlOut );
					
		gmlOut.append ( GraphMLUtils.GRAPH_TAG_START );
		writeXMLAttrib ( GraphMLUtils.DEFAULT_DIRECTED_ATTR, GraphMLUtils.DIRECTED_DEFAULT_DIRECTED_VALUE , gmlOut );
		gmlOut.println ( "\" >" ); 

		
		Stream.of ( getTempNodesPath ( tmpFilesBasePath ), getTempEdgesPath ( tmpFilesBasePath ) )
		.forEach ( tempPath -> 
		{
			try ( Reader in = 
				new BufferedReader ( new FileReader ( tempPath, StandardCharsets.UTF_8 ), 2<<19 )
			) 
			{
				// TODO: we don't need two temp files, one would be enough
				IOUtils.copy ( in, gmlOut, StandardCharsets.UTF_8 );
			}
			catch ( IOException ex ) {
				throw new UncheckedIOException ( String.format ( 
					"I/O error while copying '%s' to '%s': %s", tempPath, tmpFilesBasePath ), 
					ex
				);
			}
		});

		gmlOut.println ( GraphMLUtils.GRAPH_TAG_END );
		gmlOut.println ( GraphMLUtils.GRAPHML_TAG_END ); 

	}

	public static String getTempNodesPath ( String tmpFilesBasePath )
	{
		return tmpFilesBasePath + NODE_FILE_EXTENSION;
	}

	public static String getTempEdgesPath ( String tmpFilesBasePath )
	{
		return tmpFilesBasePath + EDGE_FILE_EXTENSION;
	}
}
