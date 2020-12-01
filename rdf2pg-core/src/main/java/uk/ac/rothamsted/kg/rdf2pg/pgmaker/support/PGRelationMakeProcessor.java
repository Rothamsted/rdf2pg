package uk.ac.rothamsted.kg.rdf2pg.pgmaker.support;

import java.util.function.Consumer;

import org.apache.jena.query.QuerySolution;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;

/**
 * <H1>The Relation Make Processor</H1>
 * 
 * <p>Similarly to the {@link PGNodeMakeProcessor}, this gets SPARQL bindings (i.e., rows) corresponding to 
 * tuples of relation basic properties () and then sends them to a 
 * {@link PGRelationHandler}, for generating target PG relations. As for the node processor, 
 * this processor does things in multi-thread fashion.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public abstract class PGRelationMakeProcessor<RH extends PGRelationHandler> extends PGMakerProcessor<QuerySolution, RH>
{	
	/**
	 * This takes the relations mapped via {@link PGRelationHandler#getRelationTypesSparql()} and creates
	 * sets of {@link QuerySolution}s that are sent to {@link PGRelationHandler} tasks.
	 */
	public void process ( RdfDataManager rdfMgr, Object...opts )
	{
		log.info ( "Starting PG relations making" );
		
		RH handler = this.getBatchJob ();

		// processNodeIris() passes the IRIs obtained from SPARQL to the IRI consumer set by the BatchProcessor. The latter
		// pushes the IRI into a batch and submits a full batch to the parallel executor.
		Consumer<Consumer<QuerySolution>> relIriProcessor = 
			solProc -> rdfMgr.processRelationIris ( handler.getRelationTypesSparql (), solProc );
		
		super.process ( relIriProcessor );
		log.info ( "PG relations making ended" );
	}
}