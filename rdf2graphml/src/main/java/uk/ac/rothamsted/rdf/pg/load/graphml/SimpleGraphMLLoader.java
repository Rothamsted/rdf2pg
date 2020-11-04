package uk.ac.rothamsted.rdf.pg.load.graphml;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.SimplePGLoader;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeExportHandler;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLRelationExportHandler;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLRelationLoadingProcessor;

/**
 * <h1>The Simple GraphML exporter</h1>
 * 
 * This is just a wrapper of {@link SimplePGLoader} which wires together the right generics and defines the
 * right Spring annotations.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>26 Oct 2020</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class SimpleGraphMLExporter
  extends SimplePGLoader <GraphMLNodeExportHandler, GraphMLRelationExportHandler, 
  												GraphMLNodeLoadingProcessor, GraphMLRelationLoadingProcessor>
{
	// As said above, nothing special is needed
}
