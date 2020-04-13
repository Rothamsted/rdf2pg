package uk.ac.rothamsted.neo4j.utils.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.neo4j.driver.internal.value.BooleanValue;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Value;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.types.TypeSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.utils.exceptions.UncheckedFileNotFoundException;
import uk.ac.rothamsted.neo4j.utils.Neo4jDataManager;

/**
 * A utility for testing Neo4j/Cypher data, probing them with Cypher queries. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>10 Apr 2018</dd></dl>
 *
 */
public class CypherTester 
{
	private Neo4jDataManager neo4jDataManager;
	private String cypherHeader = "";
	
	private Logger log = LoggerFactory.getLogger ( this.getClass() );

	/**
	 * We don't care about closing the driver, this is up to the caller.
	 */
	public CypherTester ( Driver neo4jDriver )
	{
		this ( new Neo4jDataManager ( neo4jDriver ) );
	}

	public CypherTester ( Neo4jDataManager neo4jDataManager )
	{
		super ();
		this.neo4jDataManager = neo4jDataManager;
	}

	/**
	 * Runs a query that is supposed to return a boolean, return the result of that.
	 * 
	 * <p>The keyVals parameter is passed to {@link Values#parameters(Object...)}.</p>
   *
	 */
	public boolean ask ( String cypher, Object... keyVals )
	{
		final String hcypher = this.getCypherHeader () + cypher;
		final Boolean result[] = new Boolean [] { null };
		neo4jDataManager.processCypherMatches ( 
			bool -> 
			{ 
				Value v = bool.size () > 0 ? bool.get ( 0 ) : null;
				if ( v == null || v.isNull () || !v.hasType ( BooleanValue.TRUE.type () )) 
				{
					String emsg = "Test query for CypherTester.ask() must return a boolean";
					log.error ( "{}. Query is:\n{}", emsg, hcypher );
					throw new IllegalArgumentException ( emsg );
				}
				result [ 0 ] = v.asBoolean ();
			},
			hcypher, 
			keyVals
		);
		
		if ( result [ 0 ] == null )
		{
			String emsg = "Test query for CypherTester.ask() didn't return any entry";
			log.error ( "{}. Query is:\n{}", emsg, cypher );
			throw new IllegalArgumentException ( emsg );
		}
		return result [ 0 ];
	}
	
	/**
	 * Runs a Cypher query that is supposed to return a single tuple having at least a single bound variable.
	 * Passes it to the test parameter (using a {@link Value}), this should return a boolean, which is also the
	 * return value for this method.
	 * 
	 * For instance, in the test function you might want to compare the Cypher returned value to a reference 
	 * value of yours.
	 */
	public boolean compare ( Function<Value, Boolean> test, String cypher, Object... keyVals )
	{
		final String hcypher = this.getCypherHeader () + cypher;		
		final Value result[] = new Value [] { null };
		neo4jDataManager.processCypherMatches ( 
			rec -> 
			{ 
				if ( rec.size () == 0 )
				{
					String emsg = "Test query for CypherTester.compare() must project some field";
					log.error ( "{}. Query is:\n{}", emsg, hcypher );
					throw new IllegalArgumentException ( emsg );
				}
				result [ 0 ] = rec.get ( 0 );
			},
			hcypher, 
			keyVals
		);
		
		return test.apply ( result [ 0 ] );
	}

	/**
	 * This invokes {@link #compare(Function, String, Object...)} passing a function that compares
	 * the reference parameter to the Cypher-received value. The value that is compared to the reference
	 * is obtained by {@link Value#asObject()}, so the mappings from Cypher types to Java types is based 
	 * on {@link TypeSystem}. 
	 * @see <a href = "https://neo4j.com/docs/developer-manual/current/drivers/cypher-values/">Neo4j documentation</a>
	 * for details.
	 * 
	 */
	public boolean compare ( Object reference, String cypher, Object... keyVals )
	{
		return compare ( 
			v -> {
				if ( v == null || v.isNull () ) return reference == null;
				return v.asObject ().equals ( reference );
			},
			cypher,
			keyVals
		);
	}
	
	/**
	 * Invokes {@link #ask(String, Object...)} taking the Cypher query from the file in the parameter.
	 */
	public boolean askFromFile ( String cypherPath, Object... keyVals )
	{
		try {
			return ask ( IOUtils.toString ( new FileReader ( cypherPath ) ), keyVals );
		}
		catch ( FileNotFoundException ex )
		{
			throw new UncheckedFileNotFoundException ( 
				"File '" + cypherPath + "' not found while running CypherTester: " + ex.getMessage (), 
				ex 
			);
		}
		catch ( IOException ex ) {
			throw new UncheckedIOException ( "I/O error while running CypherTester: " + ex.getMessage (), ex );
		}		
	}

	/**
	 * Invokes {@link #askFromFile(String, Object...)} for every file having {@code .cypher} extension in the specified 
	 * directory. {@code failAction} is invoked if a file causes a result of false to be returned. The file at issue is passed 
	 * to the action for convenience (e.g., to report it in a message).  
	 * 
	 */
	public long askFromDirectory ( 
		Consumer<File> failAction, String dirPath, boolean isRecursive, Object... keyVals 
	)
	{
		long testsCount = FileUtils.listFiles ( 
			new File ( dirPath ), 
			new String[] { "cypher" },
			isRecursive
		)
		.stream ()
		.sorted ()
		.filter ( f -> f != null ) // forces peek()
		.peek ( f -> {
			log.info ( "Running '{}'", f.getName () );
			if ( !this.askFromFile ( f.toString (), keyVals ) ) failAction.accept ( f );
		})
		.count ();
		
		return testsCount;
	}
	
	/**
	 * Wrapper with isRecursive = true.
	 */
	public long askFromDirectory ( Consumer<File> failAction, String dirPath, Object... keyVals ) {
		return askFromDirectory ( failAction, dirPath, true, keyVals );
	}	

	/** 
	 * We work with a {@link Neo4jDataManager} to leverage its utility function. You can setup 
	 * a tester with either this or a {@link Driver Neo4j driver}.
	 * 
	 */
	public Neo4jDataManager getNeo4jDataManager ()
	{
		return neo4jDataManager;
	}

	public void setNeo4jDataManager ( Neo4jDataManager neo4jDataManager )
	{
		this.neo4jDataManager = neo4jDataManager;
	}

	
	/**
	 * This is a prefix used with all the cypher queries mentioned in the above methods.
	 * Typically, it is a set of WITH/UNWIND clauses defining constants.
	 * 
	 */
	public String getCypherHeader ()
	{
		return cypherHeader;
	}

	public void setCypherHeader ( String cypherHeader )
	{
		this.cypherHeader = cypherHeader;
	}

	/**
	 * Defines a {@link #getCypherHeader() Cypher header} as string contant definitions, by means of
	 * {@code WITH 'value' AS key }
	 * 
	 */
	public void setCypherHeader ( Map<String, String> constants )
	{
		String cypher = constants
		.entrySet ()
		.stream ()
		.map ( e -> "  \"" + e.getValue () + "\" AS " + e.getKey () )
		.collect ( Collectors.joining ( ",\n" ) );

		cypher = StringUtils.trimToEmpty ( cypher );
		
		if ( !cypher.isEmpty () )
			cypher = "WITH\n" + cypher + "\n";

		setCypherHeader ( cypher );
	}
}
