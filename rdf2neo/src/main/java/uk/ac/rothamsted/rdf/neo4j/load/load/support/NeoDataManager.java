package uk.ac.rothamsted.rdf.neo4j.load.load.support;

import static info.marcobrandizi.rdfutils.jena.JenaGraphUtils.JENAUTILS;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb2.TDB2Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.machinezoo.noexception.throwing.ThrowingRunnable;
import com.machinezoo.noexception.throwing.ThrowingSupplier;

import info.marcobrandizi.rdfutils.jena.SparqlUtils;
import uk.ac.rothamsted.rdf.neo4j.idconvert.DefaultIri2IdConverter;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>7 Dec 2017</dd></dl>
 *
 */
public class NeoDataManager implements AutoCloseable
{
	private Function<String, String> labelIdConverter = new DefaultIri2IdConverter (); 
	private Function<String, String> propertyIdConverter = new DefaultIri2IdConverter (); 
	
	private String tdbPath = null;
	private Dataset dataSet = null;
	
	private LoadingCache<String, Query> queryCache; 
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	public NeoDataManager () 
	{
		wrapTask ( () -> 
		{
			this.tdbPath = Files.createTempDirectory ( "neo2rdf_tdb_" ).toAbsolutePath ().toString ();
			log.debug ( "Creating TDB on '{}'", tdbPath );
			this.dataSet = TDB2Factory.connectDataset ( tdbPath );
			
			Cache<String, Query> cache = CacheBuilder
				.newBuilder ()
				.maximumSize ( 1000 )
				.build ( new CacheLoader<String, Query> () 
				{
					@Override
					public Query load ( String sparql ) throws Exception {
						return QueryFactory.create ( sparql, Syntax.syntaxARQ );
					}
				});
			queryCache = (LoadingCache<String, Query>) cache;
		});
	}
	
	
	
	
	public Node getNode ( Resource nodeRes, String labelsSparql, String propsSparql )
	{
		Model model = this.dataSet.getDefaultModel ();
		
		QuerySolutionMap params = new QuerySolutionMap ();
		params.add ( "iri", nodeRes );
		Query qry = this.queryCache.getUnchecked ( labelsSparql );

		Node node = new Node ( nodeRes.getURI () );
		
		// The node's labels
		dataSet.begin ( ReadWrite.READ );
		QueryExecution qx = QueryExecutionFactory.create ( qry, model, params );
		qx.execSelect ().forEachRemaining ( row ->
			node.addLabel ( this.getNodeId ( row.get ( "label" ), this.getLabelIdConverter () ) )
		);
		dataSet.end ();
		
		// and the properties
		qry = this.queryCache.getUnchecked ( propsSparql );
		dataSet.begin ( ReadWrite.READ );
		qx = QueryExecutionFactory.create ( qry, model, params );
		qx.execSelect ().forEachRemaining ( row ->
		{
			String propName = this.getNodeId ( row.get ( "name" ), this.getPropertyIdConverter () );
			String propValue = JENAUTILS.literal2Value ( row.getLiteral ( "value" ) ).get ();
			node.addPropValue ( propName, propValue );
		});
		dataSet.end ();
		
		return node;
	}

	public Node getNode ( String nodeIri, String labelsSparql, String propsSparql )
	{
		Resource nodeRes = this.dataSet.getDefaultModel ().getResource ( nodeIri );
		return getNode ( nodeRes, labelsSparql, propsSparql ); 
	}

	
	private String getNodeId ( RDFNode node, Function<String, String> idConverter )
	{
		String id = node.canAs ( Resource.class )
			? node.as ( Resource.class ).getURI ()
			: node.asLiteral ().getLexicalForm ();
		
		if ( idConverter != null ) id = idConverter.apply ( id );
		
		return id;
	}
	
	public void processNodeIris ( String nodeIrisSparql, Consumer<Resource> action )
	{
		Dataset ds = this.dataSet;
		Model model = ds.getDefaultModel ();
		ds.begin ( ReadWrite.READ );
		SparqlUtils.select ( nodeIrisSparql, model )
		  .forEachRemaining ( qs -> 
			  action.accept ( qs.getResource ( "iri" ) )
		);
		ds.end ();
	}
	
	public Function<String, String> getLabelIdConverter ()
	{
		return labelIdConverter;
	}

	public void setLabelIdConverter ( Function<String, String> labelIdConverter )
	{
		this.labelIdConverter = labelIdConverter;
	}

	public Function<String, String> getPropertyIdConverter ()
	{
		return propertyIdConverter;
	}

	public void setPropertyIdConverter ( Function<String, String> propertyIdConverter )
	{
		this.propertyIdConverter = propertyIdConverter;
	}

	
	public Dataset getDataSet ()
	{
		return dataSet;
	}

	
	protected static void wrapTask ( ThrowingRunnable task )
	{
		wrapFun ( () -> { task.run (); return null; } ); 
	}

	
	protected static <V> V wrapFun ( ThrowingSupplier<V> fun )
	{
		try {
			return fun.get ();
		}
		catch ( IOException ex ) {
			throw new UncheckedIOException ( "I/O error while indexing imported data: " + ex.getMessage (), ex );
		}
		catch ( Exception ex ) {
			throw new RuntimeException ( "Error while indexing imported data: " + ex.getMessage (), ex );
		}
	}




	@Override
	public void close ()
	{
		if ( this.dataSet == null ) return;
		this.dataSet.close ();
	}
			
}
