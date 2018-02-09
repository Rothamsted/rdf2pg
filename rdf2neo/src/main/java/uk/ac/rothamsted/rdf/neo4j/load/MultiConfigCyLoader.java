package uk.ac.rothamsted.rdf.neo4j.load;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.neo4j.load.support.CyNodeLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyRelationLoadingHandler;
import uk.ac.rothamsted.rdf.neo4j.load.support.CyRelationLoadingProcessor;

/**
 * <H1>The multi-configuration Cypher/Neo4J loader.</H1>
 *
 * <p>This uses multiple {@link ConfigItem SPARQL query configurations} to run {@link SimpleCyLoader} multiple times.
 * This allows for logically separate items in an RDF data set to be mapped separately, each with its own set of SPARQL
 * queries.</p> 
 *
 * <p>Note also that everything is designed to support configuration via Spring Bean files.</p>
 *
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Jan 2018</dd></dl>
 *
 */
@Component
public class MultiConfigCyLoader implements CypherLoader
{
	private List<ConfigItem> configItems = new LinkedList<> ();
	private ObjectFactory<SimpleCyLoader> cypherLoaderFactory;
	
	/**
	 * Represents the RDF/Cypher configuration for a single node/relation type.
	 *
	 * @author brandizi
	 * <dl><dt>Date:</dt><dd>20 Jan 2018</dd></dl>
	 *
	 */
	public static class ConfigItem
	{
		private String name;
		
		private String nodeIrisSparql, labelsSparql, nodePropsSparql;
		private String relationTypesSparql, relationPropsSparql;
		
		public ConfigItem () {
		}

		public ConfigItem ( 
			String name, 
			String nodeIrisSparql, String labelsSparql, String nodePropsSparql,
			String relationTypesSparql, String relationPropsSparql 
		)
		{
			this.name = name;
			this.nodeIrisSparql = nodeIrisSparql;
			this.labelsSparql = labelsSparql;
			this.nodePropsSparql = nodePropsSparql;
			this.relationTypesSparql = relationTypesSparql;
			this.relationPropsSparql = relationPropsSparql;
		}
		
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
	
	/** 
	 * Loops through {@link #getConfigItems() config items} and instantiates a {@link #getCypherLoaderFactory() new simple loader}
	 * for eache item, to load nodes/relations mapped by the config item.
	 */
	@Override
	public void load ( String tdbPath, Object... opts )
	{
		// First the nodes ( mode = 0 ) and then the relations ( mode = 1 )
		// That ensures that cross-references made by different queries are taken 
		// 
		for ( int mode = 0; mode <= 1; mode++ )
		{
			for ( ConfigItem cfg: this.getConfigItems () )
			{		
				try ( SimpleCyLoader cypherLoader = this.getCypherLoaderFactory ().getObject (); )
				{
					cypherLoader.setName ( cfg.getName () );
					
					CyNodeLoadingProcessor cyNodeLoader = cypherLoader.getCyNodeLoader ();
					CyRelationLoadingProcessor cyRelLoader = cypherLoader.getCyRelationLoader ();
					
					CyNodeLoadingHandler cyNodehandler = (CyNodeLoadingHandler) cyNodeLoader.getConsumer ();
					CyRelationLoadingHandler cyRelhandler = (CyRelationLoadingHandler) cyRelLoader.getConsumer ();
					
					cyNodeLoader.setNodeIrisSparql ( cfg.getNodeIrisSparql () );
		
					cyNodehandler.setLabelsSparql ( cfg.getLabelsSparql () );
					cyNodehandler.setNodePropsSparql ( cfg.getNodePropsSparql () );
					
					cyRelhandler.setRelationTypesSparql ( cfg.getRelationTypesSparql () );
					cyRelhandler.setRelationPropsSparql ( cfg.getRelationPropsSparql () );
		
					cypherLoader.load ( tdbPath, mode == 0, mode == 1 );
				} // try				
			} // for config items
		} // for mode
	}

	/**
	 * A single configuration defines how Cypher nodes and relations are mapped from RDF.
	 * 
	 * @see {@link #load(String, Object...)} 
	 */
	public List<ConfigItem> getConfigItems ()
	{
		return configItems;
	}

	
	@Autowired ( required = false ) // @Qualifier ( "loaderConfig" )
	public void setConfigItems ( List<ConfigItem> configItems )
	{
		this.configItems = configItems;
	}

	/**
	 * This is used to get a new {@link SimpleCyLoader} to be used with a new configuration while iterating over
	 * {@link #getConfigItems()}. This is designed this way in order to make it possible to configure/autowire
	 * a factory via Spring.
	 * 
	 */
	public ObjectFactory<SimpleCyLoader> getCypherLoaderFactory ()
	{
		return cypherLoaderFactory;
	}

	@Resource ( name = "simpleCyLoaderFactory" )
	public void setCypherLoaderFactory ( ObjectFactory<SimpleCyLoader> cypherLoaderFactory )
	{
		this.cypherLoaderFactory = cypherLoaderFactory;
	}

}
