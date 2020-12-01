package uk.ac.rothamsted.kg.rdf2pg.load.support.entities;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Subclasses of this represents property graph components, i.e., nodes and relations.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 * 
 * cbobed, generalised from Cypher-specific terminology.  
 * <dl><dt>Date:</dt><dd>29 Apr 2020</dd></dl>
 */
public abstract class PGEntity
{

	private String iri;
	private Map<String, Set<Object>> properties = new HashMap<> ();

	
	public PGEntity ( String iri )
	{
		super ();
		this.iri = iri;
	}

	/** 
	 * For us, every entity must have an IRI/URI identifier.
	 */
	public String getIri ()
	{
		return iri;
	}

	public void setIri ( String iri )
	{
		this.iri = iri;
	}

	/** 
	 * Both nodes and relations have properties. For the moment, a property is a name and a set of values. The latter
	 * is translated to an array. We don't support ordered and duplicated values, since this doesn't exist in RDF.  
	 */
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