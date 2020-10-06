package uk.ac.rothamsted.rdf.pg.load.support.graphml;

import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.writeEdgeAttribHeaders;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.writeNodeAttribHeaders;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.writeXMLAttrib;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.ebi.utils.exceptions.UncheckedFileNotFoundException;
import uk.ac.rothamsted.rdf.pg.load.support.AbstractPGDataManager;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Oct 2020</dd></dl>
 *
 */
@Component
public class GraphMLDataManager extends AbstractPGDataManager
{
	public static final String NODE_FILE_EXTENSION = "-Nodes-tmp.graphml"; 
	public static final String EDGE_FILE_EXTENSION = "-Edges-tmp.graphml"; 
	
	private Set<String> gatheredNodeProperties = Collections.synchronizedSet ( new HashSet<>() );
	private Set<String> gatheredEdgeProperties = Collections.synchronizedSet ( new HashSet<>() );
	
	private String gmlOutputPath = null; 

	/**
	 * Used to synch file writing operations over file path, see {@link #appendOutput(String, String)}.
	 */
	private static Map<String, String> outLocks = new HashMap<> ();
	
	{
		gatherNodeProperty ( "iri" );
		gatherNodeProperty ( GraphMLUtils.LABEL_VERTEX_ATTR );
		
		// The iri is built using the md5
		// we get then the type as well at the same point
		gatherEdgeProperty ( GraphMLUtils.LABEL_EDGE_ATTR );
	}

	
	private void appendOutput ( String graphML, String postFix )
	{
		String outPath = this.getGmlOutputPath () + postFix;
		
		synchronized ( outLocks.get ( outPath ) )
		{
			try {
				Files.writeString ( Paths.get ( outPath ), graphML, StandardOpenOption.CREATE, StandardOpenOption.APPEND );
			}
			catch ( IOException ex )
			{
				ExceptionUtils.throwEx ( 
					UncheckedIOException.class,
					ex,
					"Error while writing to the GML file '%s': %s", outPath, ex.getMessage () 
				);
			}
		}
	}
	
	public void appendNodeOutput ( String graphML )
	{
		appendOutput ( graphML, NODE_FILE_EXTENSION );
	}
	
	public void appendEdgeOutput ( String graphML )
	{
		appendOutput ( graphML, EDGE_FILE_EXTENSION );
	}
	
	/**
	 * Handlers accumulates all property types during loading, since many graphML readers require to list them
	 * in the file header.
	 */
	public void gatherNodeProperty ( String property ) {
		gatheredNodeProperties.add ( property ); 
	}
	
	public Set<String> getGatheredNodeProperties() {
		return gatheredNodeProperties; 
	}

	
	/**
	 * Handlers accumulates all property types during loading, since many graphML readers require to list them
	 * in the file header.
	 */
	public void gatherEdgeProperty ( String property ) {
		gatheredEdgeProperties.add ( property ); 
	}
	
	public Set<String> getGatheredEdgeProperties () {
		return gatheredEdgeProperties;
	}

	public String getGmlOutputPath ()
	{
		return gmlOutputPath;
	}

	public void setGmlOutputPath ( String gmlOutputPath )
	{
		this.gmlOutputPath = gmlOutputPath;

		Stream.of ( getNodeTmpPath (), getEdgeTmpPath () )
		.map ( postFix -> gmlOutputPath + postFix )
		.forEach ( outPath -> outLocks.put ( outPath, outPath ) );
	}
	
	private String getNodeTmpPath ()
	{
		return this.gmlOutputPath + NODE_FILE_EXTENSION;
	}

	private String getEdgeTmpPath ()
	{
		return this.gmlOutputPath + EDGE_FILE_EXTENSION;
	}
	
	
	public void writeGML ()
	{
		try ( PrintStream out = new PrintStream (
			new BufferedOutputStream ( 
				new FileOutputStream ( this.gmlOutputPath ),
				2<<19
			)
		))
		{
			// Schema headers
			out.println ( GraphMLUtils.GRAPHML_TAG_HEADER );
			
			var sb = new StringBuilder ();
			writeNodeAttribHeaders ( this.getGatheredNodeProperties (), sb );
			out.println ( sb.toString () );

			sb = new StringBuilder ();
			writeEdgeAttribHeaders ( this.getGatheredEdgeProperties (), sb );
			
			out.append ( GraphMLUtils.GRAPH_TAG_START );
			writeXMLAttrib ( GraphMLUtils.DEFAULT_DIRECTED_ATTR, GraphMLUtils.DIRECTED_DEFAULT_DIRECTED_VALUE , sb );
			out.println ( "\" >" ); 
	
			
			Stream.of ( getNodeTmpPath (), getEdgeTmpPath () )
			.forEach ( tempPath -> 
			{
				try ( Reader in = 
					new BufferedReader ( new FileReader ( tempPath, StandardCharsets.UTF_8 ), 2<<19 )
				) 
				{
					// TODO: regain some space by removing the temp at this point
					IOUtils.copy ( in, out, StandardCharsets.UTF_8 );
				}
				catch ( IOException ex ) {
					throw new UncheckedIOException ( String.format ( 
						"I/O error while copying '%s' to '%s': %s", tempPath, gmlOutputPath ), 
						ex
					);
				}
			});
	
			out.println ( GraphMLUtils.GRAPH_TAG_END );
			out.println ( GraphMLUtils.GRAPHML_TAG_END );
		}
		catch ( FileNotFoundException ex )
		{
			ExceptionUtils.throwEx ( 
				UncheckedFileNotFoundException.class, ex, 
				"Error while writing to GML file '%s': %s", this.gmlOutputPath, ex.getMessage () 
			);
		}
	}	
}
