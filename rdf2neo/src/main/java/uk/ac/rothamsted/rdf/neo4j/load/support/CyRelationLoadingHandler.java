package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.jena.query.QuerySolution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Similarly to {@link CyNodeLoadingHandler}, this is used to {@link CyRelationLoadingProcessor} to process relation 
 * mappings from RDF and load them into Cypher/Neo4J. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
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

		RdfDataManager rdfMgr = this.getRdfDataManager ();
		Neo4jDataManager neoMgr = this.getNeo4jDataManager ();

		// Pre-process relation data in a form suitable for Cypher processing, i.e., group relation data on a 
		// per-relation type basis and arrange each relation as a map of key/value properties.
		//
		for ( QuerySolution row: relRecords )
		{
			CyRelation cyRelation = rdfMgr.getCyRelation ( row );
			rdfMgr.setCyRelationProps ( cyRelation, this.relationPropsSparql );

			String type = cyRelation.getType ();
			List<Map<String, Object>> cyRels = cyData.get ( type );
			if ( cyRels == null ) cyData.put ( type, cyRels = new LinkedList<> () );

			Map<String, Object> cyparams = new HashMap<> ();
			// We have a top map containing basic relation elements (from, to, properties)
			cyparams.put ( "fromIri", String.valueOf ( cyRelation.getFromIri () ) );
			cyparams.put ( "toIri", String.valueOf ( cyRelation.getToIri () ) );
			// And then we have an inner map containing the relation properties/attributes
			cyparams.put ( "properties", neoMgr.getCypherProperties ( cyRelation ) );
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
			"UNWIND {relations} AS rel\n" +
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
		}
		
		log.debug ( "{} actual relations(s) sent to Cypher", relsCtr );		
	}
		
	
	/**
	 * <p>A SPARQL that must return the variables: ?iri ?type ?fromIri ?toIri and distinct result rows (whether you use 
	 * DISTINCT or not).</p>
	 * 
	 * <p>This maps to Cypher relations. fromIri and toIri must correspond to nodes defined by 
	 * {@link CyNodeLoadingProcessor#getNodeIrisSparql()}, so that such nodes, previously inserted in the Neo4J), can be 
	 * matched and linked by the relation.</p>
	 * 
	 * <p>Each relation has an ?iri identifier.
	 * For <a href = 'https://www.w3.org/TR/swbp-n-aryRelations/'>reified RDF relations</a>, this is typically the IRI of 
	 * the reified relation, for plain RDF triples, this is a fictitious IRI, which is typically built by joining and 
	 * hashing the triple subject/predicate/object IRIs (see examples in the src/test/resources).</p> 
	 * 
	 * <p>This is used both by {@link CyRelationLoadingProcessor}, to fetch all relations and their basic 
	 * properties (there must be one type per relation), and by this handler, to fetch elements like a relation 
	 * end points (from/to IRIs) and type, after the ?iri variable is bound to some specific IRI.</p> 
	 * 
	 */
	public String getRelationTypesSparql ()
	{
		return relationTypesSparql;
	}

	@Autowired	( required = false ) @Qualifier ( "relationTypesSparql" )
	public void setRelationTypesSparql ( String relationTypesSparql )
	{
		this.relationTypesSparql = relationTypesSparql;
	}

	/**
	 * <p>This is similar to {@link CyNodeLoadingHandler#getNodePropsSparql()}, it is a SPARQL query that must contain
	 * the variables ?name ?value in the result and the ?iri variable in the WHERE clause. The latter is instantiated
	 * with a specific relation IRI, to get the relation properties.</p> 
	 *
	 * <p>Because of the nature of RDF, this query will typically return properties for reified relations.</p>
	 */
	public String getRelationPropsSparql ()
	{
		return relationPropsSparql;
	}

	@Autowired ( required = false ) @Qualifier ( "relationPropsSparql" )
	public void setRelationPropsSparql ( String relationPropsSparql )
	{
		this.relationPropsSparql = relationPropsSparql;
	}
	
}
