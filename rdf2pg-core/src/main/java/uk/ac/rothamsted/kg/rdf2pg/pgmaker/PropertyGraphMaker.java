package uk.ac.rothamsted.kg.rdf2pg.pgmaker;

import java.util.Map;

import uk.ac.ebi.utils.collections.OptionsMap;

/**
 * The Generic interface to make a property Graph, 
 * after having SPARQL-fetched them from a Jena TDB triple store. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Jan 2018</dd></dl>
 *
 * Moddied by cbobed for refactoring purposes  
 * <dl><dt>Date:</dt><dd>28 Apr 2020</dd></dl>
 */
public interface PropertyGraphMaker
{
	/**
	 * Makes a property graph from RDF data coming from the Jena TDB store. 
	 * 
	 * @param opts is a generic way to pass implementation-specific options
	 */
	public void make ( String tdbPath, OptionsMap options );
	
	public default void make ( String tdbPath ) {
		make ( tdbPath, OptionsMap.from ( Map.of () ) );
	}
}