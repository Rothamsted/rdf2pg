package uk.ac.rothamsted.kg.rdf2pg.pgmaker;

/**
 * The Generic interface to make a property Graph, after having SPARQL-fetched them from a Jena TDB triple store. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Jan 2018</dd></dl>
 *
 * Modifified by cbobed for refactoring purposes  
 * <dl><dt>Date:</dt><dd>28 Apr 2020</dd></dl>
 */
public interface PropertyGraphMaker
{
	/**
	 * Makes a property graph from RDF data coming from the Jena TDB store. 
	 * 
	 * @param opts is a generic way to pass implementation-specific options
	 * 
	 * TODO: it would be batter to have opts as a Map<String, Object> + an array of [key, val, key...].
	 */
	public void make ( String tdbPath, Object... opts );
}