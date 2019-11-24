package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import uk.ac.ebi.utils.threading.batchproc.collectors.SetBatchCollector;
import uk.ac.ebi.utils.threading.batchproc.processors.SetBasedBatchProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.SimpleCyLoader;

/**
 * <H1>The base for a Cypher/Neo4j loading processor.</H1>
 * 
 * <p>We use this just to factorise a few common parameters and methods.</p>
 *
 * <p>@see CyNodeLoadingProcessor and {@link CyRelationLoadingProcessor}.</p>
 * 
 * <p>This class extends {@link SizedBatchProcessor} with the generic type {@code Set<T>}, since each loader
 * processes a collection of entity pointers (node IRIs/relation base structures), which are obtained from RDF, 
 * via SPARQL mappings (see {@link SimpleCyLoader}).</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>13 Jan 2018</dd></dl>
 *
 */
public abstract class CyLoadingProcessor<T, BJ extends CypherLoadingHandler<T>> 
	extends SetBasedBatchProcessor<T, BJ>
	implements AutoCloseable
{
	public CyLoadingProcessor ()
	{
		super ();
		this.setBatchCollector ( new SetBatchCollector<> ( 2500 ) );
	}


	@Autowired ( required = false ) @Qualifier ( "batchMaxSize" )
	// I'm here just to use Spring annotations 
	public void setBatchMaxSize ( int maxBatchSize ) {
		this.getBatchCollector ().setMaxBatchSize ( maxBatchSize );
	}
	
	/**
	* If the {@link #getBatchJob() consumer} is {@link AutoCloseable}, invokes its {@link AutoCloseable#close()}
	* method.
	* 
	*/
	@Override
	public void close () throws Exception
	{
		Consumer<?> consumer = this.getBatchJob ();
		if ( consumer != null && consumer instanceof AutoCloseable ) ((AutoCloseable) consumer).close ();
	}	
}