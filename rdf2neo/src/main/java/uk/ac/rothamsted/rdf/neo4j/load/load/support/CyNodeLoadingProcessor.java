package uk.ac.rothamsted.rdf.neo4j.load.load.support;

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
public class CyNodeLoadingProcessor extends SizeBasedBatchProcessor<NeoDataManager, Set<Resource>>
{
	private String nodeIrisSparql;
	
	public CyNodeLoadingProcessor ()
	{
		super ();
		this.setDestinationMaxSize ( 1000 );
		this.setDestinationSupplier ( () -> new HashSet<> () );
	}

	@Override
	protected long getDestinationSize ( Set<Resource> dest ) {
		return dest.size ();
	}

	@Override
	public void process ( NeoDataManager dataMgr, Object...opts )
	{
		log.info ( "Starting Cypher Nodes Loading" );

		@SuppressWarnings ( "unchecked" )
		Set<Resource> chunk[] = new Set[] { this.getDestinationSupplier ().get () };
		
		long limit = this.getDestinationMaxSize ();
		for ( long offset = 0; offset != -1;)
		{
			offset = dataMgr.processNodeIris ( this.nodeIrisSparql, offset, limit, res -> chunk [ 0 ].add ( res ) );
			chunk [ 0 ] = handleNewTask ( chunk[ 0 ], true );
			if ( offset != -1 ) log.info ( "{} nodes processed", offset );
		}

		// We don't need to force the last one, since at this point everything was processed already.
		this.waitExecutor ( "Waiting for Cyhper Nodes Loading tasks to finish" );
		log.info ( "Cypher Nodes Loading ended" );
	}

	public String getNodeIrisSparql () {
		return nodeIrisSparql;
	}

	public void setNodeIrisSparql ( String nodeIrisSparql ) {
		this.nodeIrisSparql = nodeIrisSparql;
	}
	
}
