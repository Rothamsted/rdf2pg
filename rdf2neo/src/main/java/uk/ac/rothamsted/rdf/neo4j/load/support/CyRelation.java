package uk.ac.rothamsted.rdf.neo4j.load.support;

/**
 * Represents a Cypher/Neo4J relation.
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
