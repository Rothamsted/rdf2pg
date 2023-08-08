package uk.ac.rothamsted.neo4j.utils;


import static org.neo4j.driver.Values.parameters;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.TransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.utils.runcontrol.MultipleAttemptsExecutor;
import uk.org.lidalia.slf4jext.Level;

/**
 * Wrapper to manage access to Cypher and Neo4j.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>26 Feb 2018</dd></dl>
 *
 */
public class Neo4jDataManager
{
	private Driver neo4jDriver;
	private int maxRetries = 10;
	private Level attemptMsgLogLevel = Level.DEBUG;

	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	public Neo4jDataManager ( Driver neo4jDriver )
	{
		super ();
		this.neo4jDriver = neo4jDriver;
	}

	/**
	 * <p>Runs a Neo4j client session, which is created and given to the action as a parameter. The action can return
	 * a value useful for the invoker of this method.</p> 
	 * 
	 * <p>Because parallelism sometimes raises exceptions about race conditions, we use {@link MultipleAttemptsExecutor}
	 * to re-attempt the command execution a couple of times, after such exceptions.</p>
	 * 
	 */
	@SuppressWarnings ( "unchecked" )
	public <V> V runSession ( Function<Session, V> action )
	{
		MultipleAttemptsExecutor attempter = new MultipleAttemptsExecutor (
			TransientException.class,
			DatabaseException.class,
			ServiceUnavailableException.class
		);
		attempter.setMaxAttempts ( this.getMaxRetries () );
		attempter.setMinPauseTime ( 3 * 1000 );
		attempter.setMaxPauseTime ( 7 * 1000 );
		attempter.setAttemptMsgLogLevel ( this.attemptMsgLogLevel );
		
		Object[] result = new Object [ 1 ];
		attempter.execute ( () -> 
		{
			try ( Session session = this.neo4jDriver.session () ) {
				result [ 0 ] = action.apply ( session );
			}
		});
		return (V) result [ 0 ];
	}

	/**
	 * A convenience wrapper of {@link #runSession(Function)} that doesn't force the action executor to return a value, 
	 * if that's not expected by the invoker of this method.
	 * 
	 */
	public void runSessionVoid ( Consumer<Session> action ) {
		// TODO: Java commons collections 4.x
		runSession ( session -> { action.accept ( session ); return null; } );
	}
	
	
	/**
	 * <p>Runs a Cypher commands against the current {@link #getNeo4jDriver()}.</p>
	 * 
	 * <p>The keyVals parameter is passed to {@link Values#parameters(Object...)}.</p>
	 * 
	 * <p>The command is wrapped into a single transaction, which is committed within the method. This also
	 * means the command is wrapped in a Neo4j session, using {@link #runSession(Function)} and that 
	 * is affected by {@link #getMaxRetries()}.</p>
	 * 
	 */
	public void runCypher ( String cypher, Object... keyVals )
	{
		if ( log.isTraceEnabled () )
			log.trace ( "Cypher: {} params: {}", cypher, ArrayUtils.toString ( keyVals ) );

		this.runSession ( session -> session.run ( cypher, parameters ( keyVals ) ) );	
	}
	
	
	
	
	/**
	 * Gets {@link Record} instances from the 'cypher' command and, for each instance, runs the action, which 
	 * is supposed to do something with a record.
	 * 
	 * keyVals are parameters for the Cypher query. 
	 *  
	 * <p>The command is wrapped into a single transaction, which is committed within the method. This also
	 * means the command is wrapped in a Neo4j session, using {@link #runSession(Function)} and that 
	 * is affected by {@link #getMaxRetries()}.</p>
	 * 
	 */
	public void processCypherMatches ( Consumer<Record> action, String cypher, Object... keyVals )
	{
		if ( log.isTraceEnabled () )
			log.trace ( "Cypher: {} params: {}", cypher, ArrayUtils.toString ( keyVals ) );
		
		this.runSessionVoid ( session -> {
			Result cursor = session.run ( cypher, parameters ( keyVals ) );
			cursor.forEachRemaining ( action );			
		});
	}
	
	

	/**
	 * The driver used by this manager for its own operations, hence, this is also the target DB it operates
	 * against. We don't deal with closing this driver instance, so the caller has to do it.
	 */
	public Driver getNeo4jDriver ()
	{
		return neo4jDriver;
	}

	public void setNeo4jDriver ( Driver neo4jDriver )
	{
		this.neo4jDriver = neo4jDriver;
	}

	/**
	 * {@link #runSession(Function)} tries Cypher queries multiple times, until they run successfully, or this no. of 
	 * attempts is reached. This is useful in case of parallel writing threads/processes, where it might happen
	 * that exceptions due to concurrent access to the database server can be recovered by simply retrying after a 
	 * random time.
	 * 
	 * All the Cypher-running methods in this class are affected.
	 * 
	 * Default is 10, must be at least 1.
	 * 
	 * This bean property is a wrapper or {@link MultipleAttemptsExecutor#getMaxAttempts()}
	 * 
	 * @see #getAttemptMsgLogLevel()
	 * 
	 */
	public int getMaxRetries ()
	{
		return maxRetries;
	}

	/**
	 * @throws IllegalArgumentException if it's &lt;1
	 */
	public void setMaxRetries ( int maxRetries )
	{
		if ( maxRetries < 1 ) throw new IllegalArgumentException ( 
			"maxRetries property for Neo4jDataManager must be 1 at least" 
		);
		this.maxRetries = maxRetries;
	}

	/**
	 * A delegate of {@link MultipleAttemptsExecutor#setAttemptMsgLogLevel(Level)}, allows for setting
	 * the log level of the message notifying that {@link #runSession(Function)} is being re-attempted due to problems
	 * like deadlocked transactions.
	 * 
	 * The default is {@link Level#DEBUG}.
	 * 
	 * @see #getMaxRetries()
	 */
	public Level getAttemptMsgLogLevel ()
	{
		return attemptMsgLogLevel;
	}

	public void setAttemptMsgLogLevel ( Level attemptMsgLogLevel )
	{
		this.attemptMsgLogLevel = attemptMsgLogLevel;
	}	
}
