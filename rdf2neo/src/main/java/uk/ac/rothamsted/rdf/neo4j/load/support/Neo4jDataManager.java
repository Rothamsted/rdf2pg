package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.driver.v1.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * An extension of {@link uk.ac.rothamsted.neo4j.utils.Neo4jDataManager} that adds some RDF import-related
 * utility and Spring annotations. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>10 Apr 2018</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class Neo4jDataManager extends uk.ac.rothamsted.neo4j.utils.Neo4jDataManager
{
	private String defaultLabel = "Resource";
	
	public Neo4jDataManager ( Driver neo4jDriver ) {
		super ( neo4jDriver );
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

	/**
	 * Overridden just to add Spring annotations.
	 */
	@Autowired	 @Override
	public void setNeo4jDriver ( Driver neo4jDriver ) {
		super.setNeo4jDriver ( neo4jDriver );
	}
}
