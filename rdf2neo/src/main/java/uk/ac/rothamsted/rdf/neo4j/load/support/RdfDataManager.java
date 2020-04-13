package uk.ac.rothamsted.rdf.neo4j.load.support;

import static info.marcobrandizi.rdfutils.jena.JenaGraphUtils.JENAUTILS;

import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.QuerySolutionMap;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import info.marcobrandizi.rdfutils.jena.SparqlUtils;
import info.marcobrandizi.rdfutils.jena.TDBEndPointHelper;
import uk.ac.rothamsted.rdf.neo4j.idconvert.DefaultIri2IdConverter;

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
 */
@Component
public class RdfDataManager extends TDBEndPointHelper
{
	private Function<String, String> cyNodeLabelIdConverter = new DefaultIri2IdConverter ();
	private Function<String, String> propertyIdConverter = new DefaultIri2IdConverter (); 
	private Function<String, String> cyRelationIdConverter = new DefaultIri2IdConverter ();
	
	public RdfDataManager () {
	}

	public RdfDataManager ( String tdbPath ) {
		super ( tdbPath );
	}

	/**
	 * Uses the underlining TDB and mapping queries to create a new {@link CyNode} instance.
	 * 
	 * @param nodeRes the RDF/Jena resource correspnding to the Cypher node. This provides the ?iri paramter in the queries below.
	 * @param labelsSparql the node labels query, which is usually taken from {@link CyNodeLoadingHandler#getLabelsSparql()}.
	 * @param propsSparql the node properties query, which is usually taken from {@link CyNodeLoadingHandler#getNodePropsSparql()}.
	 */
	public CyNode getCyNode ( Resource nodeRes, String labelsSparql, String propsSparql )
	{
		ensureOpen ();
		
		QuerySolutionMap params = new QuerySolutionMap ();
		params.add ( "iri", nodeRes );

		CyNode cyNode = new CyNode ( nodeRes.getURI () );
		
		// The node's labels
		if ( labelsSparql != null )
		{
			// If it's omitted, it will get the default label.
			Query qry = SparqlUtils.getCachedQuery ( labelsSparql );
			Function<String, String> labelIdConverter = this.getCyNodeLabelIdConverter ();
			
			boolean wasInTnx = dataSet.isInTransaction ();
			if ( !wasInTnx ) dataSet.begin ( ReadWrite.READ );
			try {
				QueryExecution qx = QueryExecutionFactory.create ( qry, this.getDataSet(), params );
				qx.execSelect ().forEachRemaining ( row ->
					cyNode.addLabel ( this.getCypherId ( row.get ( "label" ), labelIdConverter ) )
				);
			}
			finally {
				if ( !wasInTnx && dataSet.isInTransaction () ) dataSet.end ();
			}
		}
		
		// and the properties
		this.addCypherProps ( cyNode, propsSparql );
		
		return cyNode;
	}

	/**
	 * Just a variant of {@link #getCyNode(Resource, String, String)}.
	 */
	public CyNode getCyNode ( String nodeIri, String labelsSparql, String propsSparql )
	{
		ensureOpen ();
		Resource nodeRes = this.getDataSet().getUnionModel().getResource ( nodeIri );
		return getCyNode ( nodeRes, labelsSparql, propsSparql ); 
	}

	/**
	 * Gets a Cypher ID by applying an {@link DefaultIri2IdConverter ID conversion function} to an IRI taken from a 
	 * {@link Resource} RDF/Jena node, or to a lexical value taken from a {@link Literal} RDF/Jena node. 
	 * 
	 * Helper method used in other methods in this class.
	 */
	protected String getCypherId ( RDFNode node, Function<String, String> idConverter )
	{
		if ( node == null ) return null;
		
		String id = node.canAs ( Resource.class )
			? node.as ( Resource.class ).getURI ()
			: node.asLiteral ().getLexicalForm ();
					
		if ( idConverter != null ) id = idConverter.apply ( id );
		
		return id;
	}
	
	/**
	 * Take an existing {@link CypherEntity} and adds the properties that can be mapped from the underlining TDB by means 
	 * of a property query, like {@link CyNodeLoadingHandler#getNodePropsSparql()}, or 
	 * {@link CyRelationLoadingHandler#getRelationPropsSparql()}.
	 * 
	 * It doesn't do anything if the query is null.
	 * 
	 */
	protected void addCypherProps ( CypherEntity cyEnt, String propsSparql )
	{
		ensureOpen ();		
		Dataset dataSet = this.getDataSet ();
		
		QuerySolutionMap params = new QuerySolutionMap ();
		params.add ( "iri", dataSet.getUnionModel().getResource ( cyEnt.getIri () ) );

		// It may be omitted, if you don't have any property except the IRI.
		if ( propsSparql == null ) return;
		
		Query qry = SparqlUtils.getCachedQuery ( propsSparql );
		Function<String, String> propIdConverter = this.getCyPropertyIdConverter ();
		
		boolean wasInTnx = dataSet.isInTransaction ();
		if ( !wasInTnx ) dataSet.begin ( ReadWrite.READ );
		try
		{
			QueryExecution qx = QueryExecutionFactory.create ( qry, dataSet, params );
			qx.execSelect ().forEachRemaining ( row ->
			{
				String propName = this.getCypherId ( row.get ( "name" ), propIdConverter );
				if ( propName == null ) throw new IllegalArgumentException ( 
					"Null property name for " + cyEnt.getIri () 
				);
				
				String propValue = JENAUTILS.literal2Value ( row.getLiteral ( "value" ) ).get ();
				cyEnt.addPropValue ( propName, propValue );
			});
		}
		finally {
			if ( !wasInTnx && dataSet.isInTransaction () ) dataSet.end ();
		}
	}
	
