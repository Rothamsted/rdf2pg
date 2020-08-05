package uk.ac.rothamsted.rdf.pg.load;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeExportHandler;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLRelationExportHandler;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLRelationLoadingProcessor;

@Component @Scope ( scopeName = "loadingSession" )
public class SimpleGraphMLExporter
  extends SimplePGLoader <GraphMLNodeExportHandler, GraphMLRelationExportHandler, 
  												GraphMLNodeLoadingProcessor, GraphMLRelationLoadingProcessor>
{
	// Nothing needed, this is just an extension to fix the generics.
	
}
