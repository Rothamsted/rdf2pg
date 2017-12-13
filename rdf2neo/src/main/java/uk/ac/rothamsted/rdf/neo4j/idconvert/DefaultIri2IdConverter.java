package uk.ac.rothamsted.rdf.neo4j.idconvert;

import java.util.function.Function;

import uk.ac.ebi.utils.ids.IdUtils;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>12 Dec 2017</dd></dl>
 *
 */
public class DefaultIri2IdConverter implements Function<String, String>
{
	@Override
	public String apply ( String iri )
	{
		return IdUtils.iri2id ( iri );
	}
}
