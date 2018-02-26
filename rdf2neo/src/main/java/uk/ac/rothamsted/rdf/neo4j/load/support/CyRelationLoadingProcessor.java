package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.Set;
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
public class CyRelationLoadingProcessor extends CyLoadingProcessor<QuerySolution>
{	
	/**
	 * This takes the relations mapped via {@link CyRelationLoadingHandler#getRelationTypesSparql()} and creates
	 * sets of {@link QuerySolution}s that are sent to {@link CyRelationLoadingHandler} tasks.
	 */
	@Override
	public void process ( RdfDataManager rdfMgr, Object...opts )
	{
		log.info ( "Starting Cypher Relations Loading" );

		@SuppressWarnings ( "unchecked" )
		Set<QuerySolution> chunk[] = new Set[] { this.getDestinationSupplier ().get () };
		
		CyRelationLoadingHandler handler = (CyRelationLoadingHandler) this.getConsumer ();
		
		rdfMgr.processRelationIris ( handler.getRelationTypesSparql (), res ->
		{
			chunk [ 0 ].add ( res );
			// This decides if the chunk is big enough and, if yes, submits a new task and returns a new empty chunk.
			chunk [ 0 ] = handleNewTask ( chunk [ 0 ] );
		});

		// Last chunk has always to be submitted.
		handleNewTask ( chunk [ 0 ], true );

		this.waitExecutor ( "Waiting for Cyhper Relation Loading tasks to finish" );
		log.info ( "Cypher Relations Loading ended" );
	}

	/**
	 * Does nothing but invoking {@link #setConsumer(Consumer)}. It's here just to accommodate Spring annotations. 
	 */
	@Autowired
	public CyRelationLoadingProcessor setConsumer ( CyRelationLoadingHandler handler ) {
		super.setConsumer ( handler );
		return this;
	}	
}
