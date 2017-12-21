//package uk.ac.rothamsted.rdf.neo4j.load.load.support;
//
//import static info.marcobrandizi.rdfutils.jena.JenaGraphUtils.JENAUTILS;
//import static java.util.Spliterator.DISTINCT;
//import static java.util.Spliterator.IMMUTABLE;
//import static java.util.Spliterator.NONNULL;
//
//import java.io.IOException;
//import java.io.UncheckedIOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.Iterator;
//import java.util.NoSuchElementException;
//import java.util.Spliterator;
//import java.util.Spliterators;
//import java.util.function.Function;
//import java.util.stream.Stream;
//import java.util.stream.StreamSupport;
//
//import org.apache.jena.query.QuerySolution;
//import org.apache.jena.query.ResultSet;
//import org.apache.jena.rdf.model.RDFNode;
//import org.apache.jena.rdf.model.Resource;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.document.Field;
//import org.apache.lucene.document.Field.Store;
//import org.apache.lucene.document.StringField;
//import org.apache.lucene.document.TextField;
//import org.apache.lucene.index.DirectoryReader;
//import org.apache.lucene.index.IndexReader;
//import org.apache.lucene.index.IndexWriter;
//import org.apache.lucene.index.IndexWriterConfig;
//import org.apache.lucene.index.Term;
//import org.apache.lucene.queryparser.classic.QueryParser;
//import org.apache.lucene.search.IndexSearcher;
//import org.apache.lucene.search.Query;
//import org.apache.lucene.search.ScoreDoc;
//import org.apache.lucene.search.TopDocs;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.FSDirectory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.machinezoo.noexception.throwing.ThrowingRunnable;
//import com.machinezoo.noexception.throwing.ThrowingSupplier;
//
//import uk.ac.rothamsted.rdf.neo4j.idconvert.DefaultIri2IdConverter;
//
///**
// * TODO: comment me!
// *
// * @author brandizi
// * <dl><dt>Date:</dt><dd>7 Dec 2017</dd></dl>
// *
// */
//public class NeoDataManagerOld
//{
//	private Function<String, String> labelIdConverter = new DefaultIri2IdConverter (); 
//	private Function<String, String> propertyIdConverter = new DefaultIri2IdConverter (); 
//	
//	private Directory index;
//	private IndexWriter idxWriter;
//	private IndexSearcher idxSearcher;
//	private StandardAnalyzer analyzer = new StandardAnalyzer ();
//
//	protected Logger log = LoggerFactory.getLogger ( this.getClass () );
//	
//	public NeoDataManagerOld () 
//	{
//		wrapTask ( () -> 
//		{
//			Path tmpDirPath = Files.createTempDirectory ( "neo2rdf_idx_" );
//			log.debug ( "Creating index on '{}'", tmpDirPath.toAbsolutePath ().toString () );
//			index = FSDirectory.open ( tmpDirPath );
//		});
//	}
//
//	public void openIdxWriter ()
//	{
//		wrapTask ( () ->
//		{
//			if ( this.idxWriter != null ) return;
//			
//			IndexWriterConfig cfg = new IndexWriterConfig ( this.analyzer );
//			this.idxWriter = new IndexWriter ( index, cfg );
//		});
//	}
//
//	public void closeIdxWriter ()
//	{
//		wrapTask ( () -> 
//		{
//			if ( this.idxWriter == null ) return;
//			this.idxWriter.close ();
//			this.idxWriter = null;
//		});
//	}
//	
//	public void openIdxSearcher ()
//	{
//		wrapTask ( () ->
//		{
//			if ( this.idxSearcher != null ) return;
//			
//			IndexReader idxRdr = DirectoryReader.open ( this.index );
//			this.idxSearcher = new IndexSearcher ( idxRdr );
//		});
//	}
//
//	public void closeIdxSearcher ()
//	{
//		wrapTask ( () -> 
//		{
//			if ( this.idxSearcher == null ) return;
//			this.idxSearcher.getIndexReader ().close ();
//		});
//	}
//		
//	
//	protected ScoreDoc[] search ( QueryParser queryParser, String queryStr, ScoreDoc after, int n ) 
//	{
//		return wrapFun ( () ->
//		{
//			this.openIdxSearcher ();
//			Query q = queryParser.parse ( queryStr );
//			TopDocs topDocs = after == null  
//				? this.idxSearcher.search ( q, n )
//				: this.idxSearcher.searchAfter ( after, q, n );
//			return topDocs.scoreDocs;
//		});
//	}
//
//	protected ScoreDoc[] search ( String queryStr, ScoreDoc after, int n )
//	{
//		return search ( new QueryParser ( "iri", this.analyzer ), queryStr, after, n );
//	}
//
//	
//	
//	// TODO: Move to jutil
//	protected Iterator<ScoreDoc> searchAll ( QueryParser queryParser, String queryStr )
//	{
//		Iterator<ScoreDoc> itr = new Iterator<ScoreDoc>() 
//		{
//			private ScoreDoc[] buffer = null;
//			private int idx = -1;
//			private boolean hasFinished = false;
//			
//			@Override
//			public boolean hasNext ()
//			{
//				if ( this.hasFinished ) return false;
//				
//				if ( buffer == null ) {
//					buffer = search ( queryParser, queryStr, null, 1000000 );
//					if ( buffer == null || buffer.length == 0 ) { 
//						hasFinished = true;
//						return false;
//					}
//					idx = 0;
//				}
//				if ( idx >= buffer.length ) 
//				{
//					buffer = search ( queryParser, queryStr, buffer [ buffer.length - 1 ], 1000000 );
//					if ( buffer == null || buffer.length == 0 ) {
//						hasFinished = true;
//						return false;
//					} 
//					idx = 0;
//				}
//				return true;
//			}
//
//			@Override
//			public ScoreDoc next ()
//			{
//				if ( hasFinished ) throw new NoSuchElementException ( "Lucene Result Iterator has no more elements" );
//				if ( buffer == null || idx < 0 || idx >= buffer.length )
//					// First time or at the end of the current buffer and without any hasNext() call, let's try
//					// to call it for you
//					if ( !hasNext () ) throw new NoSuchElementException ( "Lucene Result Iterator has no more elements" );
//				
//				return buffer [ idx++ ];
//			}
//		}; // iterator
//		
//		return itr;
//	}
//	
//	protected Iterator<ScoreDoc> searchAll ( String queryStr )
//	{
//		return searchAll ( new QueryParser ( "iri", this.analyzer ), queryStr );
//	}
//	
//	public void indexNodeLabelRow ( QuerySolution row )
//	{
//		wrapTask ( () -> 
//		{
//			Document doc = new Document ();
//			doc.add ( new TextField ( "docType", "nodeLabel", Store.YES ) );
//
//			// TODO: check blank nodes? also, in the statements below
//			String iri = row.getResource ( "iri" ).toString ();
//			Field iriField = new TextField ( "iri", iri, Store.YES );
//			doc.add ( iriField );
//			doc.add ( new TextField ( 
//				"label", this.getNodeId ( row.get ( "label" ), this.getLabelIdConverter () ), Store.YES 
//			));
//			
//			this.idxWriter.addDocument ( doc );
//			
//			// The list of all available IDs, used to reconstruct nodes from its fields
//			this.indexNodeIri ( iri );
//		});
//	}
//	
//	public void indexNodeLabelRows ( ResultSet rs )  
//	{
//		wrapTask ( () -> {
//			rs.forEachRemaining ( this::indexNodeLabelRow );
//			this.idxWriter.commit ();
//		});
//	}
//	
//	
//
//	public void indexNodePropertyRow ( QuerySolution row )
//	{
//		wrapTask ( () -> 
//		{
//			Document doc = new Document ();
//			doc.add ( new TextField ( "docType", "nodeProperty", Store.YES ) );
//
//			// TODO: check blank nodes? also, in the statements below
//			String iri = row.getResource ( "iri" ).toString ();
//			Field iriField = new TextField ( "iri", iri, Store.YES );
//			doc.add ( iriField );
//			doc.add ( new TextField ( 
//				"name", this.getNodeId ( row.get ( "name" ), this.getPropertyIdConverter () ), Store.YES 
//			));
//			
//			// TODO Datatype conversions
//			doc.add ( new TextField ( "value",
//				JENAUTILS.literal2Value ( row.getLiteral ( "value" ) ).get (), 
//				Store.YES 
//			));
//			
//			this.idxWriter.addDocument ( doc );
//			this.indexNodeIri ( iri );
//		});
//	}
//	
//	protected void indexNodeIri ( String iri )
//	{
//		Document doc = new Document ();
//		doc.add ( new TextField ( "docType", "nodeIri", Store.YES ) );
//		doc.add ( new TextField ( "nodeIri", iri, Store.YES ) );
//		// It seems that only StringField wotk with the updateDocument() below :-(
//		doc.add ( new StringField ( "key", iri, Store.NO ) );
//		wrapTask ( () -> this.idxWriter.updateDocument ( new Term ( "key", iri ), doc ) );		
//	}
//	
//	
//	
//	
//	public void indexNodePropertyRows ( ResultSet rs )  
//	{
//		wrapTask ( () -> {
//			rs.forEachRemaining ( this::indexNodePropertyRow );
//			this.idxWriter.commit ();
//		});		
//	}	
//	
//	public Node getNode ( String iri )
//	{
//		return wrapFun ( () -> 
//		{
//			Node result = new Node ( iri );
//
//			// labels
//			Iterable<ScoreDoc> docs = () -> this.searchAll ( "docType:\"nodeLabel\" AND iri:\"" + iri + "\"" );
//			for ( ScoreDoc docRef: docs )
//			{
//				Document doc = this.idxSearcher.doc ( docRef.doc );
//				result.addLabel ( doc.get ( "label" ) );
//			}
//
//			// Properties
//			docs = () -> this.searchAll ( "docType:\"nodeProperty\" AND iri:\"" + iri + "\"" );
//			for ( ScoreDoc docRef: docs )
//			{
//				Document doc = this.idxSearcher.doc ( docRef.doc );
//				result.addPropValue ( doc.get ( "name" ), doc.get ( "value" ) );
//			}
//			
//			return result;
//		});
//	}
//
//
//	public Stream<String> getNodeIris ()
//	{
//		Iterator<ScoreDoc> docs = this.searchAll ( "docType:\"nodeIri\"" );
//		Spliterator<ScoreDoc> splDocs = Spliterators.spliteratorUnknownSize ( docs, DISTINCT | IMMUTABLE | NONNULL  );
//		
//		return StreamSupport.stream ( splDocs, false )
//		  .map ( docRef -> 
//				wrapFun ( () -> (String) this.idxSearcher.doc ( docRef.doc ).get ( "nodeIri" ) ) 
//		);
//	}
//	
//	private String getNodeId ( RDFNode node, Function<String, String> idConverter )
//	{
//		String id = node.canAs ( Resource.class )
//			? node.as ( Resource.class ).getURI ()
//			: node.asLiteral ().getLexicalForm ();
//		
//		if ( idConverter != null ) id = idConverter.apply ( id );
//		
//		return id;
//	}
//	
//	
//	public Function<String, String> getLabelIdConverter ()
//	{
//		return labelIdConverter;
//	}
//
//	public void setLabelIdConverter ( Function<String, String> labelIdConverter )
//	{
//		this.labelIdConverter = labelIdConverter;
//	}
//
//	public Function<String, String> getPropertyIdConverter ()
//	{
//		return propertyIdConverter;
//	}
//
//	public void setPropertyIdConverter ( Function<String, String> propertyIdConverter )
//	{
//		this.propertyIdConverter = propertyIdConverter;
//	}
//
//	
//	
//	protected static void wrapTask ( ThrowingRunnable task )
//	{
//		wrapFun ( () -> { task.run (); return null; } ); 
//	}
//
//	
//	protected static <V> V wrapFun ( ThrowingSupplier<V> fun )
//	{
//		try {
//			return fun.get ();
//		}
//		catch ( IOException ex ) {
//			throw new UncheckedIOException ( "I/O error while indexing imported data: " + ex.getMessage (), ex );
//		}
//		catch ( Exception ex ) {
//			throw new RuntimeException ( "Error while indexing imported data: " + ex.getMessage (), ex );
//		}
//	}
//	
//	
//	@Override
//	protected void finalize () throws Throwable
//	{
//		this.closeIdxWriter ();
//		this.closeIdxSearcher ();
//		super.finalize ();
//	}
//	
//}
