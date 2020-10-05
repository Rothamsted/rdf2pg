package uk.ac.rothamsted.rdf.pg.load.support.graphml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.rothamsted.rdf.pg.load.support.AbstractPGDataManager;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Oct 2020</dd></dl>
 *
 */
@Component
public class GMLDataManager extends AbstractPGDataManager
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

		Stream.of ( NODE_FILE_EXTENSION, EDGE_FILE_EXTENSION )
		.map ( postFix -> gmlOutputPath + postFix )
		.forEach ( outPath -> outLocks.put ( outPath, outPath ) );
	}
}
