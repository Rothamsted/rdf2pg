package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 *
 */
public abstract class CypherEntity
{

	private String iri;
	private Map<String, Set<Object>> properties = new HashMap<> ();

	
	public CypherEntity ( String iri )
	{
		super ();
		this.iri = iri;
	}


	public String getIri ()
	{
		return iri;
	}

	public void setIri ( String iri )
	{
		this.iri = iri;
	}

	public Map<String, Set<Object>> getProperties ()
	{
		return properties;
	}

	public void setProperties ( Map<String, Set<Object>> properties )
	{
		this.properties = properties;
	}

	public <T> boolean addPropValue ( String name, T value )
	{
		if ( this.properties == null ) this.properties = new HashMap<String, Set<Object>> ();
		Set<Object> values = this.properties.get ( name );
		if ( values == null ) this.properties.put ( name, values = new HashSet<> () );
		return values.add ( value );
	}

	@SuppressWarnings ( "unchecked" )
	public <T> Set<T> getPropValues ( String name )
	{
		return (Set<T>) this.properties.get ( name );
	}

	@SuppressWarnings ( "unchecked" )
	public <T> T getPropValue ( String name )
	{
		Set<T> vals = (Set<T>) this.properties.get ( name );
		if ( vals == null ) return null;
		return vals.isEmpty () ? null : (T) vals.iterator ().next ();
	}

}