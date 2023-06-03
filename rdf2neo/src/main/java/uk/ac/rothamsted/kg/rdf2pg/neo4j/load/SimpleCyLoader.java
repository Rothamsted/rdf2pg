package uk.ac.rothamsted.kg.rdf2pg.neo4j.load;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyNodeLoadingHandler;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyNodeLoadingProcessor;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyRelationLoadingHandler;
import uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support.CyRelationLoadingProcessor;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.SimplePGMaker;

/**
 * <h1>The simple Cypher/Neo4j loader</h1> 
 * 
 * <p>This is just a marker extension of {@link SimplePGMaker}, it doesn't contain anything apart 
 * from proper bindings for the generics, which, of course, binds to components that maps
 * the input RDF to Cypher queries.</p> 
 * 
 * <p><b>WARNING</b>: we assume the target graph database is initially empty. For instance, we send CREATE
 * &lt;node&gt; instructions, without checking if a node already exists.</p>
 * 
 * @author brandizi
 * <dl><dt>Date:</dt><dd>11 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public class SimpleCyLoader extends
  SimplePGMaker<CyNodeLoadingHandler, CyRelationLoadingHandler, CyNodeLoadingProcessor, CyRelationLoadingProcessor>
{
	// Nothing needed, it's just a marker for fitting the generics and telling Spring.
}

