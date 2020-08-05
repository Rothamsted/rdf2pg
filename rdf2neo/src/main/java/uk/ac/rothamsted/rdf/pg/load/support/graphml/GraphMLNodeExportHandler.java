package uk.ac.rothamsted.rdf.pg.load.support.graphml;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.jena.rdf.model.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.PGNodeHandler;
import uk.ac.rothamsted.rdf.pg.load.support.entities.PGNode;
import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManager;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * <h1>The GraphML Node Loading handler.</h1>
 *
 * <p>This is used by {@link GraphMLNodeLoadingProcessor} and corresponds to the tasks that are run to load 
 * sets of nodes in GraphML.</p>
 *
 * @author cbobed
 * <dl><dt>Date:</dt><dd>16 Apr 2020</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class GraphMLNodeExportHandler extends PGNodeHandler
{
	
	// First version: in order to create the key values, we have to store the property names gathered 
	// for each of the nodes
	// TODO: They cannot be static, see TODO.md
	
	private static HashSet<String> gatheredNodeProperties = new HashSet<String>();
	{
		addGatheredProperty("iri");
		addGatheredProperty(GraphMLUtils.LABEL_VERTEX_ATTR); 
	}
	
	// for threading safety purposes
	private static synchronized void addGatheredProperty(String property) {
		gatheredNodeProperties.add(property); 
	}
	
	public static synchronized HashSet<String> getGatheredNodeProperties() {
		return gatheredNodeProperties; 
	}
	
	
	// semaphore to write in the appropriate file
	private static Object lock = new Object(); 
	
	
	@Override
	public void accept ( Set<Resource> nodeResources )
	{
		// TODO: The node/relation preparation is common code, doesn't depend on
		// the target, FACTORISE THE COPY-PASTE!!!
		
		this.renameThread ( "graphMLNodeLoad:" );
		log.trace ( "Begin of {} nodes", nodeResources.size () );
		
		// Let's collect node attributes on a per-label basis
		// This is necessary to build a CREATE Cypher command that takes multiple nodes as parameter, since the 
		// node labels cannot be parameterised
		//
		Map<SortedSet<String>, List<Map<String, Object>>> graphMLNodeData = new HashMap<> ();
				
		RdfDataManager rdfMgr = this.getRdfDataManager ();

		log.info("GraphML Resources: {}", nodeResources.size()); 
		// So, let's prepare the nodes
		for ( Resource nodeRes: nodeResources )
		{
			PGNode cyNode = rdfMgr.getPGNode ( nodeRes, this.getLabelsSparql (), this.getNodePropsSparql () );

			SortedSet<String> labels = new TreeSet<> ( cyNode.getLabels () );
			
			// We always need a default, to be able to fetch the nodes during relation creation stage
			labels.add ( GraphMLConfiguration.getNodeDefaultLabel());
			
			List<Map<String, Object>> cyNodes = graphMLNodeData.get ( labels );
			if ( cyNodes == null ) graphMLNodeData.put ( labels, cyNodes = new LinkedList<> () );

			HashMap<String, Object> properties = new HashMap<> ();
			for ( Entry<String, Set<Object>> attre: cyNode.getProperties ().entrySet () )
			{
				Set<Object> vals = attre.getValue ();
				if ( vals.isEmpty () ) continue; // shouldn't happen, but just in case
				
				Object cyAttrVal = vals.size () > 1 ? vals.toArray ( new Object [ 0 ] ) : vals.iterator ().next ();
				properties.put ( attre.getKey (), cyAttrVal );
				// we add the property to the global keys
				addGatheredProperty(attre.getKey()); 
			}
			
			properties.put ( "iri", cyNode.getIri () );

			cyNodes.add ( properties );
		} 
		
		log.info ( "Sending node(s) to file {}", GraphMLConfiguration.getOutputFile()+GraphMLConfiguration.NODE_FILE_EXTENSION);
		
		log.info("GraphMLNodeSize: {}", graphMLNodeData.keySet().size()); 
		// and this is where it happens
		graphMLNodeData.entrySet().parallelStream().parallel().forEach(
				entry -> {
					log.info("entrySet: {}", entry.getValue().size()); 
					// all of this nodes share this labels
					SortedSet<String> labels = entry.getKey(); 
					String labelsStr = labels
							.stream ()
							.map ( label -> label.replace("\"", "\\\"") )
							.collect ( Collectors.joining ( ":" ) );
					
					List<Map<String, Object>> nodePropertiesList = entry.getValue ();

					nodePropertiesList.parallelStream().parallel().forEach( 
						node ->  {
//							URI nodeIRI = URI.create((String)node.get("iri")); 
							StringBuilder strB = new StringBuilder(); 
							strB.append(GraphMLUtils.NODE_TAG_START); 
							strB.append(GraphMLUtils.ID_ATTR).append("=\"").append((String)node.get("iri")).append("\" ");
							//we write the labels
							strB.append(GraphMLUtils.LABEL_VERTEX_ATTR).append("=\"").append(StringEscapeUtils.escapeXml11(labelsStr)).append("\" >");
							//we include them as property of the node in the labels field (to use them in other indexes)
							strB.append(GraphMLUtils.DATA_TAG_START); 
							strB.append(GraphMLUtils.KEY_ATTR).append("=\"").append(GraphMLUtils.LABEL_VERTEX_ATTR).append("\" >");
				    		strB.append(StringEscapeUtils.escapeXml11(labelsStr));
				    		strB.append(GraphMLUtils.DATA_TAG_END); 
				    		// we write the rest of properties
				    		strB.append(GraphMLUtils.dataValues(node)); 
							strB.append(GraphMLUtils.NODE_TAG_END).append("\n");
							String nodeString = strB.toString();

							synchronized (lock) {
								try {
									Files.writeString(Paths.get(GraphMLConfiguration.getOutputFile()+GraphMLConfiguration.NODE_FILE_EXTENSION), 
											nodeString, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
									
								}
								catch (IOException e) {
									log.error("Problems writing the node{}", nodeString); 
									e.printStackTrace();
								}
							}
						}		
					);
	   }	
				
		);
		
		log.debug(" Node export process finished");
	}
		
}
