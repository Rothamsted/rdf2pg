package uk.ac.rothamsted.rdf.pg.load.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.MultiConfigPGLoader;
import uk.ac.rothamsted.rdf.pg.load.*;
import uk.ac.rothamsted.rdf.pg.load.ConfigItem;

/**
 * This is a simple {@link SimpleCyLoader} factory which of {@link #getObject()} invokes a 
 * {@link LoadingSessionScope#startSession() new loading session}. This factory is autowired in 
 * {@link MultiConfigPGLoader#setCypherLoaderFactory(ObjectFactory)}, where a new simple loader is
 * invoked per every new {@link ConfigItem} query type to be processed.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Feb 2018</dd></dl>
 *
 */

// no longer autowired as component in order to have it configured via the .xml file

public class SimplePGLoaderFactory<T> implements ObjectFactory<T>, ApplicationContextAware
{
	// workaround to have the information about the generic type
	// without using reflection
	private final Class<T> type; 
	
	public SimplePGLoaderFactory(Class<T> type) {
		this.type = type; 
	}
	
	private ApplicationContext appCtx;

	@Autowired
	private LoadingSessionScope session;
	
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
