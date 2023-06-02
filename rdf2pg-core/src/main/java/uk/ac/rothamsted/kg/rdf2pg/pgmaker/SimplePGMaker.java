package uk.ac.rothamsted.kg.rdf2pg.pgmaker;

import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.exceptions.ExceptionUtils;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.spring.PGMakerSessionScope;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGIndexer;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGNodeHandler;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGNodeMakeProcessor;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGRelationHandler;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGRelationMakeProcessor;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf.RdfDataManager;

/**
 * <h1>The single-config Property Graph Generator</h1>
 * 
 * <p>
 * 	This works with a single set of SPARQL queries mapping to PG node and relation entities.
 * 	The final applications are based on {@link MultiConfigPGMaker}, which allows for defining multiple
 *  queries and deal with different node/relation types separately.
 * </p>  
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>26 Oct 2020</dd></dl>
 *
 * Note: We recommend that you extend your extension of this class with both
 * {@code @Component} and {@code Scope} declarations. The special pgMakerSession puts simple makers
 * within the scope of a single-configuration conversion, and this is necessary for stateful operations 
 * like opening/closing a session for the {@link RdfDataManager}. See {@link PGMakerSessionScope} for 
 * details.  
 */
@Component @Scope ( scopeName = "pgmakerSession" )
public abstract class SimplePGMaker
  <NH extends PGNodeHandler, RH extends PGRelationHandler, 
  NP extends PGNodeMakeProcessor<NH>, RP extends PGRelationMakeProcessor<RH>>
	extends DefaultNamedPGMakeComp
	implements PropertyGraphMaker, AutoCloseable
{	
	private NP nodeMaker;
	private RP relationMaker;
	
	private PGIndexer pgIndexer;

	private RdfDataManager rdfDataManager = new RdfDataManager ();
		
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	
	/**
	 *  This does the job.
	 *  
	 *  The default version invokes {@link #makeBegin(String, Object...)}, {@link #makeBody(String, Object...)}
	 *  and {@link #makeEnd(String, Object...)}.
	 *  
	 *  Usually, the convention for the first three parameters is:
	 * 	 
	 * <p>
	 * opts [ 0 ] = true, means process the PG nodes nodes.<br/>
	 * opts [ 1 ] = true, means process the PG relations.<br/>
	 * opts [ 2 ] = true, means do post-process stuff like indices.<br/>
	 * </p>
	 * 
	 * <p>I opts is null, all the three operations above are run in the same sequence.</p>
	 *  
	 * <p>Nodes are always made before relations.</p>
	 * 
	 * <p>TODO: this needs to change in favour of a key/value map of options.
	 * 
	 */
	@Override
	public void make ( String tdbPath, Object... opts ) 
	{
		makeBegin ( tdbPath, opts );
		makeBody ( tdbPath, opts );
		makeEnd ( tdbPath, opts );		
	}

	protected void makeBegin ( String tdbPath, Object... opts )
	{
		// Nothing needed on the default.
	}

	protected void makeEnd ( String tdbPath, Object... opts )
	{
		log.info ( "{}RDF-PG conversion finished", getCompNamePrefix () );
	}
	
	/**
	 * This uses a {@link RdfDataManager} to load SPARQL queries that select PG elements, 
	 * via {@link #getPGNodeMaker()}, then it runs a similar job using {@link #getPGRelationMaker()}.
	 * 
	 */
	protected void makeBody ( String tdbPath, Object... opts )
	{		
		try
		{
			RdfDataManager rdfMgr = this.getRdfDataManager ();

			rdfDataManager.open ( tdbPath );
			Dataset ds = rdfMgr.getDataSet ();
			
			final String namePrefx = this.getCompNamePrefix ();
			
			Txn.executeRead ( ds, () -> 
				log.info ( "{}RDF source has about {} triple(s)", namePrefx, ds.getUnionModel().size () )
			);
			
			// Nodes
			boolean doNodes = opts != null && opts.length > 0 ? (Boolean) opts [ 0 ] : true;
			if ( doNodes ) this.getPGNodeMaker ().process ( rdfMgr, opts );
	
			// Relations
			boolean doRels = opts != null && opts.length > 1 ? (Boolean) opts [ 1 ] : true;
			if ( doRels ) this.getPGRelationMaker ().process ( rdfMgr, opts );
			
			// User-defined indices
			boolean doIdx = opts != null && opts.length > 2 ? (Boolean) opts [ 2 ] : true;

			if ( doIdx ) {
				PGIndexer indexer = this.getPgIndexer ();
				if ( indexer != null )
				{
					indexer.setComponentName ( this.getComponentName () );
					indexer.index ();
				}
			}
		}
		catch ( Exception ex ) {
			ExceptionUtils.throwEx ( RuntimeException.class, ex, 
				"Error while running the RDF/PG maker: $cause"
			);
		}
	}
	
	/**
	 * Closes dependency objects. It DOES NOT deal with Neo4j driver closing, since this could be reused 
	 * across multiple instantiations of this class.
	 */
	@Override
	public void close ()
	{
		try
		{
			if ( this.getRdfDataManager () != null ) this.rdfDataManager.close ();
			if ( this.getPGNodeMaker () != null ) this.nodeMaker.close ();
			if ( this.getPGRelationMaker () != null ) this.relationMaker.close ();
		}
		catch ( Exception ex ) {
			throw new RuntimeException ( "Internal error while running the PG maker: " + ex.getMessage (), ex );
		}
	}


	/**
	 * The manager to access to the underlining RDF source.
	 */
	public RdfDataManager getRdfDataManager ()
	{
		return rdfDataManager;
	}

	@Autowired
	public void setRdfDataManager ( RdfDataManager rdfDataManager )
	{
		this.rdfDataManager = rdfDataManager;
	}

	/**
	 * The processor that works out the PG node making. 
	 *  
	 */
	public NP getPGNodeMaker ()
	{
		return nodeMaker;
	}

	@Autowired
	public void setPGNodeMaker ( NP nodeMaker )
	{
		this.nodeMaker = nodeMaker;
	}

	/**
	 * Works out the mapping and making of PG relations.
	 */
	public RP getPGRelationMaker ()
	{
		return relationMaker;
	}

	@Autowired
	public void setPGRelationMaker ( RP relationMaker )
	{
		this.relationMaker = relationMaker;
	}

	public PGIndexer getPgIndexer ()
	{
		return pgIndexer;
	}

	/**
	 * @see PGIndexer for notes about Spring auto-wiring.  
	 */
	@Autowired ( required = false )	
	public void setPgIndexer ( PGIndexer pgIndexer )
	{
		this.pgIndexer = pgIndexer;
	}		
}
