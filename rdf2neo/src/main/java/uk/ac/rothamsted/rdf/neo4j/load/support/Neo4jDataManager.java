package uk.ac.rothamsted.rdf.neo4j.load.support;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.Map.Entry;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.runcontrol.MultipleAttemptsExecutor;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>26 Feb 2018</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class Neo4jDataManager implements AutoCloseable
{
	private Driver neo4jDriver;

	private String defaultLabel = "Resource";

	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	
	
	public Neo4jDataManager ( Driver neo4jDriver )
	{
		super ();
		this.neo4jDriver = neo4jDriver;
	}

	/**
	 * <p>Gets the properties in a {@link CypherEntity} as a key/value structure.</p>
	 * 
	 * <p>This does some processing:
	 *   <ul>
	 *     <li>safeguards against empty values</li>
	 *     <li>turns multiple values into an array object, which is what the Neo4j driver expects for them</li>
	 *     <li>Adds a the {@link CypherEntity#getIri() parameter IRI} as the 'iri' proerty to the result; this is because
	 *     we want always to identify nodes/relations in Neo4j with their original IRI</li>
	 *   </ul>
	 * </p>
	 * 
	 */
	public Map<String, Object> getCypherProperties ( CypherEntity cyEnt )
	{
		Map<String, Object> cyProps = new HashMap<> ();
		for ( Entry<String, Set<Object>> attre: cyEnt.getProperties ().entrySet () )
		{
			Set<Object> vals = attre.getValue ();
			if ( vals.isEmpty () ) continue; // shouldn't happen, but just in case
			
			Object cyAttrVal = vals.size () > 1 ? vals.toArray ( new Object [ 0 ] ) : vals.iterator ().next ();
			cyProps.put ( attre.getKey (), cyAttrVal );
		}
		
		cyProps.put ( "iri", cyEnt.getIri () );
		return cyProps;
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

	@Autowired
	public void setNeo4jDriver ( Driver neo4jDriver )
	{
		this.neo4jDriver = neo4jDriver;
	}

	/**
	 * <p>The node's default label. This has to be configured for both {@link CyNodeLoadingHandler} and
	 * {@link CyRelationLoadingHandler} and it is a default Cypher label that is set for each node, in addition
	 * to possible further labels, provided via {@link CyNodeLoadingHandler#getLabelsSparql()}.</p>
	 * 
	 * <p>A default label is a practical way to find nodes in components like {@link CyRelationLoadingHandler}.</p>
	 */
	public String getDefaultLabel ()
	{
		return defaultLabel;
	}

	@Autowired ( required = false )	@Qualifier ( "defaultNodeLabel" )
	public void setDefaultLabel ( String defaultLabel )
	{
		this.defaultLabel = defaultLabel;
	}

	@Override
	public void close ()
	{
		Driver neoDriver = this.getNeo4jDriver ();
		if ( neoDriver != null ) neoDriver.close ();
				
		log.debug ( "Cypher loading handler {} closed", this.getClass ().getSimpleName () );
	}	
}
