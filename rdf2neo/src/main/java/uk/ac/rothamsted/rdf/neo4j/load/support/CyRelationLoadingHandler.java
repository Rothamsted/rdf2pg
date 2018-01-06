package uk.ac.rothamsted.rdf.neo4j.load.support;

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
public class CyRelationLoadingHandler extends CypherLoadingHandler<Long>
{
	private String relationTypesSparql, relationPropsSparql;

	public CyRelationLoadingHandler ()
	{
		super ();
	}
		
	@Override
	public void accept ( Long sparqlQueryOffset )
	{
		Thread.currentThread ().setName ( "cyRelLoad:" + sparqlQueryOffset );
		if ( this.dataFinished () && sparqlQueryOffset >= this.getOverflowQueryOffset () ) {
			log.trace ( "{} over {}, skipping", sparqlQueryOffset, this.getOverflowQueryOffset () );
			return;
		}
		
		log.trace ( "Begin at offset {}", sparqlQueryOffset );
		
		Map<String, List<Map<String, Object>>> cyData = new HashMap<> ();

		NeoDataManager dataMgr = this.getDataMgr ();
		long limit = this.getSparqlQuerySize ();
		
		long newOffset = dataMgr.processRelationIris ( 
			this.relationTypesSparql, sparqlQueryOffset, limit, 
			row -> { 
				Relation relation = dataMgr.getRelation ( row );
				dataMgr.setRelationProps ( relation, this.relationPropsSparql );
	
				String type = relation.getType ();
				List<Map<String, Object>> cyRels = cyData.get ( type );
				if ( cyRels == null ) cyData.put ( type, cyRels = new LinkedList<> () );

				Map<String, Object> cyparams = new HashMap<> (); 
				cyparams.put ( "fromIri", String.valueOf ( relation.getFromIri () ) );
				cyparams.put ( "toIri", String.valueOf ( relation.getToIri () ) );
				cyparams.put ( "properties", this.getCypherProperties ( relation ) );
				cyRels.add ( cyparams );				
		});
		
		if ( newOffset == -1 ) {
			// notify we ran out of data
			this.notifyDataFinished ( sparqlQueryOffset );
			log.trace ( "Notifyng end of data at offset {}", sparqlQueryOffset );
			return;
		}
		
		// OK, ready to call Neo!
		//
		log.trace ( "Offset {}, sending relation(s) to Cypher", sparqlQueryOffset );
		
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
		log.trace ( "{} relations(s) sent to Cypher", relsCtr );		
		log.info ( "Done SPARQL offset {}", sparqlQueryOffset );		
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
