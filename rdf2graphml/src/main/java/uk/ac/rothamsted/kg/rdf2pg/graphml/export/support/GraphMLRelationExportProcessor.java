package uk.ac.rothamsted.kg.rdf2pg.graphml.export.support;

import java.util.Optional;
import java.util.function.Consumer;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGRelationMakeProcessor;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;

/**
 * <H1>The Relation Export Processor</H1>
 * 
 * It's like the parent, just binds the right handler. 
 * 
 * @author cbobed
 * <dl><dt>Date:</dt><dd>16 Apr 2020</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public class GraphMLRelationExportProcessor extends PGRelationMakeProcessor<GraphMLRelationExportHandler>
{
	public GraphMLRelationExportProcessor ()
	{
		super ();
		// Might be useful to experiment with different degrees of parallelism
		// 1, 1 essentially makes things sequential
		//this.setExecutor ( HackedBlockingQueue.createExecutor (1, 1) );
	}
	
	/**
	 * TODO: remove, here for debugging.
	 * 
	 */
	@Override
	public void process ( RdfDataManager rdfMgr, Object...opts )
	{
		log.info ( "Starting PG relations making" );
		
		GraphMLRelationExportHandler handler = this.getBatchJob ();

		// processNodeIris() passes the IRIs obtained from SPARQL to the IRI consumer set by the BatchProcessor. The latter
		// pushes the IRI into a batch and submits a full batch to the parallel executor.
		Consumer<Consumer<QuerySolution>> relIriProcessor = 
			solProc -> rdfMgr.processRelationIris ( handler.getRelationTypesSparql (), 
				qsol -> {

					int relOndexId = Optional.ofNullable ( qsol.get ( "ondexId" ) )
					.filter ( RDFNode::isLiteral )
					.map ( RDFNode::asLiteral )
					.map ( Literal::getInt )
					.orElse ( -1 );

					if ( relOndexId == 677351 ) log.warn ( "====> THE TEST REL IS HERE (BEFORE) <======" );
					
					solProc.accept ( qsol );
					
					relOndexId = Optional.ofNullable ( qsol.get ( "ondexId" ) )
					.filter ( RDFNode::isLiteral )
					.map ( RDFNode::asLiteral )
					.map ( Literal::getInt )
					.orElse ( -1 );

					if ( relOndexId == 677351 ) log.warn ( "====> THE TEST REL IS HERE (AFTER) <======" );

				} 
		);
		
		super.process ( relIriProcessor );
		log.info ( "PG relations making ended" );
	}
	
}
