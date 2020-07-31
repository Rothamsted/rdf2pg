package uk.ac.rothamsted.rdf.pg.load.spring;

import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.SimpleGraphMLExporter;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>31 Jul 2020</dd></dl>
 *
 */
@Component
public class SimpleGraphMLLoaderFactory extends SimplePGLoaderFactory<SimpleGraphMLExporter>
{
	public SimpleGraphMLLoaderFactory () {
		super ( SimpleGraphMLExporter.class );
	}
}
