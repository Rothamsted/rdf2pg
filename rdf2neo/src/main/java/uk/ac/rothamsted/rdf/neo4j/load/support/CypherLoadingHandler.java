package uk.ac.rothamsted.rdf.neo4j.load.support;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.jena.rdf.model.Resource;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.exceptions.TransientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.utils.runcontrol.MultipleAttemptsExecutor;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 *
 */
public abstract class CypherLoadingHandler<T> implements Consumer<Long> 
{
	private NeoDataManager dataMgr;
	private Driver neo4jDriver;

	private long sparqlQuerySize = 100000;
	private String defaultLabel = "Resource";

	private long overflowQueryOffset = -1;
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );

	public CypherLoadingHandler ()
	{
		super ();
	}
	
	public CypherLoadingHandler ( NeoDataManager dataMgr, Driver neo4jDriver )
	{
		super ();
		this.dataMgr = dataMgr;
		this.neo4jDriver = neo4jDriver;
	}


	protected Map<String, Object> getCypherProperties ( CypherEntity cyEnt )
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
	
	
	protected void runCypher ( String cypher, Object... keyVals )
	{
		if ( log.isTraceEnabled () )
			log.trace ( "Cypher: {} params: {}", cypher, ArrayUtils.toString ( keyVals ) );
		
		// Re-attempt a couple of times, in case of exceptions due to deadlocks over locking nodes.
		MultipleAttemptsExecutor attempter = new MultipleAttemptsExecutor ( TransientException.class );
		attempter.setMaxAttempts ( 5 );
		attempter.setMinPauseTime ( 20 * 1000 );
		attempter.setMaxPauseTime ( 80 * 1000 );
		
		attempter.execute ( () -> 
		{
			try ( Session session = this.neo4jDriver.session () ) {
				session.run ( cypher, parameters ( keyVals ) );
			}
		});
	}

	public NeoDataManager getDataMgr ()
	{
		return dataMgr;
	}

	public void setDataMgr ( NeoDataManager dataMgr )
	{
		this.dataMgr = dataMgr;
	}

	public Driver getNeo4jDriver ()
	{
		return neo4jDriver;
	}

	public void setNeo4jDriver ( Driver neo4jDriver )
	{
		this.neo4jDriver = neo4jDriver;
	}

	public String getDefaultLabel ()
	{
		return defaultLabel;
	}

	public void setDefaultLabel ( String defaultLabel )
	{
		this.defaultLabel = defaultLabel;
	}

	public long getSparqlQuerySize ()
	{
		return sparqlQuerySize;
	}

	public void setSparqlQuerySize ( long sparqlQuerySize )
	{
		this.sparqlQuerySize = sparqlQuerySize;
	}

	
	public boolean dataFinished ()
	{
		return overflowQueryOffset != -1;
	}

	protected void notifyDataFinished ( long overflowQueryOffset )
	{
		if ( this.dataFinished() && overflowQueryOffset >= this.overflowQueryOffset ) return;
		this.overflowQueryOffset = overflowQueryOffset;
	}

	protected long getOverflowQueryOffset ()
	{
		return overflowQueryOffset;
	}
	
}