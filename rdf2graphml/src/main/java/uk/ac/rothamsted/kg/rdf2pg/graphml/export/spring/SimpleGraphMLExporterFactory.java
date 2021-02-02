package uk.ac.rothamsted.kg.rdf2pg.graphml.export.spring;

import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.graphml.export.SimpleGraphMLExporter;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.spring.SimplePGMakerFactory;

/**
 * A simple extension of {@link SimplePGMakerFactory} that just binds to {@link SimpleGraphMLExporter}. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>31 Jul 2020</dd></dl>
 *
 */
@Component
public class SimpleGraphMLExporterFactory extends SimplePGMakerFactory<SimpleGraphMLExporter>
{
	public SimpleGraphMLExporterFactory () {
		super ( SimpleGraphMLExporter.class );
	}
}
