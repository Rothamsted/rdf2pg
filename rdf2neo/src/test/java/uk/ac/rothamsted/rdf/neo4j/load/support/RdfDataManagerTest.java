package uk.ac.rothamsted.rdf.neo4j.load.support;

import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Model;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import info.marcobrandizi.rdfutils.namespaces.NamespaceUtils;
import uk.ac.ebi.utils.io.IOUtils;

/**
 * A few tests for the {@link RdfDataManager}.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>8 Dec 2017</dd></dl>
 *
 */
public class RdfDataManagerTest
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
	
	
	private static RdfDataManager rdfMgr = new RdfDataManager ();
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
		
	public static final String TDB_PATH = "target/NeoDataManagerTest_tdb";
	
	/**
	 * Loads the test TDB used in this class with a bounch of RDF data.
	 */
	@BeforeClass
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
	
	@AfterClass
	public static void closeDataMgr ()
	{
		rdfMgr.close ();
	}
	
	
	@Test
	public void testNodes ()
	{
		log.info ( "Verifying Nodes" );

		Dataset ds = rdfMgr.getDataSet ();
		Model m = ds.getDefaultModel ();
		
		CyNode cyNode = rdfMgr.getCyNode ( m.getResource ( iri ( "ex:1" ) ), SPARQL_NODE_LABELS, SPARQL_NODE_PROPS );
		assertNotNull ( "CyNode 1 not found!", cyNode );
		log.info ( "Got node 1" );

		assertEquals ( "CyNode 1's Label not found!", 1, cyNode.getLabels ().size () );
		assertEquals ( "CyNode 1's Label not found!", "TestNode", cyNode.getLabels ().iterator ().next () );
		
		assertEquals ( "CyNode 1's wrong properties count!", 2, cyNode.getProperties ().size () );
		assertEquals ( "CyNode 1's prop1 not found!", "10.0", cyNode.getPropValue ( "attrib1" ) );
		assertEquals ( "CyNode 1's prop2 not found!", "a string", cyNode.getPropValue ( "attrib2" ) );
		
		log.info ( "End" );
	}
	
	
	@Test
	public void testRelations () throws Exception
	{
		log.info ( "Verifying Relations" );

		List<CyRelation> cyRelations = new ArrayList<> ();
		rdfMgr.processRelationIris ( SPARQL_REL_TYPES, 
			row -> 
			{
				CyRelation rel = rdfMgr.getCyRelation ( row );
				rdfMgr.setCyRelationProps ( rel, SPARQL_REL_PROPS );
				cyRelations.add ( rel );
		});
		
		CyRelation cyRelation = cyRelations.stream ()
		.filter ( rel -> 
			"relatedTo".equals ( rel.getType () ) 
			&& iri ( "ex:1" ).equals ( rel.getFromIri () ) 
			&& iri ( "ex:2" ).equals ( rel.getToIri () ) 
		)
		.findAny ()
		.orElse ( null );
		assertNotNull ( "{ex:1 ex:relatedTo ex:2} not found!", cyRelation );
		
		cyRelation = cyRelations.stream ()
		.filter ( rel -> 
			"derivedFrom".equals ( rel.getType () ) 
			&& iri ( "ex:3" ).equals ( rel.getFromIri () ) 
			&& iri ( "ex:1" ).equals ( rel.getToIri () ) 
		)
		.findAny ()
		.orElse ( null );
		assertNotNull ( "{ex:3 ex:derivedFrom ex:1} not found!", cyRelation );

		cyRelation = cyRelations.stream ()
		.filter ( rel -> iri ( "ex:2_3" ).equals ( rel.getIri () ) )
		.findAny ()
		.orElse ( null );
		assertNotNull ( "reified relation not found!", cyRelation );
		assertEquals ( "reified relation's type wrong!", "relatedTo", cyRelation.getType () );
		assertEquals ( "reified relation's fromIri wrong!", iri ( "ex:2" ), cyRelation.getFromIri () );
		assertEquals ( "reified relation's toIri wrong!", iri ( "ex:3" ),  cyRelation.getToIri () );		
		assertEquals ( "reified relation, wrong properties count!", 1, cyRelation.getProperties ().size () );
		
		Set<String> values = cyRelation.getPropValues ( "note" );
		Set<String> refValues = new HashSet<> ( Arrays.asList ( new String[] { "Reified Relation", "Another Note" } ) ) ;
		assertTrue ( 
			"reified relation, wrong property value for 'note'!", 
			Sets.difference ( values, refValues ).isEmpty () 
		);
		
		log.info ( "End" );
	}	
	
}
