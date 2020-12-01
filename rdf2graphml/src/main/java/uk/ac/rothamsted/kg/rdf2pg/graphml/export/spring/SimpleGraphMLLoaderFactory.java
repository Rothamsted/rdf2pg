package uk.ac.rothamsted.kg.rdf2pg.graphml.export.spring;

import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.graphml.export.SimpleGraphMLExporter;
import uk.ac.rothamsted.kg.rdf2pg.load.spring.SimplePGLoaderFactory;

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
