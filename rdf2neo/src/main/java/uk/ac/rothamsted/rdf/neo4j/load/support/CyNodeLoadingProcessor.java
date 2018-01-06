package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Resource;

import uk.ac.ebi.utils.threading.SizeBasedBatchProcessor;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
public class CyNodeLoadingProcessor extends SizeBasedBatchProcessor<NeoDataManager, Long>
{
	public CyNodeLoadingProcessor ()
	{
		super ();
		this.setDestinationMaxSize ( 100000 );
		this.setDestinationSupplier ( () -> 0l );
	}

	@Override
	protected long getDestinationSize ( Long foo ) {
		return this.getDestinationMaxSize () + 1;
	}

	@Override
	public void process ( NeoDataManager dataMgr, Object...opts )
	{
		log.info ( "Starting Cypher Nodes Loading" );
		
		long limit = this.getDestinationMaxSize ();
		CyNodeLoadingHandler handler = (CyNodeLoadingHandler) this.getConsumer ();
		handler.setSparqlQuerySize ( limit );

		for ( long offset = 0; !handler.dataFinished (); offset += limit )
			handleNewTask ( offset, true );
		
		// We don't need to force the last one, since at this point everything was processed already.
		this.waitExecutor ( "Waiting for Cyhper Node Loading tasks to finish" );
		log.info ( "Cypher Nodes Loading ended" );
	}	
}
