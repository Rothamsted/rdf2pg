package uk.ac.rothamsted.rdf.neo4j.load.spring;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.neo4j.load.MultiConfigCyLoader.ConfigItem;

/**
 * This is a custom Spring bean {@link Scope} that models the notion of a loading session. 
 * We start a new session upon every new {@link ConfigItem} and every new query type (nodes or relations), 
 * so that we can commit intermediate changes.
 *
 * There are several components around, which must have this scope, see Spring annotations and XML examples.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Feb 2018</dd></dl>
 *
 */
@Component
public class LoadingSessionScope implements Scope
{
	@Component
	public static class Configurer implements BeanFactoryPostProcessor
	{
		@Override
		public void postProcessBeanFactory ( ConfigurableListableBeanFactory beanFactory ) throws BeansException
		{
			beanFactory.registerScope ( "loadingSession", beanFactory.getBean ( LoadingSessionScope.class ) );
		}
	}

	private Map<String, Object> beans = Collections.synchronizedMap ( new HashMap<String, Object> () );	
	private AtomicInteger sessionId = new AtomicInteger ( 0 );
	
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	@Override
	public Object get ( String name, ObjectFactory<?> objectFactory )
	{
		Object result = beans.get ( name );
		
		if ( result != null ) {
			log.trace ( "Returning exising loading session object {}", name );		
			return result;
		}

		log.trace ( "New loading session object {}", name );		
		beans.put ( name, result = objectFactory.getObject () );
		return result;
	}

	@Override
	public Object remove ( String name )
	{
		log.trace ( "Destroying exising loading session object {}", name );				
		return beans.remove ( name );
	}

	@Override
	public void registerDestructionCallback ( String name, Runnable callback ) {		
	}

	@Override
	public Object resolveContextualObject ( String key ) {
		return null;
	}

	/**
	 * While it shouldn't be needed, we generate a new ID every time {@link #startSession()} is invoked.
	 * 
	 */
	@Override
	public String getConversationId () {
		return sessionId.toString ();
	}

	public synchronized void startSession ()
	{
		beans.clear ();
		log.debug ( "Starting new loading session #{} within Spring", sessionId.incrementAndGet () );
	}
	
}
