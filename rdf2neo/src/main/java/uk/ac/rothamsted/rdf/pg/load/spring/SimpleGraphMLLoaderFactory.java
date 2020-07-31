package uk.ac.rothamsted.rdf.pg.load.spring;

import uk.ac.rothamsted.rdf.pg.load.SimpleCyLoader;
import uk.ac.rothamsted.rdf.pg.load.SimpleGraphMLExporter;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>31 Jul 2020</dd></dl>
 *
 */
public class SimpleGraphMLLoaderFactory extends SimplePGLoaderFactory<SimpleGraphMLExporter>
{
	public SimpleGraphMLLoaderFactory () {
		super ( SimpleGraphMLExporter.class );
	}
}
