package uk.ac.rothamsted.rdf.pg.load.neo4j;

import javax.annotation.Resource;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;

import jdk.management.jfr.ConfigurationInfo;
import uk.ac.rothamsted.rdf.pg.load.ConfigItem;
import uk.ac.rothamsted.rdf.pg.load.MultiConfigPGLoader;
import uk.ac.rothamsted.rdf.pg.load.SimpleCyLoader;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Jun 2020</dd></dl>
 *
 */
@Component
public class MultiConfigNeo4jLoader extends MultiConfigPGLoader<Neo4jConfigItem, SimpleCyLoader>
{
	@Override
	protected void loadBegin ( String tdbPath, Object... opts )
	{
		super.loadBegin ( tdbPath, opts );
		
		if ( opts == null || opts.length < 3 ) throw new IllegalArgumentException ( String.format (
			"%s needs at least 3 parameters", this.getClass ().getSimpleName ()
		));
	}

	@Override
	protected void loadIteration ( int mode, Neo4jConfigItem cfg, String tdbPath, Object... opts )
	{
		try ( SimpleCyLoader cyLoader = this.getPGLoaderFactory ().getObject (); )
		{
			cfg.configureLoader ( cyLoader );
			cyLoader.load ( tdbPath, mode == 0, mode == 1, mode == 2 );
		}
	}
	
	
	@Resource ( type = SimpleCyLoader.class ) @Override
	public void setPGLoaderFactory ( ObjectFactory<SimpleCyLoader> loaderFactory )
	{
		super.setPGLoaderFactory ( loaderFactory );
	}
	
	
}
