package uk.ac.rothamsted.rdf.pg.load.support.graphml;

import org.apache.jena.query.QuerySolution;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.PGLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.PGRelationLoadingProcessor;

/**
 * <H1>The Relation Loading processor</H1>
 * 
 * <p>Similarly to the {@link GraphMLNodeLoadingProcessor}, this gets SPARQL bindings (i.e., rows) corresponding to 
 * tuples of relation basic properties () and then send them to a 
 * {@link GraphMLRelationExportHandler}, for storing the information about the relations. As for the node processor, 
 * this processor does things in multi-thread fashion.</p>
 *
 * @author cbobed
 * <dl><dt>Date:</dt><dd>16 Apr 2020</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class GraphMLRelationLoadingProcessor extends PGRelationLoadingProcessor<GraphMLRelationExportHandler>
{	

}
