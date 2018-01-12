package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.query.QuerySolution;
import org.neo4j.driver.v1.Driver;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 *
 */
public class CyRelationLoadingHandler extends CypherLoadingHandler<QuerySolution>
{
	private String relationTypesSparql, relationPropsSparql;

	public CyRelationLoadingHandler ()
	{
		super ();
	}
		
	@Override
	public void accept ( Set<QuerySolution> relRecords )
	{
		this.renameThread ( "cyRelLoad:" );
		log.trace ( "Begin of {} relations", relRecords.size () );
		
		Map<String, List<Map<String, Object>>> cyData = new HashMap<> ();

		NeoDataManager dataMgr = this.getDataMgr ();
		
		for ( QuerySolution row: relRecords )
		{
			CyRelation cyRelation = dataMgr.getCyRelation ( row );
			dataMgr.setCyRelationProps ( cyRelation, this.relationPropsSparql );

			String type = cyRelation.getType ();
			List<Map<String, Object>> cyRels = cyData.get ( type );
			if ( cyRels == null ) cyData.put ( type, cyRels = new LinkedList<> () );

			Map<String, Object> cyparams = new HashMap<> (); 
			cyparams.put ( "fromIri", String.valueOf ( cyRelation.getFromIri () ) );
			cyparams.put ( "toIri", String.valueOf ( cyRelation.getToIri () ) );
			cyparams.put ( "properties", this.getCypherProperties ( cyRelation ) );
			cyRels.add ( cyparams );				
		}
		
		// OK, ready to call Neo!
		//
		log.trace ( "Offset {}, sending relation(s) to Cypher", relRecords.size () );
		
		String cypherCreateRel = 
			"UNWIND {relations} AS rel\n" +
			"MATCH ( from:`%1$s`{ iri: rel.fromIri } ), ( to:`%1$s`{ iri: rel.toIri } )\n" +
			"CREATE (from)-[r:`%2$s`]->(to)\n" +
			"SET r = rel.properties";
			
		long relsCtr = 0;
		String defaultLabel = this.getDefaultLabel ();		
		for ( Entry<String, List<Map<String, Object>>> cyDataE: cyData.entrySet () )
		{
			String type = cyDataE.getKey ();
			
			String cyCreateStr = String.format ( cypherCreateRel, defaultLabel, type );
			List<Map<String, Object>> props = cyDataE.getValue ();
			
			this.runCypher ( cyCreateStr, "relations", props );
			
			relsCtr += props.size (); 
		}
		
		log.debug ( "{} actual relations(s) sent to Cypher", relsCtr );		
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
