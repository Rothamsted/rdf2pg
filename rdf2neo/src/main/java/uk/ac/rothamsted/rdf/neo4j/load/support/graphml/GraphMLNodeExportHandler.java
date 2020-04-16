package uk.ac.rothamsted.rdf.neo4j.load.support.graphml;

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

import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.neo4j.load.support.CyNode;
import uk.ac.rothamsted.rdf.neo4j.load.support.GraphMLConfiguration;
import uk.ac.rothamsted.rdf.neo4j.load.support.GraphMLUtils;
import uk.ac.rothamsted.rdf.neo4j.load.support.RdfDataManager;

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
public class GraphMLNodeExportHandler extends GraphMLLoadingHandler<Resource>
{
	private String labelsSparql, nodePropsSparql;
	
	// First version: in order to create the key values, we have to store the property names gathered 
	// for each of the nodes 
	
	private static HashSet<String> gatheredNodeProperties = new HashSet<String>();
	{
		addGatheredProperty("iri");
	}
	
	// for threading safety purposes
	private static synchronized void addGatheredProperty(String property) {
		gatheredNodeProperties.add(property); 
	}
	
	
	public GraphMLNodeExportHandler ()
	{
		super ();
	}
	
	// semaphore to write in the appropriate file
	private static Object lock = new Object(); 
	
	
	@Override
	public void accept ( Set<Resource> nodeResources )
	{
		this.renameThread ( "graphMLNodeLoad:" );
		log.trace ( "Begin of {} nodes", nodeResources.size () );
		
		// Let's collect node attributes on a per-label basis
		// This is necessary to build a CREATE Cypher command that takes multiple nodes as parameter, since the 
		// node labels cannot be parameterised
		//
		Map<SortedSet<String>, List<Map<String, Object>>> graphMLNodeData = new HashMap<> ();
				
		RdfDataManager rdfMgr = this.getRdfDataManager ();

		// So, let's prepare the nodes
		for ( Resource nodeRes: nodeResources )
		{
			CyNode cyNode = rdfMgr.getCyNode ( nodeRes, this.labelsSparql, this.nodePropsSparql );

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
		
		log.trace ( "Sending {} node(s) to file {}", nodeResources.size (), GraphMLConfiguration.getOutputFile()+GraphMLConfiguration.NODE_FILE_EXTENSION);
		
		// and this is where it happens
		graphMLNodeData.entrySet().parallelStream().parallel().forEach(
				entry -> {
					// all of this nodes share this labels
					SortedSet<String> labels = entry.getKey(); 
					String labelsStr = labels
							.stream ()
							.map ( label -> '`' + label + '`' )
							.collect ( Collectors.joining ( ":" ) );
					
					List<Map<String, Object>> nodePropertiesList = entry.getValue ();

					nodePropertiesList.parallelStream().parallel().forEach( 
						node ->  {
							URI nodeIRI = URI.create((String)node.get("iri")); 
							StringBuilder strB = new StringBuilder(); 
							strB.append(GraphMLUtils.NODE_TAG_START); 
							strB.append(GraphMLUtils.ID_ATTR).append("=\"").append(nodeIRI.hashCode()).append("\" >"); 
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
			
	
  /**
   * This is a query that must returns the variable ?label and contains the variable ?iri, which is bound to a node's 
   * IRI, to fetch its labels. It must return distinct results (we obviously don't care if you don't use the DISTINCT
   * SPARQL clause).
   */
	public String getLabelsSparql ()
	{
		return labelsSparql;
	}

	@Autowired ( required = false ) @Qualifier ( "labelsSparql" )
	public void setLabelsSparql ( String labelsSparql )
	{
		this.labelsSparql = labelsSparql;
	}

  /**
   * This is a query that must returns the variables ?name and ?value and must contain the variable ?iri, which is bound 
   * to a node's IRI, to fetch its property. Similarly {@link #getLabelsSparql()}, it must return distinct results.
   */	
	public String getNodePropsSparql ()
	{
		return nodePropsSparql;
	}

	@Autowired ( required = false ) @Qualifier ( "nodePropsSparql" )
	public void setNodePropsSparql ( String nodePropsSparql )
	{
		this.nodePropsSparql = nodePropsSparql;
	}
	
}
