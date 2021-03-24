package uk.ac.rothamsted.kg.rdf2pg.graphml.export.support;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.threading.HackedBlockingQueue;
import uk.ac.ebi.utils.threading.batchproc.BatchProcessor;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGNodeMakeProcessor;

/**
 * <H1>The Node Export processor</H1>
 * 
 * <p>This gets node IRIs from a SPARQL query and then send them to a 
 * {@link GraphMLNodeExportHandler}, for issuing Cypher creation commands. Being a subclass of {@link BatchProcessor}, 
 * this processor manages the graphML export in a multi-thread mode.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public class GraphMLNodeExportProcessor extends PGNodeMakeProcessor<GraphMLNodeExportHandler>
{
	public GraphMLNodeExportProcessor ()
	{
		super ();
		// Might be useful to experiment with different degrees of parallelism
		// 1, 1 essentially makes things sequential
		//this.setExecutor ( HackedBlockingQueue.createExecutor (1, 1) );
	}
}
