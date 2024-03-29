package uk.ac.rothamsted.kg.rdf2pg.neo4j.load;

import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.collections.OptionsMap;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.ConfigItem;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;

/**
 * It has just a very minor method alias.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Jun 2020</dd></dl>
 *
 */
@Component
public class MultiConfigNeo4jLoader extends MultiConfigPGMaker<ConfigItem<SimpleCyLoader>, SimpleCyLoader>
{
	/**
	 * Just a wrapper of {@link #make(String, Object...)}.
	 */
	public void load ( String tdbPath )
	{
		this.make ( tdbPath, OptionsMap.create () );
	}	
}
