package uk.ac.rothamsted.rdf.pg.support.graphml;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.jena.query.QuerySolution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.CyRelation;
import uk.ac.rothamsted.rdf.pg.load.support.GraphMLConfiguration;
import uk.ac.rothamsted.rdf.pg.load.support.GraphMLUtils;
import uk.ac.rothamsted.rdf.pg.load.support.RdfDataManager;

/**
 * Similarly to {@link GraphMLNodeLoadingHandler}, this is used to {@link GraphMLRelationLoadingProcessor} to process relation 
 * mappings from RDF and export them into GraphML. 
 *
 * @author cbobed
 * <dl><dt>Date:</dt><dd>16 Apr 2020</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class GraphMLRelationExportHandler extends GraphMLLoadingHandler<QuerySolution>
{
	private String relationTypesSparql, relationPropsSparql;

	// First version: in order to create the key values, we have to store the property names gathered 
	// for each of the nodes 
		
	private static HashSet<String> gatheredEdgeProperties = new HashSet<String>();
	{	
		// in this case the iri is built using the md5
		// we get then the type as well at the same point
		addGatheredEdgeProperty(GraphMLUtils.LABEL_EDGE_ATTR); 
	}
	
	// for threading safety purposes
	private static synchronized void addGatheredEdgeProperty(String property) {
		gatheredEdgeProperties.add(property); 
	}
	
	public static synchronized HashSet<String> getGatheredEdgeProperties() {
		return gatheredEdgeProperties; 
	}
	
	// semaphore to write in the appropriate file
	private static Object lock = new Object(); 
	
	
	public GraphMLRelationExportHandler ()
	{
		super ();
	}
		
	@Override
	public void accept ( Set<QuerySolution> relRecords )
	{
		this.renameThread ( "graphMLRelLoad:" );
		log.trace ( "Begin of {} relations", relRecords.size () );
		
		Map<String, List<Map<String, Object>>> graphMLRelData = new HashMap<> ();

		RdfDataManager rdfMgr = this.getRdfDataManager ();

		// Pre-process relation data in a form suitable for Cypher processing, i.e., group relation data on a 
		// per-relation type basis and arrange each relation as a map of key/value properties.
		//
		for ( QuerySolution row: relRecords )
		{
			CyRelation cyRelation = rdfMgr.getCyRelation ( row );
			rdfMgr.setCyRelationProps ( cyRelation, this.relationPropsSparql );

			String type = cyRelation.getType ();
			List<Map<String, Object>> cyRels = graphMLRelData.get ( type );
			if ( cyRels == null ) graphMLRelData.put ( type, cyRels = new LinkedList<> () );

			Map<String, Object> cyparams = new HashMap<> ();
			// We have a top map containing basic relation elements (from, to, properties)
			cyparams.put ( "fromIri", String.valueOf ( cyRelation.getFromIri () ) );
			cyparams.put ( "toIri", String.valueOf ( cyRelation.getToIri () ) );
			cyparams.put ( "iri", String.valueOf (cyRelation.getIri())); 
			// And then we have an inner map containing the relation properties/attributes
			
			HashMap<String, Object> relProperties = new HashMap<> ();
			for ( Entry<String, Set<Object>> attre: cyRelation.getProperties ().entrySet () )
			{
				Set<Object> vals = attre.getValue ();
				if ( vals.isEmpty () ) continue; // shouldn't happen, but just in case
				
				Object cyAttrVal = vals.size () > 1 ? vals.toArray ( new Object [ 0 ] ) : vals.iterator ().next ();
				relProperties.put ( attre.getKey (), cyAttrVal );
				// we add the property to the global keys
				addGatheredEdgeProperty(attre.getKey()); 
			}
			cyparams.put ( "properties", relProperties );
			
			cyRels.add ( cyparams );				
		}
		
		log.trace ( "Exporting {} relation(s) to ", relRecords.size (), GraphMLConfiguration.getOutputFile()+GraphMLConfiguration.EDGE_FILE_EXTENSION );
	
		graphMLRelData.entrySet().parallelStream().parallel().forEach(
			// the relationships are grouped by type
			entry -> {
				// we don't filter any property in the first version
				String type = entry.getKey(); 
				List<Map<String, Object>> relPropertiesList = entry.getValue(); 
				relPropertiesList.parallelStream().parallel().forEach(
					rel -> {
//						URI fromIRI = URI.create((String)rel.get("fromIri")); 
//						URI toIRI = URI.create((String)rel.get("toIri"));
						// warning: it's not the IRI of the property, but a generated one for the 
						// actual instance of the relationship
//						URI relIRI = URI.create((String)rel.get("iri")); 
						StringBuilder strB = new StringBuilder(); 
						strB.append(GraphMLUtils.EDGE_TAG_START); 
						strB.append(GraphMLUtils.ID_ATTR).append("=\"").append((String)rel.get("iri")).append("\" ");
						// we now establish the oriented edge
						strB.append(GraphMLUtils.SOURCE_ATTR).append("=\"").append((String)rel.get("fromIri")).append("\" ");
						strB.append(GraphMLUtils.TARGET_ATTR).append("=\"").append((String)rel.get("toIri")).append("\" ");
						// apparently gremlin/janusgraph takes into account the labels for the edges (not for the vertex)
						strB.append(GraphMLUtils.LABEL_EDGE_ATTR).append("=\"").append(StringEscapeUtils.escapeXml11(type)).append("\" >"); 
						
						// we include the type as a property of the edge 
						strB.append(GraphMLUtils.DATA_TAG_START); 
						strB.append(GraphMLUtils.KEY_ATTR).append("=\"").append(GraphMLUtils.LABEL_EDGE_ATTR).append("\">").append(StringEscapeUtils.escapeXml11(type)); 
						strB.append(GraphMLUtils.DATA_TAG_END); 
						
						// now the possible properties 
						strB.append(GraphMLUtils.dataValues((Map<String, Object>)rel.get("properties"))); 
						strB.append(GraphMLUtils.EDGE_TAG_END).append("\n"); 
						String relString = strB.toString(); 
						synchronized (lock) {
							try {
								Files.writeString(Paths.get(GraphMLConfiguration.getOutputFile()+GraphMLConfiguration.EDGE_FILE_EXTENSION), 
										relString, StandardOpenOption.CREATE, StandardOpenOption.APPEND); 
							}
							catch (IOException e) {
								log.error("Problems writing the relation {}", relString); 
								e.printStackTrace();
							}
						}

					}
				);
			}
				
		);
		log.debug ("Rel export process finished");		
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
