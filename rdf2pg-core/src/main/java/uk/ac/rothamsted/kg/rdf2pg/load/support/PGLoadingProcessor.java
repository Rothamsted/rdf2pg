package uk.ac.rothamsted.kg.rdf2pg.load.support;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import uk.ac.ebi.utils.threading.batchproc.collectors.SetBatchCollector;
import uk.ac.ebi.utils.threading.batchproc.processors.SetBasedBatchProcessor;

/**
 * <H1>The base for a property graph converter processor.</H1>
 * 
 * <p>We use this just to factorise a few common parameters and methods.</p>
 *
 * <p>@see {@link PGNodeLoadingProcessor} and {@link PGRelationLoadingProcessor}.</p>
 * 
 * <p>This class extends {@link SetBasedBatchProcessor}, since each loader processes a collection of entity 
 * pointers (node IRIs/relation base structures), which are obtained from RDF, via SPARQL 
 * mappings (see specific implementation packages).</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>13 Jan 2018</dd></dl>
 *
 */
public abstract class PGLoadingProcessor<T, BJ extends PGEntityHandler<T>> 
	extends SetBasedBatchProcessor<T, BJ>
	implements AutoCloseable
{
	public PGLoadingProcessor ()
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