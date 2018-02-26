package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.Set;
import java.util.function.Consumer;

import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.threading.BatchProcessor;

/**
 * <H1>The Node Loading processor</H1>
 * 
 * <p>This gets node IRIs from a SPARQL query and then send them to a 
 * {@link CyNodeLoadingHandler}, for issuing Cypher creation commands. Being a subclass of {@link BatchProcessor}, 
 * this processor manages the Cypher loading in a multi-thread mode.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class CyNodeLoadingProcessor extends CyLoadingProcessor<Resource>
{
	private String nodeIrisSparql;
	
	@Override
	public void process ( RdfDataManager dataMgr, Object...opts )
	{
		log.info ( "Starting Cypher Nodes Loading" );
		
		@SuppressWarnings ( "unchecked" )
		Set<Resource> chunk[] = new Set[] { this.getDestinationSupplier ().get () };
		
		dataMgr.processNodeIris ( this.getNodeIrisSparql (), res ->
		{
			chunk [ 0 ].add ( res );

			// This checks if we have enough items in the chunk and, if yes, it submit a new Loading job and returns a new
			// empty chunk to refill.
			chunk [ 0 ] = handleNewTask ( chunk [ 0 ] );
		});
		
		// The last chunk needs to be always submitted
		handleNewTask ( chunk [ 0 ], true );
		
		this.waitExecutor ( "Waiting for Cyhper Node Loading tasks to finish" );
		log.info ( "Cypher Nodes Loading ended" );
	}

	/**
	 * The query to be used with the {@link RdfDataManager} to fetch the IRIs of Cypher/Neo4J nodes that needs to be
	 * loaded. This usually goes together with {@link CyNodeLoadingHandler#getLabelsSparql()} and
	 * {@link CyNodeLoadingHandler#getNodePropsSparql()}.
	 * 
	 * This query <b>must</b> return the variables ?iri in its result. It must also return distinct results (we 
	 * obviously don't care if you don't use the DISTINCT SPARQL clause). 
	 */
	public String getNodeIrisSparql ()
	{
		return nodeIrisSparql;
	}

	@Autowired ( required = false ) @Qualifier ( "nodeIrisSparql" )
	public void setNodeIrisSparql ( String nodeIrisSparql )
	{
		this.nodeIrisSparql = nodeIrisSparql;
	}

	/**
	 * Does nothing but invoking {@link #setConsumer(Consumer)}. It's here just to accommodate Spring annotations. 
	 */
	@Autowired
	public CyNodeLoadingProcessor setConsumer ( CyNodeLoadingHandler handler ) {
		super.setConsumer ( handler );
		return this;
	}
}
