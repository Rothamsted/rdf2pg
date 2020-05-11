package uk.ac.rothamsted.rdf.pg.load.support;

import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * A generic {@link PGEntityHandler} to manage the creation of a property graph node.
 *
 * @author cbobed
 *         <dl>
 *         <dt>Date:</dt>
 *         <dd>11 May 2020</dd>
 *         </dl>
 *
 */
public abstract class PGNodeHandler extends PGEntityHandler<Resource>
{
	private String labelsSparql, nodePropsSparql;

	/**
	 * This is a query that must returns the variable ?label and contains the variable ?iri, which is bound to a node's
	 * IRI, to fetch its labels. It must return distinct results (we obviously don't care if you don't use the DISTINCT
	 * SPARQL clause).
	 */
	public String getLabelsSparql ()
	{
		return labelsSparql;
	}

	@Autowired ( required = false )
	@Qualifier ( "labelsSparql" )
	public void setLabelsSparql ( String labelsSparql )
	{
		this.labelsSparql = labelsSparql;
	}

	/**
	 * This is a query that must returns the variables ?name and ?value and must contain the variable ?iri, which is
	 * bound to a node's IRI, to fetch its property. Similarly {@link #getLabelsSparql()}, it must return distinct
	 * results.
	 */
	public String getNodePropsSparql ()
	{
		return nodePropsSparql;
	}

	@Autowired ( required = false )
	@Qualifier ( "nodePropsSparql" )
	public void setNodePropsSparql ( String nodePropsSparql )
	{
		this.nodePropsSparql = nodePropsSparql;
	}
}
