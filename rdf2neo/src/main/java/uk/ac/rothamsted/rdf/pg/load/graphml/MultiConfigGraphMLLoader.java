package uk.ac.rothamsted.rdf.pg.load.graphml;

import javax.annotation.Resource;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.ConfigItem;
import uk.ac.rothamsted.rdf.pg.load.MultiConfigPGLoader;
import uk.ac.rothamsted.rdf.pg.load.SimpleCyLoader;
import uk.ac.rothamsted.rdf.pg.load.SimpleGraphMLExporter;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Jun 2020</dd></dl>
 *
 */
@Component
public class MultiConfigGraphMLLoader 
	extends MultiConfigPGLoader<ConfigItem<SimpleGraphMLExporter>, SimpleGraphMLExporter>
{
	@Resource ( type = SimpleGraphMLExporter.class ) @Override
	public void setPGLoaderFactory ( ObjectFactory<SimpleGraphMLExporter> loaderFactory )
	{
		super.setPGLoaderFactory ( loaderFactory );
	}
}
