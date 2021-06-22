package uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support;

import static info.marcobrandizi.rdfutils.jena.JenaGraphUtils.JENAUTILS;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import info.marcobrandizi.rdfutils.jena.TDBEndPointHelper;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.SimpleCyLoader;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;

/**
 * Used by {@link SimpleCyLoader} to build Neo4j indices.
 * 
 * The idea is that {@link #getIndexesSparql()} returns a list of {@code <?nodeLabel ?propertyNameToIndex>}. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>26 Feb 2018</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public class CypherIndexer
{
	private Neo4jDataManager neo4jDataManager;
	private RdfDataManager rdfDataManager;
	
	private String indexesSparql;

	private Logger log = LoggerFactory.getLogger ( this.getClass () ); 
	
	public void index ()
	{
		String idxSparql = getIndexesSparql ();
		if ( idxSparql == null ) return;

		log.info ( "Starting Cypher Indexing" );

		RdfDataManager rdfMgr = this.getRdfDataManager ();
		Neo4jDataManager neoMgr = this.getNeo4jDataManager ();
		
		Function<String, String> labelIdConverter = rdfMgr.getPGNodeLabelIdConverter ();
		Function<String, String> propIdConverter = rdfMgr.getPGPropertyIdConverter ();
		
		final List<String> allLabels = new LinkedList<> (), allRelationTypes = new LinkedList<>();
		
		rdfMgr.processSelect ( "CypherIndexer", idxSparql, row -> 
		{  
			String label = rdfMgr.getPGId ( row.get ( "label" ), labelIdConverter );
			if ( label == null ) throw new IllegalArgumentException ( "Null label in the indices query" );
			
			// id converter has to be possibly used later, not for "all nodes|labels"
			String propName = rdfMgr.getPGId ( row.get ( "propertyName" ), null );
			if ( propName == null ) throw new IllegalArgumentException ( String.format (  
				"Null property name in the indices query, for the label %s", label 
			));
			
			var isRelation = JENAUTILS.literal2Boolean ( row.getLiteral ( "isRelation" ) ).orElse ( false );
			
			
			// TODO: document this case (new 4.3 indices on labels or node types)
			//
			
			// *.* label or relation type indexing (available since Neo 4.3
			//
			if ( equalsIgnoreCase ( "*", label ) && equalsIgnoreCase ( "_type_", propName ) )
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
				return;
			}

			// Regular indexing on properties
			//			
			List<String> labels;
			
			if ( "*".equals ( label ) )
			{
				// Get all the labels or types when * is given
				if ( isRelation )
				{
					if ( allRelationTypes.isEmpty () )
						neoMgr.processCypherMatches ( 
							lrec -> allRelationTypes.add ( lrec.get ( 0 ).asString () ), 
							"call db.relationshipTypes();"  
						);
					labels = allRelationTypes;
				}		
				else
				{
					if ( allLabels.isEmpty () )
						neoMgr.processCypherMatches ( 
							lrec -> allLabels.add ( lrec.get ( 0 ).asString () ), 
							"call db.labels();"  
						);
					labels = allLabels;
				}
			}
			else
			{
				// Or, just use the one you found
				labels = List.of ( label );
			}
			
			// And now use the property
			propName = propIdConverter.apply ( propName );
			
			for ( String actualLabel: labels ) {
				log.info ( "Indexing on {} '{}'.'{}'", isRelation ? "relation" : "node", actualLabel, propName );
				var cypherClause = isRelation ? "()-[x:`%s`]-()" : "(x:`%s`)";
				cypherClause = String.format ( cypherClause, actualLabel );
				neo4jDataManager.runCypher ( String.format (
					"CREATE INDEX IF NOT EXISTS FOR %s ON (x.`%s`)",						
					cypherClause, propName 
				));
			}
		}); // processSelect()

		log.info ( "Cypher Indexing Ended" );
		
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
