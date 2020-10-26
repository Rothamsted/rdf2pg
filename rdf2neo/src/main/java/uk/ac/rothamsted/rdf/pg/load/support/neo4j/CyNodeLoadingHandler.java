package uk.ac.rothamsted.rdf.pg.load.support.neo4j;

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

import uk.ac.rothamsted.rdf.pg.load.support.PGNodeHandler;
import uk.ac.rothamsted.rdf.pg.load.support.PGNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.entities.PGNode;
import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManager;

/**
 * <h1>The Cypher Node Loading handler.</h1>
 * 
 * This generates Cypher instructions to create sets of {@link PGNode property graph nodes} into the target
 * Neo4j DB.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class CyNodeLoadingHandler extends PGNodeHandler
{
	private Neo4jDataManager neo4jDataManager;
	
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
			PGNode cyNode = rdfMgr.getPGNode ( nodeRes, this.getLabelsSparql (), this.getNodePropsSparql () );
			SortedSet<String> labels = new TreeSet<> ( cyNode.getLabels () );
			labels.add ( defaultLabel );
			
			@SuppressWarnings ( "unchecked" )
			List<Map<String, Object>> cyNodes = cyData.computeIfAbsent ( labels, l -> new LinkedList<> () );

			cyNodes.add ( neoMgr.flatPGProperties ( cyNode ) );
		} 
		
		// OK, now we are ready to call Neo!
		//
		log.trace ( "Sending {} node(s) to Cypher", nodeResources.size () );

		// The labels are a constant wrt the undelining graph database, but they are varied by us for each label set
		String cypherCreateNodes = "UNWIND {nodes} AS node\n" + 
			"CREATE (n:%s)\n" +
			"SET n = node";
		
		// and this is where it happens
		//
		long nodesCtr = 0;
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
	 * This is used to manage operations with the Neo4j target. We don't care about closing this, the invoker
	 * has to do it. 
	 */	
	public Neo4jDataManager getNeo4jDataManager ()
	{
		return neo4jDataManager;
	}

	@Autowired
	public void setNeo4jDataManager ( Neo4jDataManager neo4jDataManager )
	{
		this.neo4jDataManager = neo4jDataManager;
	}
	
}
