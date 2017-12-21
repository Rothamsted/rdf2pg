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
public class CypherLoadingProcessor extends SizeBasedBatchProcessor<NeoDataManager, Set<Resource>>
{
	private String sparqlNodeIris;
	
	public CypherLoadingProcessor ()
	{
		super ();
		this.setDestinationMaxSize ( 10000 );
		this.setDestinationSupplier ( () -> new HashSet<> () );
	}

	@Override
	protected long getDestinationSize ( Set<Resource> dest ) {
		return dest.size ();
	}

	@Override
	public void process ( NeoDataManager dataMgr, Object...opts )
	{
		@SuppressWarnings ( "unchecked" )
		Set<Resource> chunk[] = new Set[] { this.getDestinationSupplier ().get () };
		
		dataMgr.processNodeIris ( this.sparqlNodeIris, res -> 
		{
			chunk [ 0 ].add ( res );
			chunk[ 0 ] = handleNewTask ( chunk[ 0 ] );
		});
				
		handleNewTask ( chunk [ 0 ], true );
		this.waitExecutor ( "Waiting for Cyhper Loading tasks to finish" );
	}

	public String getSparqlNodeIris ()
	{
		return sparqlNodeIris;
	}

	public void setSparqlNodeIris ( String sparqlNodeIris )
	{
		this.sparqlNodeIris = sparqlNodeIris;
	}
	
}
