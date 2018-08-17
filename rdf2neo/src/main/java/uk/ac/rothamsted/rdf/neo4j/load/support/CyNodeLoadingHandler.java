package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.HashMap;
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

/**
 * <h1>The Cypher Node Loading handler.</h1>
 *
 * <p>This is used by {@link CyNodeLoadingProcessor} and corresponds to the tasks that are run to load 
 * sets of nodes in Cypher/Neo4j.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class CyNodeLoadingHandler extends CypherLoadingHandler<Resource>
{
	private String labelsSparql, nodePropsSparql;
	
	public CyNodeLoadingHandler ()
	{
		super ();
	}
	
	
	@Override
	public void accept ( Set<Resource> nodeResources )
	{
		this.renameThread ( "cyNodeLoad:" );
		log.trace ( "Begin of {} nodes", nodeResources.size () );
		
		// Let's collect node attributes on a per-label basis
		// This is necessary to build a CREATE Cypher command that takes multiple nodes as parameter, since the 
		// node labels cannot be parameterised
		//
		Map<SortedSet<String>, List<Map<String, Object>>> cyData = new HashMap<> ();
				
		Neo4jDataManager neoMgr = this.getNeo4jDataManager ();
		RdfDataManager rdfMgr = this.getRdfDataManager ();
		String defaultLabel = neoMgr.getDefaultLabel ();

		// So, let's prepare the nodes
		for ( Resource nodeRes: nodeResources )
		{
			CyNode cyNode = rdfMgr.getCyNode ( nodeRes, this.labelsSparql, this.nodePropsSparql );

			SortedSet<String> labels = new TreeSet<> ( cyNode.getLabels () );
			
			// We always need a default, to be able to fetch the nodes during relation creation stage
			labels.add ( defaultLabel );
			
			List<Map<String, Object>> cyNodes = cyData.get ( labels );
			if ( cyNodes == null ) cyData.put ( labels, cyNodes = new LinkedList<> () );

			cyNodes.add ( neoMgr.getCypherProperties ( cyNode ) );
		} 
		
		// OK, now we are ready to call Neo!
		//
		log.trace ( "Sending {} node(s) to Cypher", nodeResources.size () );

		// The labels are a constant wrt the undelining graph database, but they are varied by us for each label set
		String cypherCreateNodes = "UNWIND {nodes} AS node\n" + 
			"CREATE (n:%s)\n" +
			"SET n = node";
		
		long nodesCtr = 0;
		// and this is where it happens 
		for ( Entry<SortedSet<String>, List<Map<String, Object>>> cyDataE: cyData.entrySet () )
		{
			SortedSet<String> labels = cyDataE.getKey ();

			String labelsStr = labels
				.stream ()
				.map ( label -> '`' + label + '`' )
				.collect ( Collectors.joining ( ":" ) );
			
			String cyCreateStr = String.format ( cypherCreateNodes, labelsStr );
			List<Map<String, Object>> props = cyDataE.getValue ();
			
			// So, this structure with a list having a map per each node is the parameter to be sent to Cypher (for unwinding) 
			neoMgr.runCypher ( cyCreateStr, "nodes", props );
			
			// And now, index it
			for ( String label: labels ) neoMgr.runCypher ( String.format ( "CREATE INDEX ON :`%s`(iri)", label ) );
			
			nodesCtr += props.size ();
		}
		
		log.debug ( "{} actual node(s) sent to Cypher", nodesCtr );
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
