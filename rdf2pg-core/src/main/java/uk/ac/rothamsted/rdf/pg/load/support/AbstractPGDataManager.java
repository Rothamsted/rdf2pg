package uk.ac.rothamsted.rdf.pg.load.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.entities.PGEntity;
import uk.ac.rothamsted.rdf.pg.load.support.entities.PGNode;

/**
 * Manages a few common aspects about PG graphs and their generation.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>5 Oct 2020</dd></dl>
 *
 */
@Component
public class AbstractPGDataManager
{
	private String defaultLabel = "Resource";

	/**
	 * <p>Gets the properties in a {@link PGEntity} as a key/value structure.</p>
	 * 
	 * <p>This does some processing:
	 *   <ul>
	 *     <li>safeguards against empty values</li>
	 *     <li>turns multiple values into an array object, which is what the Neo4j driver expects for them</li>
	 *     <li>Adds a the {@link PGEntity#getIri() parameter IRI} as the 'iri' proerty to the result; this is because
	 *     we want always to identify nodes/relations in Neo4j with their original IRI</li>
	 *   </ul>
	 * </p>
	 * 
	 */
	public Map<String, Object> flatPGProperties ( PGEntity cyEnt )
	{
		Map<String, Object> pgProps = new HashMap<> ();
		for ( Entry<String, Set<Object>> attre: cyEnt.getProperties ().entrySet () )
		{
			Set<Object> vals = attre.getValue ();
			if ( vals.isEmpty () ) continue; // shouldn't happen, but just in case
			
			Object cyAttrVal = vals.size () > 1 ? vals.toArray ( new Object [ 0 ] ) : vals.iterator ().next ();
			pgProps.put ( attre.getKey (), cyAttrVal );
		}
		
		pgProps.put ( "iri", cyEnt.getIri () );
		return pgProps;
	}
	
	/**
	 * <p>The node's default label. This is always added to a {@link PGNode}, in addition to 
	 * {@link PGNodeHandler#getLabelsSparql()} and it's always needed for internal operations like tracking the 
	 * PG output nodes that have to be linked to PG relations. 
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

}
