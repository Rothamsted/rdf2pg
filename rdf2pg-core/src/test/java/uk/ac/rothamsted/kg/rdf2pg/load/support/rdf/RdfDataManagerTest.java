package uk.ac.rothamsted.kg.rdf2pg.load.support.rdf;

import static info.marcobrandizi.rdfutils.namespaces.NamespaceUtils.iri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.query.Dataset;
import org.apache.jena.rdf.model.Model;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import uk.ac.rothamsted.kg.rdf2pg.load.support.entities.PGNode;
import uk.ac.rothamsted.kg.rdf2pg.load.support.entities.PGRelation;
import uk.ac.rothamsted.kg.rdf2pg.load.support.rdf.DataTestUtils;
import uk.ac.rothamsted.kg.rdf2pg.load.support.rdf.RdfDataManager;

/**
 * A few tests for the {@link RdfDataManager}.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>8 Dec 2017</dd></dl>
 *
 */
public class RdfDataManagerTest
{
	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	private static RdfDataManager rdfMgr = new RdfDataManager ( DataTestUtils.TDB_PATH );
	
	/**
	 * Loads the test TDB used in this class with a bounch of RDF data.
	 */
	@BeforeClass
	public static void initData ()
	{
		DataTestUtils.initData ();
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
		
		PGNode pgNode = rdfMgr.getPGNode ( m.getResource ( iri ( "ex:1" ) ), DataTestUtils.SPARQL_NODE_LABELS, DataTestUtils.SPARQL_NODE_PROPS );
		assertNotNull ( "PGNode 1 not found!", pgNode );
		log.info ( "Got node 1" );

		assertEquals ( "PGNode 1's Label not found!", 1, pgNode.getLabels ().size () );
		assertEquals ( "PGNode 1's Label not found!", "TestNode", pgNode.getLabels ().iterator ().next () );
		
		assertEquals ( "PGNode 1's wrong properties count!", 2, pgNode.getProperties ().size () );
		assertEquals ( "PGNode 1's prop1 not found!", "10.0", pgNode.getPropValue ( "attrib1" ) );
		assertEquals ( "PGNode 1's prop2 not found!", "a string", pgNode.getPropValue ( "attrib2" ) );
		
		log.info ( "End" );
	}
	
	
	@Test
	public void testRelations () throws Exception
	{
		log.info ( "Verifying Relations" );

		List<PGRelation> pgRelations = new ArrayList<> ();
		rdfMgr.processRelationIris ( DataTestUtils.SPARQL_REL_TYPES, 
			row -> 
			{
				PGRelation rel = rdfMgr.getPGRelation ( row );
				rdfMgr.setPGRelationProps ( rel, DataTestUtils.SPARQL_REL_PROPS );
				pgRelations.add ( rel );
		});
		
		PGRelation pgRelation = pgRelations.stream ()
		.filter ( rel -> 
			"relatedTo".equals ( rel.getType () ) 
			&& iri ( "ex:1" ).equals ( rel.getFromIri () ) 
			&& iri ( "ex:2" ).equals ( rel.getToIri () ) 
		)
		.findAny ()
		.orElse ( null );
		assertNotNull ( "{ex:1 ex:relatedTo ex:2} not found!", pgRelation );
		
		pgRelation = pgRelations.stream ()
		.filter ( rel -> 
			"derivedFrom".equals ( rel.getType () ) 
			&& iri ( "ex:3" ).equals ( rel.getFromIri () ) 
			&& iri ( "ex:1" ).equals ( rel.getToIri () ) 
		)
		.findAny ()
		.orElse ( null );
		assertNotNull ( "{ex:3 ex:derivedFrom ex:1} not found!", pgRelation );

		pgRelation = pgRelations.stream ()
		.filter ( rel -> iri ( "ex:2_3" ).equals ( rel.getIri () ) )
		.findAny ()
		.orElse ( null );
		assertNotNull ( "reified relation not found!", pgRelation );
		assertEquals ( "reified relation's type wrong!", "relatedTo", pgRelation.getType () );
		assertEquals ( "reified relation's fromIri wrong!", iri ( "ex:2" ), pgRelation.getFromIri () );
		assertEquals ( "reified relation's toIri wrong!", iri ( "ex:3" ),  pgRelation.getToIri () );		
		assertEquals ( "reified relation, wrong properties count!", 1, pgRelation.getProperties ().size () );
		
		Set<String> values = pgRelation.getPropValues ( "note" );
		Set<String> refValues = new HashSet<> ( Arrays.asList ( new String[] { "Reified Relation", "Another Note" } ) ) ;
		assertTrue ( 
			"reified relation, wrong property value for 'note'!", 
			Sets.difference ( values, refValues ).isEmpty () 
		);
		
		log.info ( "End" );
	}	
	
}
