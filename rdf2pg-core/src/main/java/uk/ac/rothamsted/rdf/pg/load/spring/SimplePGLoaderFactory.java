package uk.ac.rothamsted.rdf.pg.load.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import uk.ac.rothamsted.rdf.pg.load.ConfigItem;
import uk.ac.rothamsted.rdf.pg.load.MultiConfigPGLoader;
import uk.ac.rothamsted.rdf.pg.load.SimplePGLoader;

/**
 * This is a simple {@link SimplePGLoader} factory which of {@link #getObject()} invokes a 
 * {@link LoadingSessionScope#startSession() new loading session}. This factory is auto-wired in 
 * {@link MultiConfigPGLoader#setPGLoaderFactory(ObjectFactory)}, where a new simple loader is
 * invoked per every new {@link ConfigItem} query type to be processed.
 *  
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Feb 2018</dd></dl>
 *
 */
public abstract class SimplePGLoaderFactory<T extends SimplePGLoader> implements ObjectFactory<T>, ApplicationContextAware
{
	protected Class<T> type; 
	protected ApplicationContext appCtx;

	@Autowired
	protected LoadingSessionScope session;

	protected SimplePGLoaderFactory ( Class<T> type ) {
		this.type = type; 
	}
	
	
	/**
	 * @return the {@link SimpleCyLoader} configured in the current Spring container.
	 */
	@Override
	public T getObject () throws BeansException
	{
		session.startSession ();
		return this.appCtx.getBean ( type );
	}

	@Override
	public void setApplicationContext ( ApplicationContext applicationContext ) throws BeansException
	{
		this.appCtx = applicationContext;
	}	
}
