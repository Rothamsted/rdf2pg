package uk.ac.rothamsted.kg.rdf2pg.pgmaker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * A default implementation for {@link NamedPGMakeComponent}. You can either extend
 * your specific component class, or use an instance of this as delegate.
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>1 Jun 2023</dd></dl>
 *
 */
public class DefaultNamedPGMakeComp implements NamedPGMakeComponent
{
	private String componentName;

	/**
	 * Represents the nodes/relations kind that are made by this maker. This is prefixed to logging messages
	 * and is primarily useful when the simple maker is used by {@link MultiConfigPGMaker}. 
	 */
	public String getComponentName ()
	{
		return componentName;
	}

	@Autowired ( required = false ) @Qualifier ( "defaultMakerName" )
	public void setComponentName ( String name )
	{
		this.componentName = name;
	}

}
