package uk.ac.rothamsted.kg.rdf2pg.load.support.entities;

/**
 * Represents a property graph relation.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 *
 * cbobed, generalised from Cypher-specific terminology.  
 * <dl><dt>Date:</dt><dd>29 Apr 2020</dd></dl>
 */
public class PGRelation extends PGEntity
{
	private String type, fromIri, toIri; 
	
	public PGRelation ( String iri ) {
		super ( iri );
	}

	public String getType ()
	{
		return type;
	}

	public void setType ( String type )
	{
		this.type = type;
	}

	/**
	 * We identify the arc's nodes by means of their IRIs/URIs.  
	 */
	public String getFromIri ()
	{
		return fromIri;
	}

	public void setFromIri ( String fromIri )
	{
		this.fromIri = fromIri;
	}

	/**
	 * @see #getFromIri()
	 */
	public String getToIri ()
	{
		return toIri;
	}

	public void setToIri ( String toIri )
	{
		this.toIri = toIri;
	}	
}
