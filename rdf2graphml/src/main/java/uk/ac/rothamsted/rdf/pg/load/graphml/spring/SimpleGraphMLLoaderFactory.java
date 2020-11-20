package uk.ac.rothamsted.rdf.pg.load.graphml.spring;

import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.graphml.SimpleGraphMLLoader;
import uk.ac.rothamsted.rdf.pg.load.spring.SimplePGLoaderFactory;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>31 Jul 2020</dd></dl>
 *
 */
@Component
public class SimpleGraphMLLoaderFactory extends SimplePGLoaderFactory<SimpleGraphMLLoader>
{
	public SimpleGraphMLLoaderFactory () {
		super ( SimpleGraphMLLoader.class );
	}
}
