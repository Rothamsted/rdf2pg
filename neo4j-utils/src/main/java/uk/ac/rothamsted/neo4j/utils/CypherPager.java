package uk.ac.rothamsted.neo4j.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.TransactionContext;

/**
 * An helper to deal with the pagination of read-only queries.
 * 
 * This is an {@link Iterator} based on a Cypher query that has OFFSET/LIMIT
 * clauses. See {@link #hasNext()} for details.
 * 
 *
 * @author Marco Brandizi
 * <dl><dt>Date:</dt><dd>7 Oct 2024</dd></dl>
 *
 */
public class CypherPager implements Iterator<Record>
{
	private BiFunction<TransactionContext, Long, Result> callBack;		
	private Driver neoDriver;		
	
	private long pageSize = 2500; // From previous experience
	private long offset = 0;
	
	private Iterator<Record> currentCursor;
	private boolean isFinished = false;
	
	/**
	 * @param callBack The Cypher query from which to get a page result. TODO: more
	 * @param neoDriver
	 * @param pageSize The iterator buffers results in memory, with a buffer having the same size as pageSize, 
	 * so take this into account.
	 */
	public CypherPager ( BiFunction<TransactionContext, Long, Result> callBack, Driver neoDriver, Long pageSize )
	{
		this.callBack = callBack;
		this.neoDriver = neoDriver;
		if ( pageSize != null ) this.pageSize = pageSize;
	}

	public CypherPager ( BiFunction<TransactionContext, Long, Result> callBack, Driver neoDriver )
	{
		this ( callBack, neoDriver, null );
	}
	
	
	@Override
	public boolean hasNext ()
	{
		// you're calling me after both hasNext() and the last page said we're over.
		if ( this.isFinished ) return false; 
		
		if ( currentCursor != null && currentCursor.hasNext () )
			// We're still in a page iteration
			return true;
		
		// If we have never fetched a page or we are at the end of the current one,
		// fetch and start a new page
		//
		try ( var session = neoDriver.session () )
		{
			currentCursor = session.executeRead ( 
				tx -> callBack.apply ( tx, offset )
					.list ()
					.iterator ()
			);
		}
		
		if ( !currentCursor.hasNext () ) 
		{
			// It's over, mark it as such
			currentCursor = null; // Just to free some memory
			this.isFinished = true;
			return false;
		}
	
		// Prepare the next page
		this.offset += pageSize;
		
		// the just-retrieved cursor has items
		return true;
	
	} // hasNext ()

	@Override
	public Record next ()
	{
		if ( !hasNext () ) throw new NoSuchElementException (
			"Cypher pager called after exhaustion" 
		);
		
		return currentCursor.next ();
	}
	
} // CypherPager