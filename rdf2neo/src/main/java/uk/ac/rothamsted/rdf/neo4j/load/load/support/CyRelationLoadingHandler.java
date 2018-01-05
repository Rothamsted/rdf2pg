package uk.ac.rothamsted.rdf.neo4j.load.load.support;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.driver.v1.Driver;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 *
 */
public class CyRelationLoadingHandler extends CypherLoadingHandler<Relation>
{
	private String relationTypesSparql, relationPropsSparql;

	public CyRelationLoadingHandler ()
	{
		super ();
	}

	public CyRelationLoadingHandler ( 
		NeoDataManager dataMgr, Driver neo4jDriver, String relationTypesSparql, String relationPropsSparql
	)
	{
		super ( dataMgr, neo4jDriver );
		this.relationTypesSparql = relationTypesSparql;
		this.relationPropsSparql = relationPropsSparql;
	}
	
	
	@Override
	public void accept ( Set<Relation> relations )
	{
		Map<String, List<Map<String, Object>>> cyData = new HashMap<> ();

		for ( Relation relation: relations )
		{
			NeoDataManager dataMgr = this.getDataMgr ();
			dataMgr.setRelationProps ( relation, this.relationPropsSparql );
			
			String type = relation.getType ();
			List<Map<String, Object>> cyRels = cyData.get ( type );
			if ( cyRels == null ) cyData.put ( type, cyRels = new LinkedList<> () );

			Map<String, Object> cyparams = new HashMap<> (); 
			cyparams.put ( "fromIri", String.valueOf ( relation.getFromIri () ) );
			cyparams.put ( "toIri", String.valueOf ( relation.getToIri () ) );
			cyparams.put ( "properties", this.getCypherProperties ( relation ) );
			cyRels.add ( cyparams );
		}
		
		
		// OK, ready to call Neo!
		//
		String cypherCreateRel = 
			"UNWIND {relations} AS rel\n" +
			"MATCH ( from:`%1$s`{ iri: rel.fromIri } ), ( to:`%1$s`{ iri: rel.toIri } )\n" +
			"CREATE (from)-[r:`%2$s`]->(to)\n" +
			"SET r = rel.properties";
			
		String defaultLabel = this.getDefaultLabel ();
		
		for ( Entry<String, List<Map<String, Object>>> cyDataE: cyData.entrySet () )
		{
			String type = cyDataE.getKey ();
			
			String cyCreateStr = String.format ( cypherCreateRel, defaultLabel, type );
			List<Map<String, Object>> props = cyDataE.getValue ();
			
			this.runCypher ( cyCreateStr, "relations", props );
		}			
	}

	public String getRelationTypesSparql ()
	{
		return relationTypesSparql;
	}

	public void setRelationTypesSparql ( String relationTypesSparql )
	{
		this.relationTypesSparql = relationTypesSparql;
	}

	public String getRelationPropsSparql ()
	{
		return relationPropsSparql;
	}

	public void setRelationPropsSparql ( String relationPropsSparql )
	{
		this.relationPropsSparql = relationPropsSparql;
	}
	
}
