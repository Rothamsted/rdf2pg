package uk.ac.rothamsted.kg.rdf2pg.pgmaker.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import uk.ac.rothamsted.kg.rdf2pg.pgmaker.ConfigItem;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.SimplePGMaker;

/**
 * This is a simple {@link SimplePGMaker} factory which of {@link #getObject()} invokes a 
 * {@link PGMakerSessionScope#startSession() new maker session}. This factory is auto-wired in 
 * {@link MultiConfigPGMaker#setPGMakerFactory(ObjectFactory)}, where a new simple maker is
 * invoked per every new {@link ConfigItem} query type to be processed.
 *  
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Feb 2018</dd></dl>
 *
 */
public abstract class SimplePGMakerFactory<SM extends SimplePGMaker<?, ?, ?, ?>> 
  implements ObjectFactory<SM>, ApplicationContextAware
{
	protected Class<SM> type; 
	protected ApplicationContext appCtx;

	@Autowired
	protected PGMakerSessionScope session;

	protected SimplePGMakerFactory ( Class<SM> type ) {
		this.type = type; 
	}
	
	
	/**
	 * @return the {@link SimplePGMaker} configured in the current Spring container.
	 */
	@Override
	public SM getObject () throws BeansException
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