	/**
	 * Does something with the results coming from {@link CyNodeLoadingHandler node IRI query}. This method is an helper 
	 * that contains common operations like transaction markers, logging etc.
	 * 
	 */
	public long processNodeIris ( String nodeIrisSparql, Consumer<Resource> action )
	{
		return this.processSelect ( "processNodeIris()", nodeIrisSparql, row ->
			action.accept ( row.getResource ( "iri" ) )
		);
	}
	
	
	/**
	 * Similarly to {@link #getCyNode(Resource, String, String)}, uses a binding (i.e., row) from a 
	 * {@link CyRelationLoadingHandler#getRelationTypesSparql() relation type query} and creates a new {@link CyRelation}
	 * with the RDF mapped data.
	 */
	public CyRelation getCyRelation ( QuerySolution relRow )
	{		
		Resource relRes = relRow.get ( "iri" ).asResource ();
		CyRelation cyRelation = new CyRelation ( relRes.getURI () );
		
		cyRelation.setType ( this.getCypherId ( relRow.get ( "type" ), this.getCyRelationTypeIdConverter () ) );

		cyRelation.setFromIri ( relRow.get ( "fromIri" ).asResource ().getURI () );
		cyRelation.setToIri ( relRow.get ( "toIri" ).asResource ().getURI () );
				
		return cyRelation;
	}
	
	/**
	 * Similarly to {@link #addCypherProps(CypherEntity, String)}, takes a {@link CyRelation} and adds the properties that
	 * can be mapped via {@link CyRelationLoadingHandler#getRelationPropsSparql() relation property query}.
	 * 
	 */
	public void setCyRelationProps ( CyRelation cyRelation, String propsSparql )
	{
		this.addCypherProps ( cyRelation, propsSparql );
	}
	
	/**
	 * Similarly to {@link #processNodeIris(String, Consumer)}, does something with the results from a 
	 * {@link CyRelationLoadingHandler#getRelationTypesSparql() relation types query}.
	 * 
	 */
	public long processRelationIris ( String relationIrisSparql, Consumer<QuerySolution> action ) {
		return processSelect ( "processRelationIris()", relationIrisSparql, action );
	}
	
	/** 
	 * Methods like {@link #getCyNode(Resource, String, String)} use this {@link DefaultIri2IdConverter ID} converter to 
	 * get IDs for Cypher node labels from RDF IRIs (or even literal).
	 * 
	 */
	public Function<String, String> getCyNodeLabelIdConverter ()
	{
		return cyNodeLabelIdConverter;
	}

	@Autowired ( required = false ) @Qualifier ( "nodeLabelIdConverter" )
	public void setCyNodeLabelIdConverter ( Function<String, String> labelIdConverter )
	{
		this.cyNodeLabelIdConverter = labelIdConverter;
	}
	
	/**
	 * Similarly to {@link #getCyNodeLabelIdConverter()}, this is used to get a relation type string from 
	 * a relation type IRI (or even literal). 
	 * 
	 */
	public Function<String, String> getCyRelationTypeIdConverter ()
	{
		return cyRelationIdConverter;
	}

	@Autowired ( required = false )	@Qualifier ( "relationIdConverter" )
	public void setCyRelationTypeIdConverter ( Function<String, String> relationIdConverter )
	{
		this.cyRelationIdConverter = relationIdConverter;
	}



	/**
	 * Similarly to {@link #getCyNodeLabelIdConverter()}, this is used to get a Cypher node/relation property name
	 * from an RDF IRI (or even literal). 
	 * 
	 */
	public Function<String, String> getCyPropertyIdConverter ()
	{
		return propertyIdConverter;
	}

	@Autowired ( required = false )	@Qualifier ( "propertyIdConverter" )
	public void setCyPropertyIdConverter ( Function<String, String> propertyIdConverter )
	{
		this.propertyIdConverter = propertyIdConverter;
	}

	/**
	 * If sparql is null, just doesn't do anything. This is useful in the rdf2neo context, since there are configured
	 * queries that are optional. 
	 */
	@Override
	public long processSelect ( String logPrefix, String sparql, Consumer<QuerySolution> action )
	{
		if ( sparql == null ) {
			log.debug ( "null SPARQL for {}, skipping", logPrefix );
			return 0;
		}
		
		return super.processSelect ( logPrefix, sparql, action );
	}
}
