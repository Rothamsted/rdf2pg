package uk.ac.rothamsted.rdf.pg.load.neo4j;

import javax.annotation.Resource;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;

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
	@Resource ( type = SimpleCyLoader.class ) @Override
	public void setPGLoaderFactory ( ObjectFactory<SimpleCyLoader> loaderFactory )
	{
		super.setPGLoaderFactory ( loaderFactory );
	}
}
