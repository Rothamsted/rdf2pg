package uk.ac.rothamsted.kg.rdf2pg.pgmaker;

import java.util.function.Consumer;

import org.apache.jena.query.Dataset;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.ac.ebi.utils.collections.OptionsMap;
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
 * Note: We recommend that you subclass your specific version of this class with both
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
	 * 
	 * This does the job.
	 *  
	 * It invokes all of {@link #makeNodes(String, OptionsMap)}, {@link #makeRelations(String, OptionsMap)} and
	 * {@link #makeIndexes(String, OptionsMap)}, in this order, so that you've the relations linking referred
	 * nodes correctly.
	 * 
	 * This is not what is invoked by {@link MultiConfigPGMaker}, since that needs to invoke the stages mentioned 
	 * above separately, see details there. 
	 *  
	 * All of the stage methods mentioned above uses {@link #make(String, OptionsMap, Consumer)}.
	 * 
	 */
	@Override
	public void make ( String tdbPath, OptionsMap opts ) 
	{
		makeNodes ( tdbPath, opts );
		makeRelations ( tdbPath, opts );
		makeIndexes ( tdbPath, opts );
	}
	
	/**
	 * @see #make(String, OptionsMap)
	 */
	public void makeNodes ( String tdbPath, OptionsMap opts ) 
	{
		make ( tdbPath, opts, this::makeBodyNodes );
	}

	/**
	 * @see #make(String, OptionsMap)
	 */
	public void makeRelations ( String tdbPath, OptionsMap opts ) 
	{
		make ( tdbPath, opts, this::makeBodyRelations );
	}

	/**
	 * @see #make(String, OptionsMap)
	 */
	public void makeIndexes ( String tdbPath, OptionsMap opts ) 
	{
		make ( tdbPath, opts, this::makeBodyIndexes );
	}
	
	/**
	 * @see #make(String, OptionsMap, Consumer) 
	 */
	protected void makeBegin ( String tdbPath, OptionsMap opts )
	{
		RdfDataManager rdfMgr = this.getRdfDataManager ();
		rdfMgr.open ( tdbPath );
		Dataset ds = rdfMgr.getDataSet ();
		
		final String namePrefx = this.getCompNamePrefix ();
		
		Txn.executeRead ( ds, () -> 
			log.info ( "{}RDF source has about {} triple(s)", namePrefx, ds.getUnionModel().size () )
		);
	}

	/**
	 * @see #make(String, OptionsMap, Consumer) 
	 */
	protected void makeEnd ( OptionsMap opts )
	{
		log.info ( "{}RDF-PG conversion finished", getCompNamePrefix () );
	}
	
	/**
	 * The base method for all of the stage methods {@link #makeNodes(String, OptionsMap)},
	 * {@link #makeRelations(String, OptionsMap)}, {@link #makeIndexes(String, OptionsMap)}.
	 * 
	 * This invokes {@link #makeBegin(String, OptionsMap)}, then the stage method passed as
	 * parameter, then {@link #makeEnd(OptionsMap)}. 
	 * 
	 * The stage method is one of makeBodyXXX below, eg, {@link #makeBodyNodes(OptionsMap)}, which
	 * one is decided by the invokers mentioned above.
	 * 
	 */
	protected void make ( String tdbPath, OptionsMap opts, Consumer<OptionsMap> stage ) 
	{
		try {
			makeBegin ( tdbPath, opts );
			stage.accept ( opts );
			makeEnd ( opts );
		}
		catch ( Exception ex ) {
			ExceptionUtils.throwEx ( RuntimeException.class, ex, 
				"Error while running the RDF/PG maker: $cause"
			);
		}
	}

	/**
	 * Uses {@link #getPGNodeMaker()}
	 * @see #make(String, OptionsMap, Consumer)
	 */
	protected void makeBodyNodes ( OptionsMap opts )
	{
		this.getPGNodeMaker ().process ( this.getRdfDataManager (), opts );
	}

	/**
	 * Uses {@link #getPGNodeMaker()}
	 * @see #make(String, OptionsMap, Consumer)
	 */
	protected void makeBodyRelations ( OptionsMap opts )
	{
		this.getPGRelationMaker ().process ( this.getRdfDataManager (), opts );
	}

	/**
	 * Uses {@link #getPGNodeMaker()}
	 * @see #make(String, OptionsMap, Consumer)
	 */
	protected void makeBodyIndexes ( OptionsMap opts )
	{
		PGIndexer indexer = this.getPgIndexer ();
		if ( indexer != null )
		{
			indexer.setComponentName ( this.getComponentName () );
			indexer.index ();
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
