package uk.ac.rothamsted.rdf.neo4j.load.load.support;

import java.util.function.Consumer;

import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.marcobrandizi.rdfutils.jena.SparqlUtils;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>4 Dec 2017</dd></dl>
 *
 */
public class NodeLuceneHandler implements Consumer<Model>
{
	private NeoDataManager dataMgr; 
	private String nodeLabelsSparql, nodePropsSparql;

	private Logger log = LoggerFactory.getLogger ( this.getClass () );

	
	@Override
	public void accept ( Model model )
	{
		log.debug ( "Indexing nodes" );
		ResultSet rs = SparqlUtils.select ( nodeLabelsSparql, model );
		dataMgr.indexNodeLabelRows ( rs );

		rs = SparqlUtils.select ( nodePropsSparql, model );
		dataMgr.indexNodePropertyRows ( rs );
	}
}
