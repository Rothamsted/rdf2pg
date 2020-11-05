package uk.ac.rothamsted.rdf.pg.load.support.graphml;

import static org.apache.commons.text.StringEscapeUtils.escapeXml11;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.EDGE_TAG_END;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.EDGE_TAG_START;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.ID_ATTR;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.LABEL_EDGE_ATTR;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.SOURCE_ATTR;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.TARGET_ATTR;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.writeGraphMLProperties;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.writeXMLAttrib;

import java.util.Map;
import java.util.Set;

import org.apache.jena.query.QuerySolution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.PGRelationHandler;
import uk.ac.rothamsted.rdf.pg.load.support.entities.PGRelation;
import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManager;

/**
 * Similarly to {@link GraphMLNodeLoadingHandler}, this is used to {@link GraphMLRelationLoadingProcessor} to process
 * relation mappings from RDF and export them into GraphML.
 *
 * @author cbobed
 * 
 * <dl>
 *   <dt>Date:</dt>
 *   <dd>16 Apr 2020</dd>
 * </dl>
 *
 */
@Component
@Scope ( scopeName = "loadingSession" )
public class GraphMLRelationLoadingHandler extends PGRelationHandler
{
	@Autowired
	private GraphMLDataManager gmlDataMgr; 


	public GraphMLRelationLoadingHandler ()
	{
		super ();
	}

	@Override
	public void accept ( Set<QuerySolution> relRecords )
	{
		this.renameThread ( "gmlRelX:" );
		log.trace ( "Begin of {} relations", relRecords.size () );

		RdfDataManager rdfMgr = this.getRdfDataManager ();
		for ( QuerySolution row : relRecords )
		{
			PGRelation cyRelation = rdfMgr.getPGRelation ( row );
			rdfMgr.setPGRelationProps ( cyRelation, this.getRelationPropsSparql () );

			String type = cyRelation.getType ();

			Map<String, Object> relParams = gmlDataMgr.flatPGProperties ( cyRelation );

			// We have a top map containing basic relation elements (from, to, properties)
			relParams.put ( "fromIri", String.valueOf ( cyRelation.getFromIri () ) );
			relParams.put ( "toIri", String.valueOf ( cyRelation.getToIri () ) );
			relParams.put ( "iri", String.valueOf ( cyRelation.getIri () ) );
			
			// We need to gather property types, which go to the GraphML header
			relParams
			.keySet ()
			.forEach ( gmlDataMgr::gatherEdgeProperty );

			// Let's write it
			//
			var out = new StringBuilder ();
						
			out.append ( EDGE_TAG_START );
			writeXMLAttrib ( ID_ATTR, (String) relParams.get ( "iri" ), out );
			writeXMLAttrib ( SOURCE_ATTR, (String) relParams.get ( "fromIri" ), out );
			writeXMLAttrib ( TARGET_ATTR, (String) relParams.get ( "toIri" ), out );
			writeXMLAttrib ( LABEL_EDGE_ATTR, (String) escapeXml11 ( type ), out );
			out.append ( " >" );

			// we also include the type as a property of the edge
			relParams.put ( LABEL_EDGE_ATTR, type );
			writeGraphMLProperties ( relParams, out );

			out.append ( EDGE_TAG_END ).append ( "\n" );

			gmlDataMgr.appendEdgeOutput ( out.toString () );
		}

		log.trace ( "ML {} relation(s) exported", relRecords.size () );
	}
	
	/** 
	 * @TODO Review this
	 * Required to be able to access the configuration filename - which is taken from the 
	 * Spring configuration file 
	 */
	public GraphMLDataManager getGraphMLDataManager() {
		return gmlDataMgr; 
	}

}
