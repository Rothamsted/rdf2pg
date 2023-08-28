package uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.neo4j.driver.exceptions.ClientException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGRelationHandler;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.entities.PGRelation;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;

/**
 * Similarly to {@link CyNodeLoadingHandler}, this is used to {@link CyRelationLoadingProcessor} to process relation 
 * mappings from RDF and load them into Cypher/Neo4J. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public class CyRelationLoadingHandler extends PGRelationHandler
{
	private Neo4jDataManager neo4jDataManager;
	
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

		RdfDataManager rdfMgr = this.getRdfDataManager ();
		Neo4jDataManager neoMgr = this.getNeo4jDataManager ();

		// Pre-process relation data in a form suitable for Cypher processing, i.e., group relation data on a 
		// per-relation type basis and arrange each relation as a map of key/value properties.
		//
		for ( QuerySolution row: relRecords )
		{
			PGRelation pgRelation = rdfMgr.getPGRelation ( row );
			rdfMgr.setPGRelationProps ( pgRelation, this.getRelationPropsSparql () );

			String relType = pgRelation.getType ();
			
			List<Map<String, Object>> cyRels = cyData.computeIfAbsent ( 
				relType, tk -> new LinkedList<> ()
			);
			
			Map<String, Object> cyparams = new HashMap<> ();
			// We have a top map containing basic relation elements (from, to, properties)
			cyparams.put ( "fromIri", String.valueOf ( pgRelation.getFromIri () ) );
			cyparams.put ( "toIri", String.valueOf ( pgRelation.getToIri () ) );
			// And then we have an inner map containing the relation properties/attributes
			cyparams.put ( "properties", neoMgr.flatPGProperties ( pgRelation ) );
			cyRels.add ( cyparams );				
		}
		
		// OK, ready to call Neo!
		//
		log.trace ( "Sending {} relation(s) to Cypher", relRecords.size () );
		
		// The relation type ('%2$s') is a constant wrt the underlining Cypher processor, for us it is instead a parameter 
		// that is instantiated by the loop below.
		//
		// Nodes are always identified by means of their default label ('%1$s'), which is always created for them. 
		// 
		String cypherCreateRel = 
			"UNWIND $relations AS rel\n" +
		  // So, every item that is unwound has from/to and a property map
		  // The property map always contain the relation IRI
			"MATCH ( from:`%1$s`{ iri: rel.fromIri } ), ( to:`%1$s`{ iri: rel.toIri } )\n" +
			"CREATE (from)-[r:`%2$s`]->(to)\n" +
			"SET r = rel.properties";
			
		long relsCtr = 0;
		String defaultLabel = neoMgr.getDefaultLabel ();
		for ( Entry<String, List<Map<String, Object>>> cyDataE: cyData.entrySet () )
		{
			String type = cyDataE.getKey ();
			
			String cyCreateStr = String.format ( cypherCreateRel, defaultLabel, type );
			List<Map<String, Object>> props = cyDataE.getValue ();
			
			neoMgr.runCypher ( cyCreateStr, "relations", props );
			relsCtr += props.size ();
			
			// And now, index the iri for this type (supported since Neo4j 4.3
			try {
				neoMgr.runCypher ( String.format ( 
					"CREATE INDEX IF NOT EXISTS FOR ()-[r:`%s`]-() ON (r.iri)", type 
				));
			}
			catch ( ClientException ex )
			{
				// This seems to happen due to concurrency, the index is already there, so let's give up the duplicated
				// attempt
				if ( !StringUtils.containsIgnoreCase ( ex.getMessage (), "equivalent index already exists" ) )
					throw ex;
			}			
		}
		
		log.debug ( "{} actual relations(s) sent to Cypher", relsCtr );		
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
