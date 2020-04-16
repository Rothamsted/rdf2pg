package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.function.Consumer;

import org.apache.jena.query.QuerySolution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * <H1>The Relation Loading processor</H1>
 * 
 * <p>Similarly to the {@link CyNodeLoadingProcessor}, this gets SPARQL bindings (i.e., rows) corresponding to 
 * tuples of relation basic properties () and then send them to a 
 * {@link CyRelationLoadingHandler}, for issuing Cypher creation commands about relations. As for the node processor, 
 * this processor does things in multi-thread fashion.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class CyRelationLoadingProcessor extends CyLoadingProcessor<QuerySolution, CyRelationLoadingHandler>
{	
	/**
	 * This takes the relations mapped via {@link CyRelationLoadingHandler#getRelationTypesSparql()} and creates
	 * sets of {@link QuerySolution}s that are sent to {@link CyRelationLoadingHandler} tasks.
	 */
	public void process ( RdfDataManager rdfMgr, Object...opts )
	{
		log.info ( "Starting Cypher Relations Loading" );
		
		CyRelationLoadingHandler handler = this.getBatchJob ();

		// processNodeIris() passes the IRIs obtained from SPARQL to the IRI consumer set by the BatchProcessor. The latter
		// pushes each IRI into a batch and submits a filled-up batch to the parallel executor.
		Consumer<Consumer<QuerySolution>> relIriProcessor = 
			solProc -> rdfMgr.processRelationIris ( handler.getRelationTypesSparql (), solProc );
		
		super.process ( relIriProcessor );
		log.info ( "Cypher Relations Loading ended" );
	}

	/**
	 * Does nothing but invoking {@link #setBatchJob(Consumer)}. It's here just to accommodate Spring annotations. 
	 */
	@Autowired
	public void setBatchJob ( CyRelationLoadingHandler handler ) {
		super.setBatchJob ( handler );
	}	
}
