package uk.ac.rothamsted.kg.rdf2pg.neo4j.load.support;

import java.util.function.Consumer;

import org.apache.jena.query.QuerySolution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.load.support.PGLoadingProcessor;
import uk.ac.rothamsted.kg.rdf2pg.load.support.PGRelationLoadingProcessor;
import uk.ac.rothamsted.kg.rdf2pg.load.support.entities.PGRelation;
import uk.ac.rothamsted.kg.rdf2pg.load.support.rdf.RdfDataManager;

/**
 * <H1>The Relation Loading processor for Neo4j</H1>
 * 
 * As per {@link PGRelationLoadingProcessor} contract, gather {@link PGRelation PG relations} from RDF and 
 * uses a {@link CyRelationLoadingHandler} to issue corresponding Cypher instructions that create the 
 * relations on the target Neo4j. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "loadingSession" )
public class CyRelationLoadingProcessor extends PGRelationLoadingProcessor<CyRelationLoadingHandler>
{	
	/**
	 * This takes the relations mapped via {@link CyRelationLoadingHandler#getRelationTypesSparql()} and creates
	 * sets of {@link QuerySolution}s that are sent to {@link CyRelationLoadingHandler} tasks.
	 */
	public void process ( RdfDataManager rdfMgr, Object...opts )
	{
		log.info ( "Starting Cypher Relations Loading" );
		
		CyRelationLoadingHandler handler = this.getBatchJob ();

		// processNodeIris() passes the IRIs obtained from SPARQL to the IRI consumer set by the BatchProcessor. The latter
		// pushes the IRI into a batch and submits a full batch to the parallel executor.
		Consumer<Consumer<QuerySolution>> relIriProcessor = 
			solProc -> rdfMgr.processRelationIris ( handler.getRelationTypesSparql (), solProc );
		
		super.process ( relIriProcessor );
		log.info ( "Cypher Relations Loading ended" );
	}

	/**
	 * Does nothing but invoking {@link #setBatchJob(Consumer)}. It's here just to accommodate Spring annotations. 
	 */
	@Autowired
	public CyRelationLoadingProcessor setConsumer ( CyRelationLoadingHandler handler ) {
		super.setBatchJob ( handler );
		return this;
	}	
}