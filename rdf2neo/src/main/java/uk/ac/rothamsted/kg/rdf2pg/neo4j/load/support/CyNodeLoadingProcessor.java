package uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGNodeMakeProcessor;

/**
 * This simply binds {@link CyNodeLoadingHandler} to the node make processor.
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public class CyNodeLoadingProcessor extends PGNodeMakeProcessor<CyNodeLoadingHandler>
{
	// Nothing to do
}
