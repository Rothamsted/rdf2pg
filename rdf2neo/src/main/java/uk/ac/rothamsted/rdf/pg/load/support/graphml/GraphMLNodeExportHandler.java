package uk.ac.rothamsted.rdf.pg.load.support.graphml;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.PGNodeHandler;
import uk.ac.rothamsted.rdf.pg.load.support.entities.PGNode;

/**
 * <h1>The GraphML Node Loading handler.</h1>
 *
 * <p>This is used by {@link GraphMLNodeLoadingProcessor} and corresponds to the tasks that are run to load 
 * sets of nodes in GraphML.</p>
 *
 * @author cbobed
 * <dl><dt>Date:</dt><dd>16 Apr 2020</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class GraphMLNodeExportHandler extends PGNodeHandler
{
	@Autowired
	private GMLDataManager gmlDataMgr; 
	
	@Override
	public void accept ( Set<Resource> nodeResources )
	{
		// TODO: The node/relation preparation is common code, doesn't depend on
		// the target, FACTORISE THE COPY-PASTE!!!
		
		this.renameThread ( "graphMLNodeLoad:" );
		log.trace ( "Begin graphML export of {} node(s)", nodeResources.size () );
					
		var rdfMgr = this.getRdfDataManager ();
		String defaultLabel = gmlDataMgr.getDefaultLabel ();
		
		nodeResources.parallelStream ()
		.forEach ( nodeRes ->
		{
			PGNode pgNode = rdfMgr.getPGNode ( nodeRes, this.getLabelsSparql (), this.getNodePropsSparql () );
			Map<String, Object> nodeProps = gmlDataMgr.flatPGProperties ( pgNode );
					
			SortedSet<String> labels = new TreeSet<> ( pgNode.getLabels () );
			labels.add ( defaultLabel );
			
			String labelsStr = labels
				.stream ()
				.map ( label -> label.replace ( "\"", "\\\"" ) )
				.collect ( Collectors.joining ( ":" ) );
			
			// We need to gather property types, which go to the GraphML header
			nodeProps
			.keySet ()
			.forEach ( gmlDataMgr::gatherNodeProperty );
			
			// And now write it
			//
			// URI nodeIRI = URI.create((String)node.get("iri"));
			var out = new StringBuilder ();
			out.append ( GraphMLUtils.NODE_TAG_START );
			out.append ( GraphMLUtils.ID_ATTR ).append ( "=\"" )
				.append ( (String) nodeProps.get ( "iri" ) ).append ( "\" " );
			// we write the labels
			out.append ( GraphMLUtils.LABEL_VERTEX_ATTR ).append ( "=\"" )
				.append ( StringEscapeUtils.escapeXml11 ( labelsStr ) ).append ( "\" >" );
			// we include them as property of the node in the labels field (to use them in other indexes)
			out.append ( GraphMLUtils.DATA_TAG_START );
			out.append ( GraphMLUtils.KEY_ATTR ).append ( "=\"" ).append ( GraphMLUtils.LABEL_VERTEX_ATTR )
					.append ( "\" >" );
			out.append ( StringEscapeUtils.escapeXml11 ( labelsStr ) );
			out.append ( GraphMLUtils.DATA_TAG_END );
			// we write the rest of properties
			GraphMLUtils.writeXMLAttribs ( nodeProps, out );
			out.append ( GraphMLUtils.NODE_TAG_END ).append ( "\n" );
			
			gmlDataMgr.appendNodeOutput ( out.toString () );			
		});
		
		log.debug ( "{} node(s) sent to ML", nodeResources.size () );
	}
		
}
