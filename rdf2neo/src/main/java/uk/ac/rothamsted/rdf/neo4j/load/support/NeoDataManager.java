package uk.ac.rothamsted.rdf.neo4j.load.support;

import static info.marcobrandizi.rdfutils.jena.JenaGraphUtils.JENAUTILS;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.io.FileUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.Syntax;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.tdb2.DatabaseMgr;
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
	private Function<String, String> relationIdConverter = new DefaultIri2IdConverter ();
	
	private static String configTdbPath = System.getProperty ( "java.io.tmpdir" ) + "neo2rdf_tdb";
	
	private String tdbPath;
	private Dataset dataSet = null;
	
	/**
	 * TODO: move the query caching to {@link SparqlUtils}.
	 */
	private LoadingCache<String, Query> queryCache; 
	
	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
	
	public NeoDataManager () 
	{
		wrapTask ( () -> 
		{
			log.debug ( "Creating TDB on '{}'", tdbPath );
			
			this.tdbPath = configTdbPath;
			
			// Clean-up. TODO: should it be optional? 
			File tdbDir = new File ( tdbPath );
			FileUtils.deleteDirectory ( tdbDir  );
			tdbDir.mkdir ();
			
			this.dataSet = TDBFactory.createDataset ( tdbPath );
						
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
	
	
	
	
	public CyNode getCyNode ( Resource nodeRes, String labelsSparql, String propsSparql )
	{
		Model model = this.dataSet.getDefaultModel ();
		
		QuerySolutionMap params = new QuerySolutionMap ();
		params.add ( "iri", nodeRes );
		Query qry = this.queryCache.getUnchecked ( labelsSparql );

		CyNode cyNode = new CyNode ( nodeRes.getURI () );
		
		// The node's labels
		Function<String, String> labelIdConverter = this.getLabelIdConverter ();
		boolean wasInTnx = dataSet.isInTransaction ();
		if ( !wasInTnx ) dataSet.begin ( ReadWrite.READ );
		try {
			QueryExecution qx = QueryExecutionFactory.create ( qry, model, params );
			qx.execSelect ().forEachRemaining ( row ->
				cyNode.addLabel ( this.getCypherId ( row.get ( "label" ), labelIdConverter ) )
			);
		}
		finally {
			if ( !wasInTnx && dataSet.isInTransaction () ) dataSet.end ();
		}
		
		// and the properties
		this.addCypherProps ( cyNode, propsSparql );
		
		return cyNode;
	}

	public CyNode getCyNode ( String nodeIri, String labelsSparql, String propsSparql )
	{
		Resource nodeRes = this.dataSet.getDefaultModel ().getResource ( nodeIri );
		return getCyNode ( nodeRes, labelsSparql, propsSparql ); 
	}

	
	private String getCypherId ( RDFNode node, Function<String, String> idConverter )
	{
		String id = node.canAs ( Resource.class )
			? node.as ( Resource.class ).getURI ()
			: node.asLiteral ().getLexicalForm ();
		
		if ( idConverter != null ) id = idConverter.apply ( id );
		
		return id;
	}
	
	private void addCypherProps ( CypherEntity cyEnt, String propsSparql )
	{
		Model model = this.dataSet.getDefaultModel ();
		
		QuerySolutionMap params = new QuerySolutionMap ();
		params.add ( "iri", model.getResource ( cyEnt.getIri () ) );

		Query qry = this.queryCache.getUnchecked ( propsSparql );
		Function<String, String> propIdConverter = this.getPropertyIdConverter ();
		
		boolean wasInTnx = dataSet.isInTransaction ();
		if ( !wasInTnx ) dataSet.begin ( ReadWrite.READ );
		try
		{
			QueryExecution qx = QueryExecutionFactory.create ( qry, model, params );
			qx.execSelect ().forEachRemaining ( row ->
			{
				String propName = this.getCypherId ( row.get ( "name" ), propIdConverter );
				String propValue = JENAUTILS.literal2Value ( row.getLiteral ( "value" ) ).get ();
				cyEnt.addPropValue ( propName, propValue );
			});
		}
		finally {
			if ( !wasInTnx && dataSet.isInTransaction () ) dataSet.end ();
		}
	}
	
	
	public long processNodeIris ( String nodeIrisSparql, long offset, long limit, Consumer<Resource> action )
	{
		Dataset ds = this.dataSet;
		Model model = ds.getDefaultModel ();

		// We cannot cache this because it's stateful and hence it cannot be shared
		Query nodeIrisQuery = QueryFactory.create ( nodeIrisSparql, Syntax.syntaxARQ );
		nodeIrisQuery.setLimit ( limit );
		nodeIrisQuery.setOffset ( offset );			
		
		ds.begin ( ReadWrite.READ );
		try {
			QueryExecution qx = QueryExecutionFactory.create ( nodeIrisQuery, model );
			ResultSet cursor = qx.execSelect ();
			if ( !cursor.hasNext () ) return -1;
			do {
				QuerySolution qs = cursor.next ();
				action.accept ( qs.getResource ( "iri" ) );
			}
			while ( cursor.hasNext () );
			return offset + limit;
		}
		finally {
			if ( ds.isInTransaction () ) ds.end ();
		}
	}
	
	
	
	public CyRelation getCyRelation ( QuerySolution relRow )
	{
		Resource relRes = relRow.get ( "iri" ).asResource ();
		CyRelation cyRelation = new CyRelation ( relRes.getURI () );
		
		// The node's labels
		cyRelation.setType ( this.getCypherId ( relRow.get ( "type" ), this.getRelationIdConverter () ) );
		cyRelation.setFromIri ( relRow.get ( "fromIri" ).asResource ().getURI () );
		cyRelation.setToIri ( relRow.get ( "toIri" ).asResource ().getURI () );
				
		return cyRelation;
	}
	
	public void setCyRelationProps ( CyRelation cyRelation, String propsSparql )
	{
		this.addCypherProps ( cyRelation, propsSparql );
	}
	
	public long processRelationIris ( String relationIrisSparql, long offset, long limit, Consumer<QuerySolution> action )
	{
		Dataset ds = this.dataSet;
		Model model = ds.getDefaultModel ();

		// We cannot cache this because it's stateful and hence it cannot be shared
		Query relIrisQuery = QueryFactory.create ( relationIrisSparql, Syntax.syntaxARQ );
		relIrisQuery.setLimit ( limit );
		relIrisQuery.setOffset ( offset );
		
		boolean wasInTnx = ds.isInTransaction ();
		if ( !wasInTnx ) ds.begin ( ReadWrite.READ );
		try {
			QueryExecution qx = QueryExecutionFactory.create ( relIrisQuery, model );
			ResultSet cursor = qx.execSelect ();
			if ( !cursor.hasNext () ) return -1;
			do {
				QuerySolution qs = cursor.next ();
				action.accept ( qs );
			}
			while ( cursor.hasNext () );
			return offset + limit;
		}
		finally {
			if ( !wasInTnx && ds.isInTransaction () ) ds.end ();
		}
	}
	
	
	public Function<String, String> getLabelIdConverter ()
	{
		return labelIdConverter;
	}

	public void setLabelIdConverter ( Function<String, String> labelIdConverter )
	{
		this.labelIdConverter = labelIdConverter;
	}
	
	public Function<String, String> getRelationIdConverter ()
	{
		return relationIdConverter;
	}

	public void setRelationIdConverter ( Function<String, String> relationIdConverter )
	{
		this.relationIdConverter = relationIdConverter;
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

	public String getTdbPath ()
	{
		return tdbPath;
	}

	public static void setConfigTdbPath ( String configuredTdbPath )
	{
		NeoDataManager.configTdbPath = configuredTdbPath;
	}

	@Override
	public void close ()
	{
		if ( this.dataSet == null ) return;
		this.dataSet.close ();
		this.dataSet = null;
	}
			
}
