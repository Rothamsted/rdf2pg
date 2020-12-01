package uk.ac.rothamsted.kg.rdf2pg.neo4j.load.spring;

import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.SimpleCyLoader;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.spring.SimplePGMakerFactory;

/**
 * A simple extension of {@link SimplePGMakerFactory} that just binds to {@link SimpleCyLoader}. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>31 Jul 2020</dd></dl>
 *
 */
@Component
public class SimpleCyLoaderFactory extends SimplePGMakerFactory<SimpleCyLoader>
{
	public SimpleCyLoaderFactory () {
		super ( SimpleCyLoader.class );
	}
}
