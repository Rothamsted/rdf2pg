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
public class CyRelationLoadingProcessor extends SizeBasedBatchProcessor<NeoDataManager, Set<Relation>>
{
	private String relationIrisSparql;
	
	public CyRelationLoadingProcessor ()
	{
		super ();
		this.setDestinationMaxSize ( 1000 );
		this.setDestinationSupplier ( () -> new HashSet<> () );
	}

	@Override
	protected long getDestinationSize ( Set<Relation> dest ) {
		return dest.size ();
	}

	@Override
	public void process ( NeoDataManager dataMgr, Object...opts )
	{
		log.info ( "Starting Cypher Relations Loading" );

		@SuppressWarnings ( "unchecked" )
		Set<Relation> chunk[] = new Set[] { this.getDestinationSupplier ().get () };
		
		long limit = this.getDestinationMaxSize ();
		for ( long offset = 0; offset != -1;)
		{
			offset = dataMgr.processRelationIris ( 
				this.relationIrisSparql, 
				offset, 
				limit, 
				row -> { 
					Relation rel = dataMgr.getRelation ( row );
					chunk [ 0 ].add ( rel );
				}
			);
			chunk [ 0 ] = handleNewTask ( chunk[ 0 ], true );
			if ( offset != -1 ) log.info ( "{} relations processed", offset );
		}

		// We don't need to force the last one, since at this point everything was processed already.
		this.waitExecutor ( "Waiting for Cyhper Relations Loading to finish" );
		log.info ( "Cypher Relations Loading ended" );
	}

	public String getRelationIrisSparql () {
		return relationIrisSparql;
	}

	public void setRelationIrisSparql ( String relationIrisSparql ) {
		this.relationIrisSparql = relationIrisSparql;
	}	
}
