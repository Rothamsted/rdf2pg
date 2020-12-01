package uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.entities;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a Property Graph node.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>5 Dec 2017</dd></dl>
 *
 * cbobed, generalised from Cypher-specific terminology.  
 * <dl><dt>Date:</dt><dd>29 Apr 2020</dd></dl>
 */
 
public class PGNode extends PGEntity
{
	private Set<String> labels = new HashSet<> ();

	public PGNode ( String iri )
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
