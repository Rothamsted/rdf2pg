package uk.ac.rothamsted.kg.rdf2pg.pgmaker.support;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import uk.ac.ebi.utils.threading.batchproc.processors.SetBasedBatchProcessor;

/**
 * <H1>The base for a property graph converter processor.</H1>
 * 
 * <p>We use this just to factorise a few common parameters and methods.</p>
 *
 * <p>@see {@link PGNodeMakeProcessor} and {@link PGRelationMakeProcessor}.</p>
 * 
 * <p>This class extends {@link SetBasedBatchProcessor}, since each maker processes a collection of entity 
 * pointers (node IRIs/relation base structures), which are obtained from RDF, via SPARQL 
 * mappings (see specific implementation packages).</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>13 Jan 2018</dd></dl>
 *
 */
public abstract class PGMakerProcessor<T, H extends PGEntityHandler<T>> 
	extends SetBasedBatchProcessor<T, H>
	implements AutoCloseable
{
	public PGMakerProcessor ()
	{
		super ( 2500 );
	}


	// I'm here just to use Spring annotations 
	@Autowired ( required = false ) @Qualifier ( "batchMaxSize" )
	@Override
	public void setMaxBatchSize ( int maxBatchSize ) {
		super.setMaxBatchSize ( maxBatchSize );
	}
	
	/**
	 * I'm here just to accommodate Spring annotations. 
	 */
	@Autowired
	@Override
	public void setBatchJob ( H handler ) {
		super.setBatchJob ( handler );
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