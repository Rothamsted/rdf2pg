package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>5 Dec 2017</dd></dl>
 *
 */
public class Node extends CypherEntity
{
	private Set<String> labels = new HashSet<> ();

	public Node ( String iri )
	{
		super ( iri );
	}

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
		if ( this.labels == null ) this.labels = new HashSet<> ();
		return this.labels.add ( label );
	}

}
