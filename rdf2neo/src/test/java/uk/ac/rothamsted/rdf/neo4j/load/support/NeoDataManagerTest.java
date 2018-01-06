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
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import info.marcobrandizi.rdfutils.namespaces.NamespaceUtils;
import uk.ac.ebi.utils.io.IOUtils;
import uk.ac.rothamsted.rdf.neo4j.load.support.NeoDataManager;
import uk.ac.rothamsted.rdf.neo4j.load.support.Node;
import uk.ac.rothamsted.rdf.neo4j.load.support.Relation;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>8 Dec 2017</dd></dl>
 *
 */
public class NeoDataManagerTest
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
	
	
	private static NeoDataManager dataMgr = new NeoDataManager ();
	private Logger log = LoggerFactory.getLogger ( this.getClass () );
		
	
	@BeforeClass
	public static void initData () throws IOException
	{
		Dataset ds = dataMgr.getDataSet ();
		Model m = ds.getDefaultModel ();
		ds.begin ( ReadWrite.WRITE );
		try 
		{
			if ( m.size () > 0 ) return;
			m.read ( IOUtils.openResourceReader ( "test_data.ttl" ), null, "TURTLE" );
			ds.commit ();
		}
		finally { 
			ds.end ();
		}
	}
	
	
	@Test
	public void testNodes ()
	{
		log.info ( "Verifying Nodes" );

		Dataset ds = dataMgr.getDataSet ();
		Model m = ds.getDefaultModel ();
		
		Node node = dataMgr.getNode ( m.getResource ( iri ( "ex:1" ) ), SPARQL_NODE_LABELS, SPARQL_NODE_PROPS );
		assertNotNull ( "Node 1 not found!", node );
		log.info ( "Got node 1" );

		assertEquals ( "Node 1's Label not found!", 1, node.getLabels ().size () );
		assertEquals ( "Node 1's Label not found!", "TestNode", node.getLabels ().iterator ().next () );
		
		assertEquals ( "Node 1's wrong properties count!", 2, node.getProperties ().size () );
		assertEquals ( "Node 1's prop1 not found!", "10.0", node.getPropValue ( "attrib1" ) );
		assertEquals ( "Node 1's prop2 not found!", "a string", node.getPropValue ( "attrib2" ) );
		
		log.info ( "End" );
	}
	
	
	@Test
	public void testRelations () throws Exception
	{
		log.info ( "Verifying Relations" );

		List<Relation> relations = new ArrayList<> ();
		dataMgr.processRelationIris ( SPARQL_REL_TYPES, 0, (long) 1E6, 
			row -> 
			{
				Relation rel = dataMgr.getRelation ( row );
				dataMgr.setRelationProps ( rel, SPARQL_REL_PROPS );
				relations.add ( rel );
		});
		
		Relation relation = relations.stream ()
		.filter ( rel -> 
			"relatedTo".equals ( rel.getType () ) 
			&& iri ( "ex:1" ).equals ( rel.getFromIri () ) 
			&& iri ( "ex:2" ).equals ( rel.getToIri () ) 
		)
		.findAny ()
		.orElse ( null );
		assertNotNull ( "{ex:1 ex:relatedTo ex:2} not found!", relation );
		
		relation = relations.stream ()
		.filter ( rel -> 
			"derivedFrom".equals ( rel.getType () ) 
			&& iri ( "ex:3" ).equals ( rel.getFromIri () ) 
			&& iri ( "ex:1" ).equals ( rel.getToIri () ) 
		)
		.findAny ()
		.orElse ( null );
		assertNotNull ( "{ex:3 ex:derivedFrom ex:1} not found!", relation );

		relation = relations.stream ()
		.filter ( rel -> iri ( "ex:2_3" ).equals ( rel.getIri () ) )
		.findAny ()
		.orElse ( null );
		assertNotNull ( "reified relation not found!", relation );
		assertEquals ( "reified relation's type wrong!", "relatedTo", relation.getType () );
		assertEquals ( "reified relation's fromIri wrong!", iri ( "ex:2" ), relation.getFromIri () );
		assertEquals ( "reified relation's toIri wrong!", iri ( "ex:3" ),  relation.getToIri () );		
		assertEquals ( "reified relation, wrong properties count!", 1, relation.getProperties ().size () );
		
		Set<String> values = relation.getPropValues ( "note" );
		Set<String> refValues = new HashSet<> ( Arrays.asList ( new String[] { "Reified Relation", "Another Note" } ) ) ;
		assertTrue ( 
			"reified relation, wrong property value for 'note'!", 
			Sets.difference ( values, refValues ).isEmpty () 
		);
		
		log.info ( "End" );
	}	
	
	
	public static NeoDataManager getDataMgr ()
	{
		return dataMgr;
	}
	
}
