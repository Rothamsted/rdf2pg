package uk.ac.rothamsted.kg.rdf2pg.pgmaker;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Simple interface for those components that need to expose a name in log messages or alike.
 * 
 * The name is typically associated to a {@link ConfigItem configuration} and set in configuration
 * files.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Jun 2023</dd></dl>
 *
 */
public interface NamedPGMakeComponent
{
	/**
	 * Represents the nodes/relations kind that are made by this maker. This is prefixed to logging messages
	 * and is primarily useful when the simple maker is used by {@link MultiConfigPGMaker}. 
	 */
	String getComponentName ();

	@Autowired ( required = false ) @Qualifier ( "defaultMakerName" )
	void setComponentName ( String name );
	
	/**
	 * It's {@link #getComponentName()}, possibly (if not empty/null) in a form like "[ name ] ", 
	 * which is used internally for logging and alike.
	 */
	default String getCompNamePrefix ()
	{
		String result = StringUtils.trimToEmpty ( this.getComponentName () );
		return result.isEmpty () ? "" : "[" + result + "] ";
	}
}
