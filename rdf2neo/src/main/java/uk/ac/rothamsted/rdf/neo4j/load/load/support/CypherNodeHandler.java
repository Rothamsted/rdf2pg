package uk.ac.rothamsted.rdf.neo4j.load.load.support;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
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
public class CypherNodeHandler implements Consumer<Set<String>>
{
	protected NeoDataManager dataMgr;
	protected Driver neo4jDriver;
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );

	
	public CypherNodeHandler ( NeoDataManager dataMgr, Driver neo4jDriver )
	{
		super ();
		this.dataMgr = dataMgr;
		this.neo4jDriver = neo4jDriver;
	}

	
	@Override
	public void accept ( Set<String> nodeIris )
	{
		// Let's collect node attributes on a per-label basis
		Map<String, List<Map<String, Object>>> cyData = new HashMap<> ();
				
		for ( String nodeIri: nodeIris )
		{
			Node node = dataMgr.getNode ( nodeIri );
			
			// TODO: check for empty labels 
			String labelsKey = node.getLabels ()
				.stream ()
				.sorted ()
				.map ( label -> '`' + label + '`' )
				.collect ( Collectors.joining ( ":" ) );
			
			List<Map<String, Object>> cyNodes = cyData.get ( labelsKey );
			if ( cyNodes == null ) cyData.put ( labelsKey, cyNodes = new LinkedList<> () );

			Map<String, Object> cyAttrs = new HashMap<> ();
			for ( Entry<String, Set<Object>> attre: node.getProperties ().entrySet () )
			{
				Set<Object> vals = attre.getValue ();
				if ( vals.isEmpty () ) continue; // shouldn't happen, but just in case
				
				Object cyAttrVal = vals.size () > 1 ? vals.toArray ( new Object [ 0 ] ) : vals.iterator ().next ();
				cyAttrs.put ( attre.getKey (), cyAttrVal );
			}
			
			cyAttrs.put ( "iri", nodeIri );
			cyNodes.add ( cyAttrs );
		}

		// OK, ready to call Neo!
		
		String cypherCreateNodes = "UNWIND {nodes} AS node\n" + 
			"CREATE (n:%s)\n" +
			"SET n = node";
		
		for ( Entry<String, List<Map<String, Object>>> cyDataE: cyData.entrySet () )
		{
			String labelsStr = cyDataE.getKey ();
			String cyCreateStr = String.format ( cypherCreateNodes, labelsStr );
			List<Map<String, Object>> attribs = cyDataE.getValue ();
			
			this.runCypher ( cyCreateStr, "nodes", attribs );
		}
	}
	
	protected void runCypher ( String cypher, Object... keyVals  )
	{
		log.info ( "Cypher: {} params: {}", cypher, ArrayUtils.toString ( keyVals ) );
		
		Session session = this.neo4jDriver.session ();
		session.run ( cypher, parameters ( keyVals ) );
		session.close ();
	}

}
