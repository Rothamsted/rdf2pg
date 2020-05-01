package uk.ac.rothamsted.rdf.pg.load.support.neo4j;

import org.apache.jena.rdf.model.Resource;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.threading.batchproc.BatchProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.PGLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.PGNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.PGRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.neo4j.CyNodeLoadingHandler;

/**
 * <H1>The Node Loading processor</H1>
 * 
 * <p>This gets node IRIs from a SPARQL query and then send them to a 
 * {@link CyNodeLoadingHandler}, for issuing Cypher creation commands. Being a subclass of {@link BatchProcessor}, 
 * this processor manages the Cypher loading in a multi-thread mode.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
  *  Modifified by cbobed for refactoring purposes  
 * <dl><dt>Date:</dt><dd>30 Apr 2020</dd></dl>
 */
@Component @Scope ( scopeName = "loadingSession" )
public class CyNodeLoadingProcessor extends PGNodeLoadingProcessor<CyNodeLoadingHandler>
{
	
}
