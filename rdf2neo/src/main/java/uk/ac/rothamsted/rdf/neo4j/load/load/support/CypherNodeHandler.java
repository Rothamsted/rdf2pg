package uk.ac.rothamsted.rdf.neo4j.load.load.support;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.rdf.model.Resource;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 *
 */
public class CypherNodeHandler implements Consumer<Set<Resource>>
{
	private String labelsSparql, nodePropsSparql;
	private String defaultLabel = "Resource";
	
	private NeoDataManager dataMgr;
	private Driver neo4jDriver;
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	public CypherNodeHandler ()
	{
	}
	
	public CypherNodeHandler (
		NeoDataManager dataMgr, Driver neo4jDriver, String labelsSparql, String nodePropsSparql 
	)
	{
		super ();
		this.dataMgr = dataMgr;
		this.neo4jDriver = neo4jDriver;
		this.labelsSparql = labelsSparql;
		this.nodePropsSparql = nodePropsSparql;
	}

	
	@Override
	public void accept ( Set<Resource> nodeRess )
	{
		// Let's collect node attributes on a per-label basis
		Map<SortedSet<String>, List<Map<String, Object>>> cyData = new HashMap<> ();
				
		for ( Resource nodeRes: nodeRess )
		{
			
			Node node = dataMgr.getNode ( nodeRes, this.labelsSparql, this.nodePropsSparql );

			SortedSet<String> labels = new TreeSet<> ( node.getLabels () );
			
			// We always need a default, to be able to fetch the nodes during relation creation stage
			labels.add ( this.defaultLabel );
			
			List<Map<String, Object>> cyNodes = cyData.get ( labels );
			if ( cyNodes == null ) cyData.put ( labels, cyNodes = new LinkedList<> () );

			Map<String, Object> cyAttrs = new HashMap<> ();
			for ( Entry<String, Set<Object>> attre: node.getProperties ().entrySet () )
			{
				Set<Object> vals = attre.getValue ();
				if ( vals.isEmpty () ) continue; // shouldn't happen, but just in case
				
				Object cyAttrVal = vals.size () > 1 ? vals.toArray ( new Object [ 0 ] ) : vals.iterator ().next ();
				cyAttrs.put ( attre.getKey (), cyAttrVal );
			}
			
			cyAttrs.put ( "iri", nodeRes.getURI () );
			cyNodes.add ( cyAttrs );
		}

		// OK, ready to call Neo!
		
		String cypherCreateNodes = "UNWIND {nodes} AS node\n" + 
			"CREATE (n:%s)\n" +
			"SET n = node";
		
		for ( Entry<SortedSet<String>, List<Map<String, Object>>> cyDataE: cyData.entrySet () )
		{
			SortedSet<String> labels = cyDataE.getKey ();

			String labelsStr = labels
				.stream ()
				.map ( label -> '`' + label + '`' )
				.collect ( Collectors.joining ( ":" ) );
			
			String cyCreateStr = String.format ( cypherCreateNodes, labelsStr );
			List<Map<String, Object>> attribs = cyDataE.getValue ();
			
			this.runCypher ( cyCreateStr, "nodes", attribs );
			
			// And now, index it
			for ( String label: labels ) this.runCypher ( String.format ( "CREATE INDEX ON :`%s`(iri)", label ) );
		}		
	}
	
	protected void runCypher ( String cypher, Object... keyVals  )
	{
		if ( log.isTraceEnabled () )
			log.trace ( "Cypher: {} params: {}", cypher, ArrayUtils.toString ( keyVals ) );
		
		try ( Session session = this.neo4jDriver.session () ) {
			session.run ( cypher, parameters ( keyVals ) );
		}
	}

	
	public String getLabelsSparql ()
	{
		return labelsSparql;
	}

	public void setLabelsSparql ( String labelsSparql )
	{
		this.labelsSparql = labelsSparql;
	}

	
	public String getNodePropsSparql ()
	{
		return nodePropsSparql;
	}

	public void setNodePropsSparql ( String nodePropsSparql )
	{
		this.nodePropsSparql = nodePropsSparql;
	}

	
	public String getDefaultLabel ()
	{
		return defaultLabel;
	}

	public void setDefaultLabel ( String defaultLabel )
	{
		this.defaultLabel = defaultLabel;
	}

	public NeoDataManager getDataMgr ()
	{
		return dataMgr;
	}

	public void setDataMgr ( NeoDataManager dataMgr )
	{
		this.dataMgr = dataMgr;
	}

	public Driver getNeo4jDriver ()
	{
		return neo4jDriver;
	}

	public void setNeo4jDriver ( Driver neo4jDriver )
	{
		this.neo4jDriver = neo4jDriver;
	}	
}
