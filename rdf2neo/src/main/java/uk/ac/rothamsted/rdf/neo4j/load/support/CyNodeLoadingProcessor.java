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
public class CyNodeLoadingProcessor extends SizeBasedBatchProcessor<NeoDataManager, Set<Resource>>
{
	private String nodeIrisSparql;
	
	public CyNodeLoadingProcessor ()
	{
		super ();
		this.setDestinationMaxSize ( 100000 );
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
		
		dataMgr.processNodeIris ( nodeIrisSparql, res ->
		{
			chunk [ 0 ].add ( res );
			chunk [ 0 ] = handleNewTask ( chunk [ 0 ] );
		});
		
		handleNewTask ( chunk [ 0 ], true );
		
		// We don't need to force the last one, since at this point everything was processed already.
		this.waitExecutor ( "Waiting for Cyhper Node Loading tasks to finish" );
		log.info ( "Cypher Nodes Loading ended" );
	}

	public String getNodeIrisSparql ()
	{
		return nodeIrisSparql;
	}

	public void setNodeIrisSparql ( String nodeIrisSparql )
	{
		this.nodeIrisSparql = nodeIrisSparql;
	}
}
