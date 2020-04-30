package uk.ac.rothamsted.rdf.pg.support.graphml;

import java.util.function.Consumer;

import org.apache.jena.query.QuerySolution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.RdfDataManager;

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
public class GraphMLRelationLoadingProcessor extends GraphMLLoadingProcessor<QuerySolution, GraphMLRelationExportHandler>
{	
	/**
	 * This takes the relations mapped via {@link GraphMLRelationExportHandler#getRelationTypesSparql()} and creates
	 * sets of {@link QuerySolution}s that are sent to {@link GraphMLRelationExportHandler} tasks.
	 */
	public void process ( RdfDataManager rdfMgr, Object...opts )
	{
		log.info ( "Starting Cypher Relations Loading" );
		
		GraphMLRelationExportHandler handler = this.getBatchJob ();

		// processNodeIris() passes the IRIs obtained from SPARQL to the IRI consumer set by the BatchProcessor. The latter
		// pushes the IRI into a batch and submits a full batch to the parallel executor.
		Consumer<Consumer<QuerySolution>> relIriProcessor = 
			solProc -> rdfMgr.processRelationIris ( handler.getRelationTypesSparql (), solProc );
		
		super.process ( relIriProcessor );
		log.info ( "GraphML Loading ended" );
	}

	/**
	 * Does nothing but invoking {@link #setBatchJob(Consumer)}. It's here just to accommodate Spring annotations. 
	 */
	@Autowired
	public GraphMLRelationLoadingProcessor setConsumer ( GraphMLRelationExportHandler handler ) {
		super.setBatchJob ( handler );
		return this;
	}	
}
