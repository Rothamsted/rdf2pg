package uk.ac.rothamsted.rdf.pg.load.support.rdf;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.system.Txn;


import info.marcobrandizi.rdfutils.namespaces.NamespaceUtils;
import uk.ac.ebi.utils.io.IOUtils;

/**
 * A base for testing {@link RdfDataManager}, which is also an initialiser of RDF data for many
 * RDF2PG tests (that's why this is here and not in the test subtree).
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>8 Dec 2017</dd></dl>
 *
 */
public class RdfDataManagerTestBase
{
	static 
	{
		try
		{
			// This must go before the SPARQL constant definitions below, since they depend on it 
			NamespaceUtils.registerNs ( "ex", "http://www.example.com/res/" );
			
			SPARQL_NODE_LABELS = IOUtils.readResource ( "test_node_labels.sparql" );
			SPARQL_NODE_PROPS = IOUtils.readResource ( "test_node_props.sparql" );
			
			SPARQL_REL_TYPES = IOUtils.readResource ( "test_rel_types.sparql" );
			SPARQL_REL_PROPS = IOUtils.readResource ( "test_rel_props.sparql" );
		}
		catch ( IOException ex ) {
			throw new UncheckedIOException ( "Internal error: " + ex.getMessage (), ex );
		} 
	}

	public final static String SPARQL_NODE_LABELS;
	public final static String SPARQL_NODE_PROPS;
	
	public final static String SPARQL_REL_TYPES;
	public final static String SPARQL_REL_PROPS;
	
	protected static RdfDataManager rdfMgr = new RdfDataManager ();
		
	public static final String TDB_PATH = "target/NeoDataManagerTest_tdb";
	
	/**
	 * Loads the test TDB used in this class with a bounch of RDF data.
	 */
	public static void initData ()
	{
		rdfMgr.open ( TDB_PATH );
		Dataset ds = rdfMgr.getDataSet ();
		Model m = ds.getDefaultModel ();
		ds.begin ( ReadWrite.WRITE );
		try 
		{
			//if ( m.size () > 0 ) return;
			m.read ( IOUtils.openResourceReader ( "test_data.ttl" ), null, "TURTLE" );
			ds.commit ();
		}
		catch ( Exception ex ) {
			ds.abort ();
			throw new RuntimeException ( "Test error: " + ex.getMessage (), ex );
		}
		finally { 
			ds.end ();
		}
	}
	
	/** 
	 * Initialize a test dataset based on DBpedia 
	 * Brought here from test classes used for loader testing (*LoaderIT)
	 */
	
	public static void initDBpediaDataSet ()
	{
		try (
			RdfDataManager rdfMgr = new RdfDataManager ( RdfDataManagerTestBase.TDB_PATH );
	  )
		{
			Dataset ds = rdfMgr.getDataSet ();
			for ( String ttlPath: new String [] { "dbpedia_places.ttl", "dbpedia_people.ttl" } )
			Txn.executeWrite ( ds, () -> 
				ds.getDefaultModel ().read ( 
					"file:target/test-classes/" + ttlPath, 
					null, 
					"TURTLE" 
			));
		}	
	}
	
	public static void closeDataMgr ()
	{
		rdfMgr.close ();
	}	
	
}
