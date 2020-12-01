package uk.ac.rothamsted.kg.rdf2pg.graphml.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLDataManager;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.ConfigItem;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;

/**
 * Similar to the parent, manages the multi-config GraphML export. 
 * 
 * Indeed, this is the <b>only</b> class that writes some output, since the GraphML writing requires type
 * collection and then, to write a header with the collected types.
 * 
 * TODO: for the moment, we only support file output path as output option, we'd better supporting
 * File or PrintStream objects too.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Jun 2020</dd></dl>
 *
 */
@Component
public class MultiConfigGraphMLExporter 
	extends MultiConfigPGMaker<ConfigItem<SimpleGraphMLExporter>, SimpleGraphMLExporter>
{
	@Autowired
	private GraphMLDataManager graphmlDataMgr;
	
	@Override
	protected void makeBegin ( String tdbPath, Object... opts )
	{
		super.makeBegin ( tdbPath, opts );
		
		if ( opts == null || opts.length != 1 ) throw new IllegalArgumentException ( String.format (
			"%s needs the output file parameter", this.getClass ().getSimpleName ()
		));
		
		String outPath = (String) opts [ 0 ];
		this.graphmlDataMgr.setGraphmlOutputPath ( outPath );
	}

	@Override
	protected void makeIteration ( int mode, ConfigItem<SimpleGraphMLExporter> cfg, String tdbPath, Object... opts )
	{
		String outPath = (String) opts [ 0 ];

		try (  var graphMLExporter = this.getPGMakerFactory ().getObject (); )
		{
			cfg.configureMaker ( graphMLExporter );
			graphMLExporter.make ( tdbPath, mode == 0, mode == 1, outPath );
		}
	}

	@Override
	protected void makeEnd ( String tdbPath, Object... opts )
	{
		graphmlDataMgr.writeGraphML ();
	}	
}
