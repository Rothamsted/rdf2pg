package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.HashSet;
import java.util.Set;

import uk.ac.ebi.utils.threading.SizeBasedBatchProcessor;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
public class CyRelationLoadingProcessor extends SizeBasedBatchProcessor<NeoDataManager, Long>
{	
	public CyRelationLoadingProcessor ()
	{
		super ();
		this.setDestinationMaxSize ( 100000 );
		this.setDestinationSupplier ( () -> 0l );
	}

	@Override
	protected long getDestinationSize ( Long dest ) {
		return this.getDestinationMaxSize () + 1;
	}

	@Override
	public void process ( NeoDataManager dataMgr, Object...opts )
	{
		log.info ( "Starting Cypher Relations Loading" );

		long limit = this.getDestinationMaxSize ();
		CyRelationLoadingHandler handler = (CyRelationLoadingHandler) this.getConsumer ();
		handler.setSparqlQuerySize ( limit );

		for ( long offset = 0; !handler.dataFinished (); offset += limit )
			handleNewTask ( offset, true );

		// We don't need to force the last one, since at this point everything was processed already.
		this.waitExecutor ( "Waiting for Cyhper Relation Loading tasks to finish" );
		log.info ( "Cypher Relations Loading ended" );
	}
}
