package uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support;

import java.util.function.Consumer;
import java.util.function.Function;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.AbstractPGDataManager;

/**
 * A conceptual extension of {@link uk.ac.rothamsted.neo4j.utils.Neo4jDataManager} that adds some RDF import-related
 * utility and Spring annotations. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>10 Apr 2018</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public class Neo4jDataManager extends AbstractPGDataManager
{
	private final uk.ac.rothamsted.neo4j.utils.Neo4jDataManager delegateMgr;
	
	public Neo4jDataManager ( Driver neo4jDriver ) {
		delegateMgr = new uk.ac.rothamsted.neo4j.utils.Neo4jDataManager ( neo4jDriver );
	}

	@Autowired
	public void setNeo4jDriver ( Driver neo4jDriver ) {
		delegateMgr.setNeo4jDriver ( neo4jDriver );
	}

	public <V> V runSession ( Function<Session, V> action )
	{
		return delegateMgr.runSession ( action );
	}

	public void runSessionVoid ( Consumer<Session> action )
	{
		delegateMgr.runSessionVoid ( action );
	}

	public void runCypher ( String cypher, Object... keyVals )
	{
		delegateMgr.runCypher ( cypher, keyVals );
	}

	public void processCypherMatches ( Consumer<Record> action, String cypher, Object... keyVals )
	{
		delegateMgr.processCypherMatches ( action, cypher, keyVals );
	}

	public Driver getNeo4jDriver ()
	{
		return delegateMgr.getNeo4jDriver ();
	}

	public int getMaxRetries ()
	{
		return delegateMgr.getMaxRetries ();
	}

	public void setMaxRetries ( int maxRetries )
	{
		delegateMgr.setMaxRetries ( maxRetries );
	}

	/**
	 * TODO: requires refactoring with interface extraction 
	 */
	public uk.ac.rothamsted.neo4j.utils.Neo4jDataManager getDelegateMgr ()
	{
		return delegateMgr;
	}
	
}
