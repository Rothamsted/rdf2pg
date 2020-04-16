package uk.ac.rothamsted.rdf.neo4j.load.support.graphml;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.rothamsted.rdf.neo4j.load.support.RdfDataManager;

/**
 * <p>A GraphML loading handler, which of instances are used by {@link CyLoadingProcessor} instances.</p>
 * 
 * <p>This has a few common functions and class fields.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 *
 */
public abstract class GraphMLLoadingHandler<T> implements Consumer<Set<T>> 
{
	private RdfDataManager rdfDataManager;

	protected Logger log = LoggerFactory.getLogger ( this.getClass () );

	/** Used to make thread names */
	private static final AtomicLong threadId = new AtomicLong ( 0 );

	/** Used to make thread names */
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern ( "YYMMdd-HHmmss" ); 
	
	public GraphMLLoadingHandler ()
	{
		super ();
	}
	
	public GraphMLLoadingHandler ( RdfDataManager rdfDataManager)
	{
		super ();
		this.rdfDataManager = rdfDataManager;	}

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