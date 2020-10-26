package uk.ac.rothamsted.rdf.pg.load.support.graphml;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.threading.batchproc.BatchProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.PGNodeLoadingProcessor;

/**
 * <H1>The Node Loading processor</H1>
 * 
 * <p>This gets node IRIs from a SPARQL query and then send them to a 
 * {@link GraphMLNodeExportHandler}, for issuing Cypher creation commands. Being a subclass of {@link BatchProcessor}, 
 * this processor manages the Cypher loading in a multi-thread mode.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class GraphMLNodeLoadingProcessor extends PGNodeLoadingProcessor<GraphMLNodeExportHandler>
{
	// It's like the base class, it just needs generics instantiation and Spring annotations.
}
