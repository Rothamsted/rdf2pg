package uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.rdf;

import static info.marcobrandizi.rdfutils.jena.JenaGraphUtils.JENAUTILS;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import info.marcobrandizi.rdfutils.jena.TDBEndPointHelper;
import uk.ac.rothamsted.kg.rdf2pg.idconvert.DefaultIri2IdConverter;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGNodeHandler;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGNodeMakeProcessor;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.PGRelationHandler;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.entities.PGEntity;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.entities.PGNode;
import uk.ac.rothamsted.kg.rdf2pg.pgmaker.support.entities.PGRelation;

/**
 * <h1>The RDF source data manager.</h1> 
 * 
 * <p>This manages the input Jena TDB store for the Neo4J conversion operations. an instance of this class
 * is associated to a file path where a TDB sits and various operations provide access to its RDF data.</p> 
 *  
 * <p>TODO: rename to something like TDBManager.</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>7 Dec 2017</dd></dl>
 *
 * 
 */
@Component
public class RdfDataManager extends TDBEndPointHelper
{
	private Function<String, String> pgNodeLabelIdConverter = new DefaultIri2IdConverter ();
	private Function<String, String> pgPropertyIdConverter = new DefaultIri2IdConverter (); 
	private Function<String, String> pgRelationIdConverter = new DefaultIri2IdConverter ();
		
	public RdfDataManager () {
	}

	public RdfDataManager ( String tdbPath ) {
		super ( tdbPath );
	}

	/**
	 * Uses the underlining TDB and mapping queries to create a new {@link PGNode} instance.
	 * 
	 * @param nodeRes the RDF/Jena resource correspnding to the Cypher node. This provides the ?iri paramter in the queries below.
	 * @param labelsSparql the node labels query, which is usually taken from {@link PGNodeHandler#getLabelsSparql()}.
	 * @param propsSparql the node properties query, which is usually taken from {@link PGNodeHandler#getNodePropsSparql()}.
	 */
	public PGNode getPGNode ( Resource nodeRes, String labelsSparql, String propsSparql )
	{
		ensureOpen ();
		
		QuerySolutionMap params = new QuerySolutionMap ();
		params.add ( "iri", nodeRes );

		PGNode pgNode = new PGNode ( nodeRes.getURI () );
		
		// The node's labels
		// If this is omitted, nodes will get the default label only.
		if ( labelsSparql != null )
		{
			Function<String, String> labelIdConverter = this.getPGNodeLabelIdConverter ();
						
			this.processSelect (
				labelsSparql,
				row -> pgNode.addLabel ( this.getPGId ( row.get ( "label" ), labelIdConverter ) ),
				params
			);
		}
		
		// and the properties
		this.addPGProps ( pgNode, propsSparql );
		
		return pgNode;
		
	} // getPGNode()

	/**
	 * Just a variant of {@link #getPGNode(Resource, String, String)}.
	 */
	public PGNode getPGNode ( String nodeIri, String labelsSparql, String propsSparql )
	{
		ensureOpen ();
		Resource nodeRes = this.getDataSet().getUnionModel().getResource ( nodeIri );
		return getPGNode ( nodeRes, labelsSparql, propsSparql ); 
	}

	/**
	 * Gets a Cypher ID by applying an {@link DefaultIri2IdConverter ID conversion function} to an IRI taken from a 
	 * {@link Resource} RDF/Jena node, or to a lexical value taken from a {@link Literal} RDF/Jena node. 
	 * 
	 * Helper method used in other methods in this class.
	 */
	public String getPGId ( RDFNode node, Function<String, String> idConverter )
	{
		if ( node == null ) return null;
		
		String id = node.canAs ( Resource.class )
			? node.as ( Resource.class ).getURI ()
			: node.asLiteral ().getLexicalForm ();
					
		if ( idConverter != null ) id = idConverter.apply ( id );
		
		return id;
	}
	
	/**
	 * Take an existing {@link PGEntity} and adds the properties that can be mapped from the underlining TDB by means 
	 * of a property query, like {@link PGNodeHandler#getNodePropsSparql()}, or 
	 * {@link PGNodeHandler#getRelationPropsSparql()}.
	 * 
	 * It doesn't do anything if the query is null.
	 * 
	 */
	protected void addPGProps ( PGEntity cyEnt, String propsSparql )
	{
		ensureOpen ();		
		Dataset dataSet = this.getDataSet ();
		
		QuerySolutionMap params = new QuerySolutionMap ();
		params.add ( "iri", dataSet.getUnionModel().getResource ( cyEnt.getIri () ) );

		// It may be omitted, if you don't have any property except the IRI.
		if ( propsSparql == null ) return;
		
		Function<String, String> propIdConverter = this.getPGPropertyIdConverter ();
		
		this.processSelect ( 
			propsSparql,
			row ->
			{
				String propName = this.getPGId ( row.get ( "name" ), propIdConverter );
				if ( propName == null ) throw new IllegalArgumentException ( 
					"Null property name for " + cyEnt.getIri () 
				);
				
				String propValue = JENAUTILS.literal2Value ( row.getLiteral ( "value" ) ).get ();
				cyEnt.addPropValue ( propName, propValue );
			},
			params
		);
	} // addPGProps()
	
