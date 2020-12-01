package uk.ac.rothamsted.kg.rdf2pg.neo4j.load;

import javax.annotation.Resource;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;

import jdk.management.jfr.ConfigurationInfo;
import uk.ac.rothamsted.kg.rdf2pg.load.ConfigItem;
import uk.ac.rothamsted.kg.rdf2pg.load.MultiConfigPGLoader;

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
	
}
