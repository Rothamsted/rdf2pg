package uk.ac.rothamsted.kg.rdf2pg.load;

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

import uk.ac.rothamsted.kg.rdf2pg.load.spring.SimplePGLoaderFactory;


/**
 * <H1>The multi-configuration property graph generator.</H1>
 *
 * <p>This uses multiple {@link ConfigItem SPARQL query configurations} to run {@link AbstractSimplePGLoader} multiple times.
 * This allows for logically separate items in an RDF data set to be mapped separately, each with its own set of SPARQL
 * queries.</p> 
 *
 * <p>Note also that everything is designed to support configuration via Spring Bean files.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Jan 2018</dd></dl>
 *
 * Modifified by cbobed for refactoring purposes  
 * <dl><dt>Date:</dt><dd>28 Apr 2020</dd></dl>
 */
public abstract class MultiConfigPGLoader<CI extends ConfigItem<SL>, SL extends SimplePGLoader>
  implements PropertyGraphLoader, AutoCloseable
{
	private List<CI> configItems = new LinkedList<> ();
	private ObjectFactory<SL> pgLoaderFactory;
	
	private ApplicationContext springContext;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	private static Logger slog = LoggerFactory.getLogger ( MultiConfigPGLoader.class );

	
	/** 
	 * Gets an instance from the Spring application context. The returned instance is bound to the context parameter,
	 * so that {@link MultiConfigPGLoader#close()} can close it.
	 * 
	 * See the XML examples to know how you should configure
	 * beans for this.
	 * 
	 */
	public static <ML extends MultiConfigPGLoader<?, ?>> ML getSpringInstance ( 
		ApplicationContext beanCtx, Class<? extends ML> loaderClass
	)
	{
		slog.info ( "Getting Loader configuration from Spring Context" );
		ML mloader = beanCtx.getBean ( loaderClass );
		mloader.setSpringContext ( beanCtx );
		return mloader;
	}

	/**
	 * Invokes {@link #getSpringInstance(ApplicationContext)} with the context obtained from the XML 
	 * configuration file.
	 * 
	 * The instance returned this way will close the application context when the {@link MultiConfigPGLoader#close()}
	 * method is invoked.
	 *  
	 */
	public static <ML extends MultiConfigPGLoader<?, ?>> ML getSpringInstance ( String xmlConfigPath, Class<? extends ML> loaderClass )
	{
		slog.info ( "Getting Loader configuration from Spring XML file '{}'", xmlConfigPath );		
		ApplicationContext ctx = new FileSystemXmlApplicationContext ( xmlConfigPath );
		return getSpringInstance ( ctx, loaderClass );
	}
	
	
	
	protected ApplicationContext getSpringContext ()
	{
		return springContext;
	}

	protected void setSpringContext ( ApplicationContext springContext )
	{
		this.springContext = springContext;
	}

	@Override
	public void load ( String tdbPath, Object... opts )
	{
		this.loadBegin ( tdbPath, opts );
		
		// First the nodes ( mode = 0 ) and then the relations ( mode = 1 )
		// That ensures that cross-references made by different queries are taken  
		for ( int mode = 0; mode <= 2; mode++ )
			for ( CI cfg: this.getConfigItems () )
				this.loadIteration ( mode, cfg, tdbPath, opts );
		
		this.loadEnd ( tdbPath, opts );
	}
	
	/**
	 * Just logs that it's beginning with the current loader.
	 */
	protected void loadBegin ( String tdbPath, Object... opts )
	{
		log.info ( "Using {} exporter", this.getClass ().getSimpleName () );
	}
	
	protected void loadIteration ( int mode, CI cfg, String tdbPath, Object... opts )
	{
		try ( SL pgLoader = this.getPGLoaderFactory ().getObject (); )
		{
			cfg.configureLoader ( pgLoader );
			pgLoader.load ( tdbPath, mode == 0, mode == 1 );
		}
	}
	
	protected void loadEnd ( String tdbPath, Object... opts )
	{
		// Defaults to null
	}
	
	
	
	/**
	 * A single configuration defines how Cypher nodes and relations are mapped from RDF.
	 * 
	 * @see {@link #load(String, Object...)} 
	 */
	public List<CI> getConfigItems ()
	{
		return configItems;
	}

	
	@Autowired ( required = false )
	public void setConfigItems ( List<CI> configItems )
	{
		this.configItems = configItems;
	}

	/**
	 * This is used to get a new {@link SimplePGLoader} to be used with a new configuration while iterating over
	 * {@link #getConfigItems()}. This is designed this way in order to make it possible to configure/autowire
	 * a factory via Spring.
	 * 
	 */
	public ObjectFactory<SL> getPGLoaderFactory ()
	{
		return pgLoaderFactory;
	}

	@Resource ( type = SimplePGLoaderFactory.class )
	public void setPGLoaderFactory ( ObjectFactory<SL> loaderFactory )
	{
		this.pgLoaderFactory = loaderFactory;
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
