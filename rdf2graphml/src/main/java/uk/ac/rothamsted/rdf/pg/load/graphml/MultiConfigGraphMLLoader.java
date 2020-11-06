package uk.ac.rothamsted.rdf.pg.load.graphml;

import javax.annotation.Resource;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.ConfigItem;
import uk.ac.rothamsted.rdf.pg.load.MultiConfigPGLoader;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLDataManager;

/**
 * TODO: comment me!
 * TODO: for the moment, we only support file output path as output option, we'd better supporting
 * File or PrintStream objects too.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Jun 2020</dd></dl>
 *
 */
@Component
public class MultiConfigGraphMLLoader 
	extends MultiConfigPGLoader<ConfigItem<SimpleGraphMLLoader>, SimpleGraphMLLoader>
{
	@Autowired
	GraphMLDataManager gmlDataMgr;
	
	
	@Resource ( type = SimpleGraphMLLoader.class ) @Override
	public void setPGLoaderFactory ( ObjectFactory<SimpleGraphMLLoader> loaderFactory )
	{
		super.setPGLoaderFactory ( loaderFactory );
	}

	@Override
	protected void loadBegin ( String tdbPath, Object... opts )
	{
		super.loadBegin ( tdbPath, opts );
		
		if ( opts == null || opts.length != 1 ) throw new IllegalArgumentException ( String.format (
			"%s needs the output file parameter", this.getClass ().getSimpleName ()
		));
		
		String outPath = (String) opts [ 0 ];
		this.gmlDataMgr.setGraphmlOutputPathOutputPath ( outPath );
	}

	@Override
	protected void loadIteration ( int mode, ConfigItem<SimpleGraphMLLoader> cfg, String tdbPath, Object... opts )
	{
		String outPath = (String) opts [ 0 ];

		try (  var graphMLExporter = this.getPGLoaderFactory ().getObject (); )
		{
			cfg.configureLoader ( graphMLExporter );
			graphMLExporter.load ( tdbPath, mode == 0, mode == 1, outPath );
		}
	}

	@Override
	protected void loadEnd ( String tdbPath, Object... opts )
	{
		gmlDataMgr.writeGraphML ();
	}
	
}
