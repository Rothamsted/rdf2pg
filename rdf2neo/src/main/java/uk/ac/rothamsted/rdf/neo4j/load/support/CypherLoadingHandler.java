package uk.ac.rothamsted.rdf.neo4j.load.support;

import static org.neo4j.driver.v1.Values.parameters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.apache.commons.lang3.ArrayUtils;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.exceptions.DatabaseException;
import org.neo4j.driver.v1.exceptions.ServiceUnavailableException;
import org.neo4j.driver.v1.exceptions.TransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import uk.ac.ebi.utils.runcontrol.MultipleAttemptsExecutor;

/**
 * <p>A Cypher loading handler, which of instances are used by {@link CyLoadingProcessor} instances.</p>
 * 
 * <p>This has a few common functions and class fields.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 *
 */
public abstract class CypherLoadingHandler<T> implements Consumer<Set<T>>, AutoCloseable 
{
	private Neo4jDataManager neo4jDataManager;
	private RdfDataManager rdfDataManager;

	protected Logger log = LoggerFactory.getLogger ( this.getClass () );

	/** Used to make thread names */
	private static final AtomicLong threadId = new AtomicLong ( 0 );

	/** Used to make thread names */
	private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern ( "YYMMdd-HHmmss" ); 
	
	public CypherLoadingHandler ()
	{
		super ();
	}
	
	public CypherLoadingHandler ( RdfDataManager rdfDataManager, Neo4jDataManager neo4jDataManager )
	{
		super ();
		this.rdfDataManager = rdfDataManager;
		this.neo4jDataManager = neo4jDataManager;
	}

	/**
	 * Changes the thread name with a timestamp marker
	 */
	protected void renameThread ( String prefix )
	{
		Thread.currentThread ().setName ( 
			prefix + LocalDateTime.now ().format ( TIMESTAMP_FORMATTER ) + ' ' + threadId.getAndIncrement () 
		);
	}

	public RdfDataManager getRdfDataManager ()
	{
		return rdfDataManager;
	}

	@Autowired
	public void setRdfDataManager ( RdfDataManager rdfDataManager )
	{
		this.rdfDataManager = rdfDataManager;
	}
	
	public Neo4jDataManager getNeo4jDataManager ()
	{
		return neo4jDataManager;
	}

	@Autowired
	public void setNeo4jDataManager ( Neo4jDataManager neo4jDataManager )
	{
		this.neo4jDataManager = neo4jDataManager;
	}

	@Override
	public void close ()
	{
		RdfDataManager rdfMgr = this.getRdfDataManager ();
		if ( rdfMgr != null ) rdfMgr.close ();
		
		Neo4jDataManager neoMgr = this.getNeo4jDataManager ();
		if ( neoMgr != null ) neoMgr.close ();
				
		log.debug ( "Cypher loading handler {} closed", this.getClass ().getSimpleName () );
	}

}