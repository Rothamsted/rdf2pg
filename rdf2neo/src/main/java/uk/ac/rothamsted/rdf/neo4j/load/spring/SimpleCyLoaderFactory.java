package uk.ac.rothamsted.rdf.neo4j.load.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.neo4j.load.MultiConfigCyLoader;
import uk.ac.rothamsted.rdf.neo4j.load.MultiConfigCyLoader.ConfigItem;
import uk.ac.rothamsted.rdf.neo4j.load.SimpleCyLoader;

/**
 * This is a simple {@link SimpleCyLoader} factory which of {@link #getObject()} invokes a 
 * {@link LoadingSessionScope#startSession() new loading session}. This factory is autowired in 
 * {@link MultiConfigCyLoader#setCypherLoaderFactory(ObjectFactory)}, where a new simple loader is
 * invoked per every new {@link ConfigItem} query type to be processed.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Feb 2018</dd></dl>
 *
 */
@Component
public class SimpleCyLoaderFactory implements ObjectFactory<SimpleCyLoader>, ApplicationContextAware
{
	private ApplicationContext appCtx;
	
	@Autowired
	private LoadingSessionScope session;
	
	/**
	 * @return the {@link SimpleCyLoader} configured in the current Spring container.
	 */
	@Override
	public SimpleCyLoader getObject () throws BeansException
	{
		session.startSession ();
		return this.appCtx.getBean ( SimpleCyLoader.class );
	}

	@Override
	public void setApplicationContext ( ApplicationContext applicationContext ) throws BeansException
	{
		this.appCtx = applicationContext;
	}
}
