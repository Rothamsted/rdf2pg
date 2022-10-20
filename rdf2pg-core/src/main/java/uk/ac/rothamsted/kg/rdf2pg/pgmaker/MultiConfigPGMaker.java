package uk.ac.rothamsted.kg.rdf2pg.pgmaker;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import uk.ac.rothamsted.kg.rdf2pg.pgmaker.spring.SimplePGMakerFactory;


/**
 * <H1>The multi-configuration property graph generator.</H1>
 *
 * <p>This uses multiple {@link ConfigItem SPARQL query configurations} to run {@link SimplePGMaker} multiple times.
 * This allows for logically separated items in an RDF data set to be mapped separately, each with its own set of SPARQL
 * queries.</p> 
 *
 * <p>Note also that everything is designed to support configuration via Spring Bean files.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Jan 2018</dd></dl>
 *
 * Modified by cbobed for refactoring purposes  
 * <dl><dt>Date:</dt><dd>28 Apr 2020</dd></dl>
 */
public abstract class MultiConfigPGMaker<CI extends ConfigItem<SM>, SM extends SimplePGMaker<?, ?, ?, ?>>
  implements PropertyGraphMaker, AutoCloseable
{
	private List<CI> configItems = new LinkedList<> ();
	private SimplePGMakerFactory<SM> pgMakerFactory;
	
	private ApplicationContext springContext;
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	private static Logger slog = LoggerFactory.getLogger ( MultiConfigPGMaker.class );

	
	/** 
	 * Gets an instance from the Spring application context. The returned instance is bound to the beanCtx parameter,
	 * so that {@link MultiConfigPGMaker#close()} can close it.
	 * 
	 * See the XML examples to know how you should configure
	 * beans for this.
	 * 
	 */
	public static <MM extends MultiConfigPGMaker<?, ?>> MM getSpringInstance ( 
		ApplicationContext beanCtx, Class<? extends MM> makerClass
	)
	{
		slog.info ( "Getting maker configuration from Spring Context" );
		MM multiMaker = beanCtx.getBean ( makerClass );
		multiMaker.setSpringContext ( beanCtx );
		return multiMaker;
	}

	/**
	 * Invokes {@link #getSpringInstance(ApplicationContext)} with the context obtained from the XML 
	 * configuration file.
	 * 
	 * The instance returned this way will close the application context when the {@link MultiConfigPGMaker#close()}
	 * method is invoked.
	 *  
	 */
	public static <MM extends MultiConfigPGMaker<?, ?>> MM getSpringInstance ( String xmlConfigPath, Class<? extends MM> makerClass )
	{
		slog.info ( "Getting maker configuration from Spring XML file '{}'", xmlConfigPath );		
		ApplicationContext ctx = new FileSystemXmlApplicationContext ( xmlConfigPath );
		return getSpringInstance ( ctx, makerClass );
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
	public void make ( String tdbPath, Object... opts )
	{
		this.makeBegin ( tdbPath, opts );
		
		// First the nodes ( mode = 0 ) and then the relations ( mode = 1 )
		// That ensures that cross-references made by different queries are taken  
		for ( int mode = 0; mode <= 2; mode++ )
			for ( CI cfg: this.getConfigItems () )
				this.makeIteration ( mode, cfg, tdbPath, opts );
		
		this.makeEnd ( tdbPath, opts );
	}
	
	/**
	 * Just logs that it's beginning with the current maker.
	 */
	protected void makeBegin ( String tdbPath, Object... opts )
	{
		log.info ( "Using {} PG maker", this.getClass ().getSimpleName () );
	}
	
	protected void makeIteration ( int mode, CI cfg, String tdbPath, Object... opts )
	{
		try ( SM pgSimpleMaker = this.getPGMakerFactory ().getObject (); )
		{
			cfg.configureMaker ( pgSimpleMaker );
			pgSimpleMaker.make ( tdbPath, mode == 0, mode == 1 );
		}
	}
	
	protected void makeEnd ( String tdbPath, Object... opts )
	{
		// Defaults to null
	}
	
	
	
	/**
	 * A single configuration defines how Cypher nodes and relations are mapped from RDF.
	 * 
	 * @see {@link #make(String, Object...)} 
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
	 * This is used to get a new {@link SimplePGMaker} to be used with a new configuration while iterating over
	 * {@link #getConfigItems()}. This is designed this way in order to make it possible to configure/autowire
	 * a factory via Spring.
	 * 
	 */
	public SimplePGMakerFactory<SM> getPGMakerFactory ()
	{
		return pgMakerFactory;
	}

	/**
	 * You need to create a bean of type {@link SimplePGMakerFactory} in the specific PG package.
	 * 
	 * @Resource creates problems with Java 11 (<a href = "https://stackoverflow.com/questions/56855335">see here</a>).
	 */
	@Autowired
	public void setPGMakerFactory ( SimplePGMakerFactory<SM> makerFactory )
	{
		this.pgMakerFactory = makerFactory;
	}
	
	
	/**
	 * This does something effectively if the current maker instance was obtained via one of the 
	 * {@link #getSpringInstance(ApplicationContext)} methods, namely, the corresponding Spring context is closed.
	 * 
	 * If the maker was obtained some other way, this method has no effect and you can safely call it, just in case.
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
