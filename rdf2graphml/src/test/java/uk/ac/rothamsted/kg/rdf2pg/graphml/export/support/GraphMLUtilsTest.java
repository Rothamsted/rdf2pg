package uk.ac.rothamsted.kg.rdf2pg.graphml.export.support;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.function.Function;

import org.junit.Test;


/**
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>7 Nov 2020</dd></dl>
 *
 */
public class GraphMLUtilsTest
{
	@Test
	public void testGraphMLValueNumber ()
	{
		doGraphMlValueTest ( 2.5d, x -> Double.toString ( x ), "number" );
	}

	@Test
	public void testGraphMLValueString ()
	{
		doGraphMlValueTest ( "Hello, World!", Function.identity (), "string" );
	}

	@Test
	public void testGraphMLValueNull ()
	{
		doGraphMlValueTest ( null, x -> "NULL", "<null>" );
	}

	@Test
	public void testGraphMLValueList ()
	{
		doGraphMlValueTest ( new Integer[] { 1, 2, 3, 4, 5, 6 }, x -> Arrays.asList ( x ).toString (), "array" );
	}
	
	
	private <T> void doGraphMlValueTest ( T value, Function<T, String> expectedConversion, String type )
	{
		assertEquals (
			"Bad ML value for the type " + type + "!", 
			expectedConversion.apply ( value ), GraphMLUtils.graphMLValue ( value )
		); 
	}

}
