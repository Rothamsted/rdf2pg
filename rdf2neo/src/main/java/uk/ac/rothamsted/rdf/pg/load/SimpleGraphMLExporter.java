package uk.ac.rothamsted.rdf.pg.load;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.rdf.pg.load.support.PGNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.PGRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLNodeLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.graphml.GraphMLRelationLoadingProcessor;
import uk.ac.rothamsted.rdf.pg.load.support.rdf.RdfDataManager;

@Component @Scope ( scopeName = "loadingSession" )
public class SimpleGraphMLExporter extends SimplePGLoader <GraphMLNodeLoadingProcessor, GraphMLRelationLoadingProcessor>
{	
}
