package uk.ac.rothamsted.kg.rdf2pg.graphml.export;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.collections.OptionsMap;
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
	
	/**
	 * Just a wrapper of {@link #make(String, Object...)}.
	 */
	public void export ( String tdbPath, String graphmlOutPath )
	{
		var opts = OptionsMap.from ( Map.of ( "graphmlOutPath", graphmlOutPath ) );
		this.make ( tdbPath, opts );
	}

	/**
	 * This takes "graphmlOutPath" from the opts and sets it in {@link #graphmlDataMgr}. 
	 */
	@Override
	protected void makeBegin ( String tdbPath, OptionsMap opts )
	{
		super.makeBegin ( tdbPath, opts );
				
		String outPath = Optional.ofNullable ( opts )
		.map ( o -> o.getString ( "graphmlOutPath" ) )
		.orElseThrow ( () -> new IllegalArgumentException ( String.format (
			"%s needs the graphmlOutPath option", this.getClass ().getSimpleName ()
		)));
		this.graphmlDataMgr.setGraphmlOutputPath ( outPath );
	}


	/**
	 * Finalises the export via {@link GraphMLDataManager#writeGraphML()}.
	 */
	@Override
	protected void makeEnd ( String tdbPath, OptionsMap opts )
	{
		graphmlDataMgr.writeGraphML ();
	}	
}
