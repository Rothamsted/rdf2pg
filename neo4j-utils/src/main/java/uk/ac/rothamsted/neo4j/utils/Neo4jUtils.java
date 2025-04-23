package uk.ac.rothamsted.neo4j.utils;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.reactivestreams.ReactiveTransactionContext;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.ac.ebi.utils.collections.PaginationIterator;
import uk.ac.ebi.utils.exceptions.ExceptionUtils;

/**
 * Utilities to work with the Project Reactor integration into Neo4j.
 *
 * TODO: write tests and examples.
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>29 Jun 2024</dd></dl>
 *
 */
public class Neo4jUtils
{
	private static Logger log = LoggerFactory.getLogger ( Neo4jUtils.class );
	
	private Neo4jUtils () {}


	/**
	 * Helper to process a Neo4j read query in a reactive style.
	 * 
	 * In a nutshell, prepares a {@link Flux} that publishes each record
	 * returned by the query in the callback.
	 * 
	 * This is a template based on the approach described
	 * 
	 * <a href = "https://neo4j.com/docs/java-manual/current/reactive">here</a>
	 * <a href = "https://graphaware.com/neo4j/2021/01/14/reactive-data-copy.html">here</>.
	 * 
	 * @param callBack this is passed to 
	 * {@link ReactiveSession#executeRead(org.neo4j.driver.reactivestreams.ReactiveTransactionCallback)}
	 * and it's where you should run your Cypher query and produce a {@link ReactiveResult}, from which 
	 * we do downstream processing. Typically, this is done via {@link ReactiveTransactionContext#run(String)}
	 * and its variants (see the examples above)
	 * 
	 * @param neoDriver obviously, you need a Neo4j driver to talk to.
	 * 
	 * @return a reactive {@link Flux} of {@link Record}.
	 */
	public static Flux<Record> reactiveRead ( 
		Function<ReactiveTransactionContext, Publisher<ReactiveResult>> callBack,
		Driver neoDriver )
	{
		return Flux.usingWhen (
			// The reactive session is generated when a subscriber comes...
			Mono.fromSupplier ( () -> neoDriver.session ( ReactiveSession.class ) ),
			
			// ...and it's used in the closure, to spawn a Flux of Rs
			rsession -> rsession.executeRead ( tx ->
			  // This yields a ReactiveResult
				Mono.fromDirect ( callBack.apply ( tx ) )
				// which is mapped onto its records
				.flatMapMany ( ReactiveResult::records )
			), // executeRead(), usingWhen(), closure publisher
			
			ReactiveSession::close, // usingWhen(), flux cleanup in case of completion
			
			// usingWhen(), flux cleanup in case of error
			(rsession, ex) -> {
				throw ExceptionUtils.buildEx ( 
					ClientException.class, ex, "Error while running reactive Neo4j query: $cause"
				);
			},
				
			// usingWhen(), flux cleanup in case of cancelling
			rsession -> {
				log.debug ( "Neo4j reactive query cancelled" );
				return Flux.empty ();
			}
			
		); // usingWhen ()
	
	} // static reactiveRead()
	
	
	/**
	 * An helper to deal with the pagination of read-only queries.
	 * 
	 * This is an {@link Iterator} based on a Cypher query that has OFFSET/LIMIT clauses.
	 * This is based on {@link PaginationIterator}.
	 * 
	 * @param callBack The Cypher query from which to get a page result. This is run 
	 * through {@link #reactiveRead(Function, Driver)}. This receives the current result offset 
	 * as second parameter, so that the query can return the publisher for the next page or null.
	 * 
	 * @param neoDriver
	 * 
	 * @param pageSize The iterator buffers results in memory, with a buffer having the same size as pageSize, 
	 * so take this into account.
	 */
	public static Iterator<Record> paginatedRead ( BiFunction<ReactiveTransactionContext, Long, Publisher<ReactiveResult>> callBack, Driver neoDriver, Long pageSize )
	{
		if ( pageSize == null ) pageSize = 2500L; // From past experience

		// As explained in PaginationIterator, here we can return the page elements iterator,
		// or null when the current page has not elements anymore.
		Function<Long, Iterator<Record>> pageSelector = offset ->
		{
		  Iterator<Record> pageItr = reactiveRead ( tx -> callBack.apply (tx, offset), neoDriver )
		    .toIterable ()
		    .iterator ();
		  
		  return pageItr.hasNext () ? pageItr : null;
		};
				
		return new PaginationIterator<> ( 
			pageSelector, pageSize, Function.identity () 
		);		
	}

	/**
	 * Default page size.
	 */
	public Iterator<Record> paginatedRead ( BiFunction<ReactiveTransactionContext, Long, Publisher<ReactiveResult>> callBack, Driver neoDriver )
	{
		return paginatedRead ( callBack, neoDriver, null );
	}	
}
