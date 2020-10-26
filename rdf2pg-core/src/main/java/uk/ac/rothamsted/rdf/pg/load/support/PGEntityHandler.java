package uk.ac.rothamsted.rdf.pg.load.support;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManager;


/**
 * <p>A property graph creation handler, which of instances are used by {@link PGLoadingProcessor} instances
 * to do the job of loading/creating/exporting/materialising/etc a property graph in its target form.</p>
 * 
 * <p>This has a few common functions and class fields.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 * 
 * Modifified by cbobed for refactoring purposes  
 * <dl><dt>Date:</dt><dd>28 Apr 2020</dd></dl>
 */
public abstract class PGEntityHandler<T> implements Consumer<Set<T>> 
{
	protected RdfDataManager rdfDataManager;
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );

	/** Used to make thread names */
	private static final AtomicLong threadId = new AtomicLong ( 0 );

	/** Used to make thread names */
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern ( "YYMMdd-HHmmss" ); 
	
	public PGEntityHandler ()
	{
		super(); 
	}
	
	public PGEntityHandler (RdfDataManager rdfDataManager)
	{
		super(); 
		this.rdfDataManager = rdfDataManager;
	}
	
	/**
	 * Changes the thread name with a timestamp marker. This can be used internally, to ease logging and alike
	 * reporting. 
	 */
	protected void renameThread ( String prefix )
	{
		Thread.currentThread ().setName ( 
			prefix + LocalDateTime.now ().format ( TIMESTAMP_FORMATTER ) + ' ' + threadId.getAndIncrement () 
		);
	}
	
	/**
	 * This is used to manage operations with the RDF data source. We don't care about closing this, the invoker
	 * has to do it. 
	 */
	public RdfDataManager getRdfDataManager ()
	{
		return rdfDataManager;
	}

	@Autowired
	public void setRdfDataManager ( RdfDataManager rdfDataManager )
	{
		this.rdfDataManager = rdfDataManager;
	}

}