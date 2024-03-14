package uk.ac.rothamsted.neo4j.utils;

import org.neo4j.driver.exceptions.Neo4jException;

/**
 * This is useful when you want to report a generic Neo4j error.
 * 
 * @deprecated this class was created because @Neo4jException used to be abstract, 
 * now you can use that. 
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>3 Jul 2019</dd></dl>
 *
 */
@Deprecated
public class GenericNeo4jException extends Neo4jException
{

	private static final long serialVersionUID = -5529823747303867500L;

	public GenericNeo4jException ( String message )
	{
		super ( message );
	}

	public GenericNeo4jException ( String message, Throwable cause )
	{
		super ( message, cause );
	}

	public GenericNeo4jException ( String code, String message )
	{
		super ( code, message );
	}

	public GenericNeo4jException ( String code, String message, Throwable cause )
	{
		super ( code, message, cause );
	}
}
