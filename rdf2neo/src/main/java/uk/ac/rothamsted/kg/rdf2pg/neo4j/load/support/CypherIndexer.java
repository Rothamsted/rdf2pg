package uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import info.marcobrandizi.rdfutils.jena.TDBEndPointHelper;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.SimpleCyLoader;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.SimplePGMaker;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGIndexer;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.SimpleTsvIndexer;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;

/**
 * TODO: review comments.
 * 
 * Used by {@link SimpleCyLoader} to build Neo4j indices.
 * 
 * The idea is that {@link #getIndexesSparql()} returns a list of {@code <?nodeLabel ?propertyNameToIndex>}. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>26 Feb 2018</dd></dl>
 *
 * As explained in {@link SimplePGMaker}, this is marked as @Primary, which 
 * means the default {@link SimpleTsvIndexer} is overriden. 
 *
 */
@Component @Primary @Scope ( scopeName = "pgmakerSession" )
public class CypherIndexer extends PGIndexer
{
	private Neo4jDataManager neo4jDataManager;
	private RdfDataManager rdfDataManager;
	
	private String indexesSparql;

	private Logger log = LoggerFactory.getLogger ( this.getClass () ); 
	
	public void index ( List<IndexDef> indexDefinitions )
	{
		if ( indexDefinitions.size () == 0 ) {
			log.warn ( "{}No Cypher index definitions found, no Cypher index will be created", getCompNamePrefix () );
			return;
		}

		log.info ( "{}Starting Cypher Indexing", getCompNamePrefix () );
		
		Neo4jDataManager neoMgr = this.getNeo4jDataManager ();
		
		final List<String> allLabels = new LinkedList<> (), 
				  allRelationTypes = new LinkedList<>();

		for ( IndexDef idxDef: indexDefinitions )
		{
			var type = idxDef.getType ();
			var isRelation = idxDef.isRelation ();
			var propName = idxDef.getPropertyName ();
			
			// *.* label or relation type indexing (available since Neo 4.3)
			//
			if ( equalsIgnoreCase ( "*", type ) && equalsIgnoreCase ( "_type_", propName ) )
			{
				String cypher = null;
				if ( isRelation )
				{
					log.info ( "Indexing on relation types" );
					cypher = "IF NOT EXISTS FOR ()-[r]-() ON EACH type(r)";
				}
				else
				{
					log.info ( "Indexing on node labels" );
					cypher = "IF NOT EXISTS FOR (n) ON EACH labels(n)";
				}
				
				cypher = "CREATE LOOKUP INDEX " + cypher;
				neo4jDataManager.runCypher ( cypher );

				continue;
			}

			// Regular indexing on properties
			//			
			List<String> pgTypes;
			
			if ( "*".equals ( type ) )
			{
				// Get all the labels or types when * is given
				if ( isRelation )
				{
					if ( allRelationTypes.isEmpty () )
						neoMgr.processCypherMatches ( 
							lrec -> allRelationTypes.add ( lrec.get ( 0 ).asString () ), 
							"call db.relationshipTypes();"  
						);
					pgTypes = allRelationTypes;
				}		
				else
				{
					if ( allLabels.isEmpty () )
						neoMgr.processCypherMatches ( 
							lrec -> allLabels.add ( lrec.get ( 0 ).asString () ), 
							"call db.labels();"  
						);
					pgTypes = allLabels;
				}
			}
			else
			{
				// Or, just use the one you found
				pgTypes = List.of ( type );
			}
			
			// And now use the property (which is already converted into the PG format)
			
			for ( String actualType: pgTypes ) {
				log.info ( "Indexing on {} '{}'.'{}'", isRelation ? "relation" : "node", actualType, propName );
				var cypherClause = isRelation ? "()-[x:`%s`]-()" : "(x:`%s`)";
				cypherClause = String.format ( cypherClause, actualType );
				neo4jDataManager.runCypher ( String.format (
					"CREATE INDEX IF NOT EXISTS FOR %s ON (x.`%s`)",						
					cypherClause, propName 
				));
			}			
		} // for indexDefinitions

		log.info ( "{}Cypher Indexing Ended", getCompNamePrefix () );
		
	} // index ()
	
	public Neo4jDataManager getNeo4jDataManager ()
	{
		return neo4jDataManager;
	}

	@Autowired
	public void setNeo4jDataManager ( Neo4jDataManager neo4jDataManager )
	{
		this.neo4jDataManager = neo4jDataManager;
	}
	
	/**
	 * Used internally, to interact with an RDF backend. We don't care about 
	 * {@link TDBEndPointHelper#close() closing} it, so the caller has to do it.
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

	/**
	 * See above.
	 */
	public String getIndexesSparql ()
	{
		return indexesSparql;
	}

	@Autowired ( required = false ) @Qualifier ( "indexesSparql" )
	public void setIndexesSparql ( String indexesSparql )
	{
		this.indexesSparql = indexesSparql;
	}
}
