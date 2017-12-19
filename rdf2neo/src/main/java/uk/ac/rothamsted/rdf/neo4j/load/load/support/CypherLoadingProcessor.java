package uk.ac.rothamsted.rdf.neo4j.load.load.support;

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
public class CypherLoadingProcessor extends SizeBasedBatchProcessor<NeoDataManager, Set<String>>
{
	public CypherLoadingProcessor ()
	{
		super ();
		this.setDestinationSupplier ( () -> new HashSet<> () );
		this.setDestinationMaxSize ( 10000 );
	}

	@Override
	protected long getDestinationSize ( Set<String> dest ) {
		return dest.size ();
	}

	@Override
	public void process ( NeoDataManager dataMgr, Object...opts )
	{
		@SuppressWarnings ( "unchecked" )
		Set<String> chunk[] = new Set[] { this.getDestinationSupplier ().get () };
		
		dataMgr.getNodeIris ().forEach ( iri -> {
			chunk[ 0 ].add ( iri );
			chunk[ 0 ] = handleNewTask ( chunk[ 0 ] );
		});
		
		handleNewTask ( chunk [ 0 ], true );
		this.waitExecutor ( "Waiting for Cyhper Loading tasks to finish" );
	}
}
