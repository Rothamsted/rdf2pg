package uk.ac.rothamsted.kg.rdf2pg.graphml.export;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLNodeExportHandler;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLNodeExportProcessor;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLRelationExportHandler;
import uk.ac.rothamsted.kg.rdf2pg.graphml.export.support.GraphMLRelationExportProcessor;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.SimplePGMaker;

/**
 * <h1>The Simple GraphML Exporter</h1>
 * 
 * This is just a wrapper of {@link SimplePGMaker} which wires together the right generics and defines the
 * right Spring annotations.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>26 Oct 2020</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public class SimpleGraphMLExporter
  extends SimplePGMaker <GraphMLNodeExportHandler, GraphMLRelationExportHandler, 
  												GraphMLNodeExportProcessor, GraphMLRelationExportProcessor>
{
	// As said above, nothing special is needed
}
