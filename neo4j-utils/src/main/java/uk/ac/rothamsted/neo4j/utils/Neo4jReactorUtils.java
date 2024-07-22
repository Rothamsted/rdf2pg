package uk.ac.rothamsted.neo4j.utils;

import java.util.function.Function;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.ClientException;
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.reactivestreams.ReactiveTransactionContext;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
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
public class Neo4jReactorUtils
{
	private static Logger log = LoggerFactory.getLogger ( Neo4jReactorUtils.class );
	
	/**
	 * Helper to process a Neo4j read query in a reactive style.
	 * 
	 * In a nutshell, prepares a {@link Flux} that pushes an R object for each record
	 * returned by the query in the callback. Records are mapped to R by the recordMapper
	 * function.
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
	 * @param recordMapper We use {@link ReactiveResult#records()} to get the records that the callBack's 
	 * returns and then we map them to R objects. The mapping is based on {@link Mono#flatMapMany(Function)},
	 * so it's a flux-to-flux mapping (again, see the linked examples).
	 * 
	 * @param neoDriver obviously, you need a Neo4j driver to talk to.
	 * 
	 * @return a reactive {@link Flux} of objects, where each object correspond to a Cypher
	 * record, in the way explained above.
	 * 
	 */
	public static <R> Flux<R> reactiveRead ( 
		Function<ReactiveTransactionContext, Publisher<ReactiveResult>> callBack,
		Function<Record, R> recordMapper,
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
				// and then records are mapped by our custom mapper, the result is Flux<R>
				.map ( recordMapper )
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
	
}
