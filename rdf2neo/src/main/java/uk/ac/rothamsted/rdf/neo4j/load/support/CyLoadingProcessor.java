package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import uk.ac.ebi.utils.threading.SizeBasedBatchProcessor;
import uk.ac.rothamsted.rdf.neo4j.load.SimpleCyLoader;

/**
 * <H1>The base for a Cypher/Neo4j loading processor.</H1>
 * 
 * <p>We use this just to factorise a few common parameters and methods.</p>
 *
 * <p>@see CyNodeLoadingProcessor and {@link CyRelationLoadingProcessor}.</p>
 * 
 * <p>This class extends {@link SizeBasedBatchProcessor} with the generic type {@code Set<T>}, since each loader
 * processes a collection of entity pointers (node IRIs/relation base structures), which are obtained from RDF, 
 * via SPARQL mappings (see {@link SimpleCyLoader}).</p>
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>13 Jan 2018</dd></dl>
 *
 */
public abstract class CyLoadingProcessor<T> extends SizeBasedBatchProcessor<RdfDataManager, Set<T>>
	implements AutoCloseable
{
	public CyLoadingProcessor ()
	{
		super ();
		this.setDestinationMaxSize ( 25000 );
		this.setDestinationSupplier ( () -> new HashSet<> () );
	}

	@Override
	protected long getDestinationSize ( Set<T> dest )
	{
		return dest.size ();
	}

	@Autowired ( required = false ) @Qualifier ( "destinationMaxSize" )
	@Override // Just to use annotations
	public CyLoadingProcessor<T> setDestinationMaxSize ( long destinationMaxSize )
	{
		super.setDestinationMaxSize ( destinationMaxSize );
		return this;
	}
	
	/**
	* If the {@link #getConsumer() consumer} is {@link AutoCloseable}, invokes its {@link AutoCloseable#close()}
	* method.
	* 
	*/
	@Override
	public void close () throws Exception
	{
		Consumer<?> consumer = this.getConsumer ();
		if ( consumer != null && consumer instanceof AutoCloseable ) ((AutoCloseable) consumer).close ();
	}	
}