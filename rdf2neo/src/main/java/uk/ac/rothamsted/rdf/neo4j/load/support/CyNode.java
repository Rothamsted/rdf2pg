package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a Cypher/Neo4J node.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>5 Dec 2017</dd></dl>
 *
 */
public class CyNode extends CypherEntity
{
	private Set<String> labels = new HashSet<> ();

	public CyNode ( String iri )
	{
		super ( iri );
	}

	/** Node labels (i.e., types ) */
	public Set<String> getLabels ()
	{
		return labels;
	}

	public void setLabels ( Set<String> labels )
	{
		this.labels = labels;
	}

	public boolean addLabel ( String label ) 
	{
		if ( label == null ) return false;
		if ( this.labels == null ) this.labels = new HashSet<> ();
		return this.labels.add ( label );
	}

}