	/**
	 * Does something with the results coming from {@link PGNodeMakeProcessor#getNodeIrisSparql() node IRI query}.
	 * This method is an helper that contains common operations like transaction markers, logging etc.
	 * 
	 */
	public long processNodeIris ( String nodeIrisSparql, Consumer<Resource> action )
	{
		return this.processSelect ( "processNodeIris()", nodeIrisSparql, row -> action.accept ( row.getResource ( "iri" ) ) );
	}
	
	
	/**
	 * Similarly to {@link #getPGNode(Resource, String, String)}, uses a binding (i.e., row) from a 
	 * {@link PGRelationHandler#getRelationTypesSparql() relation type query} and creates a new {@link PGRelation}
	 * with the RDF mapped data.
	 */
	public PGRelation getPGRelation ( QuerySolution relRow )
	{		
		Resource relRes = relRow.get ( "iri" ).asResource ();
		PGRelation pgRelation = new PGRelation ( relRes.getURI () );
		
		pgRelation.setType ( this.getPGId ( relRow.get ( "type" ), this.getPGRelationTypeIdConverter () ) );

		pgRelation.setFromIri ( relRow.get ( "fromIri" ).asResource ().getURI () );
		pgRelation.setToIri ( relRow.get ( "toIri" ).asResource ().getURI () );
				
		return pgRelation;
	}
	
	/**
	 * Similarly to {@link #addPGProps(PGEntity, String)}, takes a {@link PGRelation} and adds the properties that
	 * can be mapped via {@link PGRelationHandler#getRelationPropsSparql() relation property query}.
	 * 
	 */
	public void setPGRelationProps ( PGRelation cyRelation, String propsSparql )
	{
		this.addPGProps ( cyRelation, propsSparql );
	}
	
	/**
	 * Similarly to {@link #processNodeIris(String, Consumer)}, does something with the results from a 
	 * {@link PGRelationHandler#getRelationTypesSparql() relation types query}.
	 * 
	 */
	public long processRelationIris ( String relationIrisSparql, Consumer<QuerySolution> action )
	{
		return processSelect ( "processRelationIris()", relationIrisSparql, action );
	}
	
	/** 
	 * Methods like {@link #getPGNode(Resource, String, String)} use this {@link DefaultIri2IdConverter ID} converter to 
	 * get IDs for Cypher node labels from RDF IRIs (or even literal).
	 * 
	 */
	public Function<String, String> getPGNodeLabelIdConverter ()
	{
		return pgNodeLabelIdConverter;
	}

	@Autowired ( required = false ) @Qualifier ( "nodeLabelIdConverter" )
	public void setPGNodeLabelIdConverter ( Function<String, String> labelIdConverter )
	{
		this.pgNodeLabelIdConverter = labelIdConverter;
	}
	
	/**
	 * Similarly to {@link #getPGNodeLabelIdConverter()}, this is used to get a relation type string from 
	 * a relation type IRI (or even literal). 
	 * 
	 */
	public Function<String, String> getPGRelationTypeIdConverter ()
	{
		return pgRelationIdConverter;
	}

	@Autowired ( required = false )	@Qualifier ( "relationIdConverter" )
	public void setPGRelationTypeIdConverter ( Function<String, String> relationIdConverter )
	{
		this.pgRelationIdConverter = relationIdConverter;
	}



	/**
	 * Similarly to {@link #getPGNodeLabelIdConverter()}, this is used to get a Cypher node/relation property name
	 * from an RDF IRI (or even literal). 
	 * 
	 */
	public Function<String, String> getPGPropertyIdConverter ()
	{
		return pgPropertyIdConverter;
	}

	@Autowired ( required = false )	@Qualifier ( "pgPropertyIdConverter" )
	public void setPGPropertyIdConverter ( Function<String, String> propertyIdConverter )
	{
		this.pgPropertyIdConverter = propertyIdConverter;
	}

	/**
	 * No action for the case that sparqlSelect is null. 
	 * This is useful in rdf2pg, to ignore the conversion of certain types.
	 */
	@Override
	public long processSelect (
		String logPrefix, String sparqlSelect, Consumer<QuerySolution> action, QuerySolutionMap params 
	)
	{
		if ( sparqlSelect == null ) {
			log.debug ( "null SPARQL for {}, skipping", logPrefix );
			return 0;
		}
		return super.processSelect ( logPrefix, sparqlSelect, action, params );
	}
	
}
