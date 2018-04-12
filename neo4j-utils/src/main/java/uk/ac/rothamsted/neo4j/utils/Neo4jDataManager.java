package uk.ac.rothamsted.neo4j.utils;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.function.Consumer;

import org.apache.commons.lang3.ArrayUtils;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.exceptions.DatabaseException;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;
import org.neo4j.driver.v1.exceptions.TransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.utils.runcontrol.MultipleAttemptsExecutor;

/**
 * Wrapper to manage access to Cypher and Neo4j.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>26 Feb 2018</dd></dl>
 *
 */
public class Neo4jDataManager implements AutoCloseable
{
	private Driver neo4jDriver;

	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	
	
	public Neo4jDataManager ( Driver neo4jDriver )
	{
		super ();
		this.neo4jDriver = neo4jDriver;
	}

	
	/**
	 * <p>Runs a Cypher gommands against the current {@link #getNeo4jDriver()}.</p>
	 * 
	 * <p>The keyVals parameter is passed to {@link Values#parameters(Object...)}.</p>
	 * 
	 * <p>The command is wrapped into a single transaction, which is committed within the method.</p>
	 * 
	 * <p>Because parallelism sometimes raises exceptions about race conditions, we use {@link MultipleAttemptsExecutor}
	 * to re-attempt the command execution a couple of times, after such exceptions.</p>
	 */
	public void runCypher ( String cypher, Object... keyVals )
	{
		if ( log.isTraceEnabled () )
			log.trace ( "Cypher: {} params: {}", cypher, ArrayUtils.toString ( keyVals ) );
		
		// Re-attempt a couple of times, in case of exceptions due to deadlocks over locking nodes.
		MultipleAttemptsExecutor attempter = new MultipleAttemptsExecutor (
			TransientException.class,
			DatabaseException.class,
			ServiceUnavailableException.class
		);
		attempter.setMaxAttempts ( 10 );
		attempter.setMinPauseTime ( 30 * 1000 );
		attempter.setMaxPauseTime ( 3 * 60 * 1000 );
		
		attempter.execute ( () -> 
		{
			try ( Session session = this.neo4jDriver.session () ) {
				session.run ( cypher, parameters ( keyVals ) );
			}
		});
	}
	
	/**
	 * Gets {@link Record} instances from the 'cypher' command and, for each instance, runs the action, which 
	 * is supposed to do something with a record.
	 * 
	 * keyVals are parameters for the Cypher query. 
	 *  
	 * Works like {@link #runCypher(String, Object...)} for what concerns transaction and multiple attempts 
	 * wrapping.
	 * 
	 */
	public void processCypherMatches ( Consumer<Record> action, String cypher, Object... keyVals )
	{
		if ( log.isTraceEnabled () )
			log.trace ( "Cypher: {} params: {}", cypher, ArrayUtils.toString ( keyVals ) );
		
		// Re-attempt a couple of times, in case of exceptions due to deadlocks over locking nodes.
		MultipleAttemptsExecutor attempter = new MultipleAttemptsExecutor (
			TransientException.class,
			DatabaseException.class,
			ServiceUnavailableException.class
		);
		attempter.setMaxAttempts ( 10 );
		attempter.setMinPauseTime ( 30 * 1000 );
		attempter.setMaxPauseTime ( 3 * 60 * 1000 );
		
		attempter.execute ( () -> 
		{
			try ( Session session = this.neo4jDriver.session () ) 
			{
				StatementResult cursor = session.run ( cypher, parameters ( keyVals ) );
				cursor.forEachRemaining ( action );
			}
		});
	}
	
	

	/**
	 * The driver and target Neo4j destination used to send Cypher elements mapped from RDF.
	 */
	public Driver getNeo4jDriver ()
	{
		return neo4jDriver;
	}

	public void setNeo4jDriver ( Driver neo4jDriver )
	{
		this.neo4jDriver = neo4jDriver;
	}


	@Override
	public void close ()
	{
		Driver neoDriver = this.getNeo4jDriver ();
		if ( neoDriver != null ) neoDriver.close ();
				
		log.debug ( "Cypher loading handler {} closed", this.getClass ().getSimpleName () );
	}	
}
