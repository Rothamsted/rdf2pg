package uk.ac.rothamsted.rdf.pg.load;

/**
 * Represents a configuration to be used for the conversion of a single node or relation type
 * to a property graph.
 * 
 * This was extracted from {@link MultiConfigPGLoader} and originally it was
 * Cypher-specific.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>20 Jan 2018</dd></dl>
 * 
 * TODO: Better new name.
 *
 */
public class ConfigItem
{
	private String name;
	
	private String nodeIrisSparql, labelsSparql, nodePropsSparql;
	private String relationTypesSparql, relationPropsSparql;
	
	public ConfigItem () {}


		
	/**
	 * @see SimpleCyLoader#getName().
	 */
	public String getName () {
		return name;
	}
	public void setName ( String name ) {
		this.name = name;
	}
	
	/**
	 * @see CyNodeLoadingProcessor#getNodeIrisSparql(). 
	 */
	public String getNodeIrisSparql () {
		return nodeIrisSparql;
	}
	public void setNodeIrisSparql ( String nodeIrisSparql ) {
		this.nodeIrisSparql = nodeIrisSparql;
	}

	/**
	 * @see CyNodeLoadingHandler#getLabelsSparql().
	 */
	public String getLabelsSparql () {
		return labelsSparql;
	}
	public void setLabelsSparql ( String labelsSparql ) {
		this.labelsSparql = labelsSparql;
	}
	
	/**
	 * @see CyNodeLoadingHandler#getNodePropsSparql(). 
	 */
	public String getNodePropsSparql () {
		return nodePropsSparql;
	}
	public void setNodePropsSparql ( String nodePropsSparql ) {
		this.nodePropsSparql = nodePropsSparql;
	}
	
	/**
	 * @see CyRelationLoadingHandler#getRelationTypesSparql(). 
	 */
	public String getRelationTypesSparql () {
		return relationTypesSparql;
	}
	public void setRelationTypesSparql ( String relationTypesSparql ) {
		this.relationTypesSparql = relationTypesSparql;
	}
	
	/**
	 * @see CyRelationLoadingHandler#getRelationPropsSparql().
	 */
	public String getRelationPropsSparql () {
		return relationPropsSparql;
	}
	public void setRelationPropsSparql ( String relationPropsSparql ) {
		this.relationPropsSparql = relationPropsSparql;
	}

}
