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
import org.neo4j.driver.v1.Driver;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 *
 */
public class CyNodeLoadingHandler extends CypherLoadingHandler<Long>
{
	private String nodeIrisSparql, labelsSparql, nodePropsSparql;
	
	public CyNodeLoadingHandler ()
	{
		super ();
	}
	
	
	@Override
	public void accept ( Long sparqlQueryOffset )
	{
		Thread.currentThread ().setName ( "cyNodeLoad:" + sparqlQueryOffset );
		if ( this.dataFinished () && sparqlQueryOffset >= this.getOverflowQueryOffset () ) {
			log.trace ( "{} over {}, skipping", sparqlQueryOffset, this.getOverflowQueryOffset () );
			return;
		}
		
		log.trace ( "Begin at offset {}", sparqlQueryOffset );
		
		// Let's collect node attributes on a per-label basis
		Map<SortedSet<String>, List<Map<String, Object>>> cyData = new HashMap<> ();
				
		String defaultLabel = this.getDefaultLabel ();
		long limit = this.getSparqlQuerySize ();
		NeoDataManager dataMgr = this.getDataMgr ();

		long newOffset = dataMgr.processNodeIris 
		( 
			this.nodeIrisSparql, sparqlQueryOffset, limit, 
			nodeRes -> {
				Node node = dataMgr.getNode ( nodeRes, this.labelsSparql, this.nodePropsSparql );

				SortedSet<String> labels = new TreeSet<> ( node.getLabels () );
				
				// We always need a default, to be able to fetch the nodes during relation creation stage
				labels.add ( defaultLabel );
				
				List<Map<String, Object>> cyNodes = cyData.get ( labels );
				if ( cyNodes == null ) cyData.put ( labels, cyNodes = new LinkedList<> () );

				cyNodes.add ( this.getCypherProperties ( node ) );
		}); 
		
		
		if ( newOffset == -1 ) {
			// notify we ran out of data
			this.notifyDataFinished ( sparqlQueryOffset );
			log.trace ( "Notifyng end of data at offset {}", sparqlQueryOffset );
			return;
		}
		
		// OK, ready to call Neo!
		//
		log.trace ( "Offset {}, sending node(s) to Cypher", sparqlQueryOffset );

		String cypherCreateNodes = "UNWIND {nodes} AS node\n" + 
			"CREATE (n:%s)\n" +
			"SET n = node";
		
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
			
			this.runCypher ( cyCreateStr, "nodes", props );
			
			// And now, index it
			for ( String label: labels ) this.runCypher ( String.format ( "CREATE INDEX ON :`%s`(iri)", label ) );
			
			nodesCtr += props.size ();
		}
		
		log.trace ( "{} node(s) sent to Cypher", nodesCtr );
		log.info ( "Done SPARQL offset {}", sparqlQueryOffset );
	}
	
	public String getNodeIrisSparql ()
	{
		return nodeIrisSparql;
	}

	public void setNodeIrisSparql ( String nodeIrisSparql )
	{
		this.nodeIrisSparql = nodeIrisSparql;
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
}
