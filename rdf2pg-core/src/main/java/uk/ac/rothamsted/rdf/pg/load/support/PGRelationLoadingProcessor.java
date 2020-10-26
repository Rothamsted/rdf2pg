package uk.ac.rothamsted.rdf.pg.load.support;

import java.util.function.Consumer;

import org.apache.jena.query.QuerySolution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManager;

/**
 * <H1>The Relation Loading processor</H1>
 * 
 * <p>Similarly to the {@link PGNodeLoadingProcessor}, this gets SPARQL bindings (i.e., rows) corresponding to 
 * tuples of relation basic properties () and then sends them to a 
 * {@link PGRelationHandler}, for generating target PG relations. As for the node processor, 
 * this processor does things in multi-thread fashion.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public abstract class PGRelationLoadingProcessor<T extends PGRelationHandler> extends PGLoadingProcessor<QuerySolution, T>
{	
	/**
	 * This takes the relations mapped via {@link PGRelationHandler#getRelationTypesSparql()} and creates
	 * sets of {@link QuerySolution}s that are sent to {@link PGRelationHandler} tasks.
	 */
	public void process ( RdfDataManager rdfMgr, Object...opts )
	{
		log.info ( "Starting Cypher Relations Loading" );
		
		T handler = this.getBatchJob ();

		// processNodeIris() passes the IRIs obtained from SPARQL to the IRI consumer set by the BatchProcessor. The latter
		// pushes the IRI into a batch and submits a full batch to the parallel executor.
		Consumer<Consumer<QuerySolution>> relIriProcessor = 
			solProc -> rdfMgr.processRelationIris ( handler.getRelationTypesSparql (), solProc );
		
		super.process ( relIriProcessor );
		log.info ( "Cypher Relations Loading ended" );
	}

	/**
	 * Does nothing but invoking {@link #setBatchJob(Consumer)}. It's here just to accommodate Spring annotations. 
	 */
	@Autowired
	public PGRelationLoadingProcessor setConsumer ( T handler ) {
		super.setBatchJob ( handler );
		return this;
	}	
}