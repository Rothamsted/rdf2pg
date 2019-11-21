package uk.ac.rothamsted.rdf.neo4j.load;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
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
public class MultiConfigCyLoader implements CypherLoader, AutoCloseable
{
	private List<ConfigItem> configItems = new LinkedList<> ();
	private ObjectFactory<SimpleCyLoader> cypherLoaderFactory;
	
	private ApplicationContext springContext;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	private static Logger slog = LoggerFactory.getLogger ( MultiConfigCyLoader.class );
	
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
		private String indexesSparql;
		
		public ConfigItem () {
		}

		public ConfigItem ( 
			String name, 
			String nodeIrisSparql, String labelsSparql, String nodePropsSparql,
			String relationTypesSparql, String relationPropsSparql,
			String indexesSparql
		)
		{
			this.name = name;
			this.nodeIrisSparql = nodeIrisSparql;
			this.labelsSparql = labelsSparql;
			this.nodePropsSparql = nodePropsSparql;
			this.relationTypesSparql = relationTypesSparql;
			this.relationPropsSparql = relationPropsSparql;
			this.indexesSparql = indexesSparql;
		}
		
		public ConfigItem ( 
			String name, 
			String nodeIrisSparql, String labelsSparql, String nodePropsSparql,
			String relationTypesSparql, String relationPropsSparql
		)
		{
			this ( name, nodeIrisSparql, labelsSparql, nodePropsSparql, relationTypesSparql, relationPropsSparql, null );
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

		/**
		 * @see CypherIndexer#getIndexesSparql(). 
		 */
		public String getIndexesSparql ()
		{
			return indexesSparql;
		}

		public void setIndexesSparql ( String indexesSparql )
		{
			this.indexesSparql = indexesSparql;
		}
	}
	

	/** 
	 * Gets an instance from the Spring application context. The returned instance is bound to the context parameter,
	 * so that {@link MultiConfigCyLoader#close()} can close it.
	 * 
	 * See the XML examples to know how you should configure
	 * beans for this.
	 * 
	 */
	public static MultiConfigCyLoader getSpringInstance ( ApplicationContext beanCtx )
	{
		slog.info ( "Getting Loader configuration from Spring Context" );
		MultiConfigCyLoader mloader = beanCtx.getBean ( MultiConfigCyLoader.class );
		mloader.springContext = beanCtx;
		return mloader;
	}

	/**
	 * Invokes {@link #getSpringInstance(ApplicationContext)} with the context obtained from the XML 
	 * configuration file.
	 * 
	 * The instance returned this way will close the application context when the {@link MultiConfigCyLoader#close()}
	 * method is invoked.
	 *  
	 */
	public static MultiConfigCyLoader getSpringInstance ( String xmlConfigPath )
	{
		slog.info ( "Getting Loader configuration from Spring XML file '{}'", xmlConfigPath );		
		ApplicationContext ctx = new FileSystemXmlApplicationContext ( xmlConfigPath );
		return getSpringInstance ( ctx );
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
		for ( int mode = 0; mode <= 2; mode++ )
		{
			for ( ConfigItem cfg: this.getConfigItems () )
			{		
				try ( SimpleCyLoader cypherLoader = this.getCypherLoaderFactory ().getObject (); )
				{
					cypherLoader.setName ( cfg.getName () );
					
					CyNodeLoadingProcessor cyNodeLoader = cypherLoader.getCyNodeLoader ();
					CyRelationLoadingProcessor cyRelLoader = cypherLoader.getCyRelationLoader ();
					
					CyNodeLoadingHandler cyNodehandler = (CyNodeLoadingHandler) cyNodeLoader.getBatchJob ();
					CyRelationLoadingHandler cyRelhandler = (CyRelationLoadingHandler) cyRelLoader.getBatchJob ();
					
					cyNodeLoader.setNodeIrisSparql ( cfg.getNodeIrisSparql () );
		
					cyNodehandler.setLabelsSparql ( cfg.getLabelsSparql () );
					cyNodehandler.setNodePropsSparql ( cfg.getNodePropsSparql () );
					
					cyRelhandler.setRelationTypesSparql ( cfg.getRelationTypesSparql () );
					cyRelhandler.setRelationPropsSparql ( cfg.getRelationPropsSparql () );
		
					String indexesSparql = cfg.getIndexesSparql ();
					if ( indexesSparql != null )
						cypherLoader.getCypherIndexer ().setIndexesSparql ( indexesSparql );
					
					cypherLoader.load ( tdbPath, mode == 0, mode == 1, mode == 2 );
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

	
	@Autowired ( required = false )
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


	/**
	 * This does something effectively if the current loader instance was obtained via one of the 
	 * {@link #getSpringInstance(ApplicationContext)} methods. The corresponding Spring context is closed. If the 
	 * loader was obtained some other way, this method has no effect and you can safely call it, just in case.
	 * 
	 */
	@Override
	public void close ()
	{
		if ( this.springContext == null || ! ( springContext instanceof ConfigurableApplicationContext ) ) return;
		
		ConfigurableApplicationContext cfgCtx = (ConfigurableApplicationContext) springContext;
		cfgCtx.close ();
	}

}
