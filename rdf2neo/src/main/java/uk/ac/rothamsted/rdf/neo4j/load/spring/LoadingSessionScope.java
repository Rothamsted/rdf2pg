package uk.ac.rothamsted.rdf.neo4j.load.spring;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.Scope;
import org.springframework.stereotype.Component;

/**
 * TODO: comment me!
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
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	@Override
	public Object get ( String name, ObjectFactory<?> objectFactory )
	{
		log.trace ( "New loading session object {}", name );		
		Object result = beans.get ( name );
		if ( result == null ) 
			beans.put ( name, result = objectFactory.getObject () );
		return result;
	}

	@Override
	public Object remove ( String name ) {
		return beans.get ( name );
	}

	@Override
	public void registerDestructionCallback ( String name, Runnable callback ) {		
	}

	@Override
	public Object resolveContextualObject ( String key ) {
		return null;
	}

	@Override
	public String getConversationId () {
		return "loadingSession";
	}

	public void startSession ()
	{
		log.debug ( "Starting new loading session within Spring loading scope" );
		beans.clear ();
	}
	
}
