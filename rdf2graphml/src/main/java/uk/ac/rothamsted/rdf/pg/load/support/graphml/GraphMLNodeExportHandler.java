package uk.ac.rothamsted.rdf.pg.load.support.graphml;

import static org.apache.commons.text.StringEscapeUtils.escapeXml11;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.ID_ATTR;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.LABEL_VERTEX_ATTR;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.NODE_TAG_END;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.NODE_TAG_START;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.writeGraphMLProperties;
import static uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLUtils.writeXMLAttrib;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.PGNodeHandler;
import uk.ac.rothamsted.rdf.pg.load.support.entities.PGNode;

/**
 * <h1>The GraphML Node Loading handler.</h1>
 *
 * This generates GraphML output corresponding to sets of {@link PGNode property graph nodes} mapped from RDF.
 * Neo4j DB.
 * 
 * @author cbobed
 * <dl><dt>Date:</dt><dd>16 Apr 2020</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class GraphMLNodeExportHandler extends PGNodeHandler
{
	@Autowired
	private GraphMLDataManager graphmlDataMgr; 
	
	@Override
	public void accept ( Set<Resource> nodeResources )
	{
		// TODO: The node/relation preparation is common code, doesn't depend on
		// the target, FACTORISE THE COPY-PASTE!!!
		
		this.renameThread ( "graphmlNodeX:" );
		log.trace ( "Begin graphML export of {} node(s)", nodeResources.size () );
					
		var rdfMgr = this.getRdfDataManager ();
		String defaultLabel = graphmlDataMgr.getDefaultLabel ();

		for ( Resource nodeRes: nodeResources )
		{
			PGNode pgNode = rdfMgr.getPGNode ( nodeRes, this.getLabelsSparql (), this.getNodePropsSparql () );
			Map<String, Object> nodeProps = graphmlDataMgr.flatPGProperties ( pgNode );
					
			SortedSet<String> labels = new TreeSet<> ( pgNode.getLabels () );
			labels.add ( defaultLabel );
			
			String labelsStr = labels
				.stream ()
				.map ( label -> label.replace ( "\"", "\\\"" ) )
				.collect ( Collectors.joining ( ":" ) );
			
			// We need to gather property types, which go to the GraphML header
			nodeProps
			.keySet ()
			.forEach ( graphmlDataMgr::gatherNodeProperty );
			
			// And now write it
			//
			// URI nodeIRI = URI.create((String)node.get("iri"));
			var out = new StringBuilder ();
			out.append ( NODE_TAG_START );
			
			writeXMLAttrib ( ID_ATTR, (String) nodeProps.get ( "iri" ), out );
			out.append(" "); 
			writeXMLAttrib ( LABEL_VERTEX_ATTR, escapeXml11 ( labelsStr ), out );
			out.append ( " >" );
			
			// we also include them as property of the node in the labels field (to use them in other indexes)
			nodeProps.put ( LABEL_VERTEX_ATTR, labelsStr );
			writeGraphMLProperties ( nodeProps, out );
			
			out.append ( NODE_TAG_END ).append ( "\n" );
			
			graphmlDataMgr.appendNodeOutput ( out.toString () );			
		}
		
		log.debug ( "{} node(s) sent to ML", nodeResources.size () );
	}

	/**
	 * This is usually set by Spring. This setter is for the tests running outside of Spring.
	 */
	public void setGraphmlDataMgr ( GraphMLDataManager graphmlDataMgr )
	{
		this.graphmlDataMgr = graphmlDataMgr;
	}
}
