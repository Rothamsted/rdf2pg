package uk.ac.rothamsted.rdf.pg.load.support.graphml;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.PGRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.entities.PGRelation;

/**
 * <H1>The Relation Loading processor</H1>
 * 
 * As per {@link PGRelationLoadingProcessor} contract, gather {@link PGRelation PG relations} from RDF and 
 * uses a {@link GraphMLRelationLoadingHandler} to output corresponding GraphML code that defines the same
 * relations. 
 *
 * @author cbobed
 * <dl><dt>Date:</dt><dd>16 Apr 2020</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class GraphMLRelationLoadingProcessor extends PGRelationLoadingProcessor<GraphMLRelationLoadingHandler>
{	

}
