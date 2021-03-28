package uk.ac.rothamsted.kg.rdf2pg.pgmaker.support;

import java.util.function.Consumer;

import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.threading.batchproc.BatchProcessor;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;

/**
 * <H1>The Node Make processor</H1>
 * 
 * <p>This gets node IRIs from a SPARQL query and then send them to a 
 * {@link PGNodeHandler}, for issuing Cypher creation commands. Being a subclass of {@link BatchProcessor}, 
 * this processor manages the PG making in a multi-thread mode.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public abstract class PGNodeMakeProcessor<NH extends PGNodeHandler>
	extends PGMakerProcessor<Resource, NH>
{
	private String nodeIrisSparql;
	
	public void process ( RdfDataManager rdfMgr, Object...opts )
	{
		log.info ( "Starting PG nodes making" );
		
		// processNodeIris() passes the IRIs obtained from SPARQL to the IRI consumer set by the BatchProcessor. The latter
		// pushes the IRI into a batch and submits a full batch to the parallel executor.
		Consumer<Consumer<Resource>> nodeIriProcessor = 
			resProc -> rdfMgr.processNodeIris ( this.getNodeIrisSparql (), resProc );
		
		super.process ( nodeIriProcessor );
		log.info ( "PG nodes making ended" );
	}

	/**
	 * The query to be used with the {@link RdfDataManager} to fetch the IRIs about PG nodes that need to be
	 * loaded (created/exported/whatever). This usually goes together with {@link PGNodeHandler#getLabelsSparql()} and
	 * {@link PGNodeHandler#getNodePropsSparql()}.
	 * 
	 * This query <b>must</b> return the variables {@code ?iri} in its result. It must also return distinct results (we 
	 * obviously don't care if you obtain that by means of {@code DISTINCT} or not). 
	 */
	public String getNodeIrisSparql ()
	{
		return nodeIrisSparql;
	}

	@Autowired ( required = false ) @Qualifier ( "nodeIrisSparql" )
	public void setNodeIrisSparql ( String nodeIrisSparql )
	{
		this.nodeIrisSparql = nodeIrisSparql;
	}
	
}
