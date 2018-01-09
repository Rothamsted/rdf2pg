package uk.ac.rothamsted.rdf.neo4j.load.support;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>21 Dec 2017</dd></dl>
 *
 */
public class CyRelation extends CypherEntity
{
	private String type, fromIri, toIri; 
	
	public CyRelation ( String iri ) {
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

	public String getFromIri ()
	{
		return fromIri;
	}

	public void setFromIri ( String fromIri )
	{
		this.fromIri = fromIri;
	}

	public String getToIri ()
	{
		return toIri;
	}

	public void setToIri ( String toIri )
	{
		this.toIri = toIri;
	}	
}
