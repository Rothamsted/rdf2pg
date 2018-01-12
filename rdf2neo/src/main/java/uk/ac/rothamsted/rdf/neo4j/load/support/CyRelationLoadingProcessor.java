package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Resource;

import uk.ac.ebi.utils.threading.SizeBasedBatchProcessor;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
public class CyRelationLoadingProcessor extends SizeBasedBatchProcessor<NeoDataManager, Set<QuerySolution>>
{	
	public CyRelationLoadingProcessor ()
	{
		super ();
		this.setDestinationMaxSize ( 100000 );
		this.setDestinationSupplier ( () -> new HashSet<> () );
	}

	@Override
	protected long getDestinationSize ( Set<QuerySolution> dest ) {
		return dest.size ();
	}

	@Override
	public void process ( NeoDataManager dataMgr, Object...opts )
	{
		log.info ( "Starting Cypher Relations Loading" );

		@SuppressWarnings ( "unchecked" )
		Set<QuerySolution> chunk[] = new Set[] { this.getDestinationSupplier ().get () };
		
		CyRelationLoadingHandler handler = (CyRelationLoadingHandler) this.getConsumer ();
		String relTypesSparql = handler.getRelationTypesSparql ();
		
		dataMgr.processRelationIris ( relTypesSparql, res ->
		{
			chunk [ 0 ].add ( res );
			chunk [ 0 ] = handleNewTask ( chunk [ 0 ] );
		});
		
		handleNewTask ( chunk [ 0 ], true );

		// We don't need to force the last one, since at this point everything was processed already.
		this.waitExecutor ( "Waiting for Cyhper Relation Loading tasks to finish" );
		log.info ( "Cypher Relations Loading ended" );
	}
}
