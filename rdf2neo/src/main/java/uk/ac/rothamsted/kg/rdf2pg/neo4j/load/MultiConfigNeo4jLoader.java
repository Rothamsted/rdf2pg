package uk.ac.rothamsted.kg.rdf2pg.neo4j.load;

import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.pgmaker.MultiConfigPGMaker;

/**
 * It has just a minor addition to consider the DB indexing.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>29 Jun 2020</dd></dl>
 *
 */
@Component
public class MultiConfigNeo4jLoader extends MultiConfigPGMaker<Neo4jConfigItem, SimpleCyLoader>
{
	/**
	 * Just a wrapper of {@link #make(String, Object...)}.
	 */
	public void load ( String tdbPath )
	{
		super.make ( tdbPath );
	}

	@Override
	protected void makeBegin ( String tdbPath, Object... opts )
	{
		super.makeBegin ( tdbPath, opts );
	}

	/**
	 * With respect to the parent, manages the additional case of mode == 2 (do the indexing).
	 * TODO: options as a Map and then we can get rid of this, leaving the parent's version.
	 */
	@Override
	protected void makeIteration ( int mode, Neo4jConfigItem cfg, String tdbPath, Object... opts )
	{
		try ( SimpleCyLoader cyLoader = this.getPGMakerFactory ().getObject (); )
		{
			cfg.configureMaker ( cyLoader );
			cyLoader.make ( tdbPath, mode == 0, mode == 1, mode == 2 );
		}		
	}
	
}
