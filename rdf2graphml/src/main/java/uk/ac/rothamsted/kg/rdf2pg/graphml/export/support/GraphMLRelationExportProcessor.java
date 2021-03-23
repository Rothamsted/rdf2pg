package uk.ac.rothamsted.kg.rdf2pg.graphml.export.support;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.threading.HackedBlockingQueue;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGRelationMakeProcessor;

/**
 * <H1>The Relation Export Processor</H1>
 * 
 * It's like the parent, just binds the right handler. 
 * 
 * @author cbobed
 * <dl><dt>Date:</dt><dd>16 Apr 2020</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public class GraphMLRelationExportProcessor extends PGRelationMakeProcessor<GraphMLRelationExportHandler>
{
	public GraphMLRelationExportProcessor ()
	{
		super ();
		//this.setExecutor ( HackedBlockingQueue.createExecutor (1, 1) );
	}	
}
