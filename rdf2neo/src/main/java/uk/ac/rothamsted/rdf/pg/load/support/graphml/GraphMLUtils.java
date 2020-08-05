package uk.ac.rothamsted.rdf.pg.load.support.graphml;

public class GraphMLUtils 
{	
	/** Some constants related to GraphML **/ 
	
	public static final String GRAPHML_TAG_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n" + 
			"<graphml xmlns=\"http://graphml.graphdrawing.org/xmlns\" \n" + 
			"    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n" + 
			"    xsi:schemaLocation=\"http://graphml.graphdrawing.org/xmlns http://graphml.graphdrawing.org/xmlns/1.0/graphml.xsd\">"; 
	public static final String GRAPHML_TAG_END = "</graphml>"; 
	public static final String GRAPH_TAG_START = "<graph "; 
	public static final String GRAPH_TAG_END = "</graph>"; 
	public static final String NODE_TAG_START = "<node ";
	public static final String NODE_TAG_END = "</node>"; 
	public static final String EDGE_TAG_START = "<edge ";
	public static final String EDGE_TAG_END = "</edge>";
	public static final String DATA_TAG_START = "<data "; 
	public static final String DATA_TAG_END = "</data>";
	public static final String KEY_TAG_START = "<key ";
	public static final String KEY_TAG_END = "</key>"; 
	public static final String ID_ATTR = "id";
	// these two attributes are the default for gremlin graphML 
	public static final String LABEL_VERTEX_ATTR = "labelV";
	public static final String LABEL_EDGE_ATTR = "labelE"; 
	public static final String KEY_ATTR = "key";
	public static final String LABELS_ATTR = "labels"; 
	public static final String SOURCE_ATTR = "source"; 
	public static final String TARGET_ATTR = "target"; 
	public static final String DIRECTED_ATTR = "directed"; 
	public static final String DEFAULT_DIRECTED_ATTR = "edgedefault";
	public static final String FOR_ATTR = "for"; 
	public static final String ATTR_NAME_ATTR = "attr.name"; 
	public static final String EDGE_FOR_VALUE = "edge"; 
	public static final String NODE_FOR_VALUE = "node"; 
	public static final String DIRECTED_DEFAULT_DIRECTED_VALUE="directed"; 
	
	public static final String EDGE_TYPE_TAG = "type"; 
	
	private static final Map<Class<Object>, Function<Object, String>> ATTR_VALUE_CONVERTERS = new HashMap<> (); 

	
	static 
	{
		init ();
	}
	
	/**
	 * This is the code in the class initialiser. It's here just because I don't want to
	 * use {@code @SuppressWarnings} at the class level.
	 */
	@SuppressWarnings ( { "rawtypes", "unchecked" } )
	private static void init()
	{
		Stream.of ( 
			Boolean.class, String.class, Character.class, Number.class, 
			Temporal.class, ChronoPeriod.class, TemporalAmount.class 
		)
		.forEach ( cls -> ATTR_VALUE_CONVERTERS.put ( (Class<Object> )cls, Object::toString ) );
		
		ATTR_VALUE_CONVERTERS.put ( (Class) Array.class, v -> Arrays.asList ( (Object[]) v ).toString () );	
	}
	
	
	/**
	 * Support method, which goes through all the parameter parents recursively, if the type isn't 
	 * immediately available in {@link #ATTR_VALUE_CONVERTERS}
	 */
	private static String graphMLValue ( Object value, Class<?> type )
	{
		String result = Optional.ofNullable ( ATTR_VALUE_CONVERTERS.get ( type ) )
			.map ( cvt -> cvt.apply ( value ) )
			.orElse ( null );
		if ( result != null ) return result;
		
		Class<?> parentClass = value.getClass ().getSuperclass ();
		if ( parentClass != null )
		{
			result = graphMLValue ( value, parentClass );
			if ( result != null ) return result;
		}
		
		for ( var parentType: value.getClass ().getInterfaces () )
		{
			result = graphMLValue ( value, parentType );
			if ( result != null ) return result;
		}
		
		return null;
	}
	
	/** 
	 * Modification of the the Value method in neo4j driver to map all the values to String
	 */
  public static String graphMLValue( Object value )
  {
    if ( value == null ) return "NULL";
    String result = graphMLValue ( value, value.getClass () );

    if ( result == null ) throw new UnsupportedOperationException ( String.format (
    	"The value '%s' cannot be converted into a GraphML value, type %s is not supported",
    	StringUtils.abbreviate ( value.toString (), 30 ),
    	value.getClass ()
    ));
    
    return result;
  }
    
  public static void writeXMLAttribs ( Map<String, Object> properties, PrintStream out )
  {
  	for ( String key: properties.keySet() ) 
  	{
  		out.append ( GraphMLUtils.DATA_TAG_START );
  		writeXMLAttrib ( KEY_ATTR, key, out );
  		out.append ( " >" );
  		
  		// TODO: probably you actually wand a CDATA block without unreliable escaping
  		out.append ( StringEscapeUtils.escapeXml11 ( graphMLValue ( properties.get ( key ) )).replace ( "\"", "\\\"" ) );
  		out.append ( GraphMLUtils.DATA_TAG_END ); 
  	}
  }
 
  public static void writeXMLAttrib ( String attrName, String attrValue, PrintStream out )
  {
  	out.append ( attrName ).append ( "=\"" ).append ( attrValue ).append ( "\"" );
  }

  public static void writeNodeAttribHeaders ( Set<String> attribIDs, PrintStream out ) {
  	writeAttribHeaders ( attribIDs, NODE_FOR_VALUE, out );
  }
  
  public static void writeEdgeAttribHeaders ( Set<String> attribIDs, PrintStream out ) {
  	writeAttribHeaders ( attribIDs, EDGE_FOR_VALUE, out );
  }
  
  private static void writeAttribHeaders ( Set<String> attribIDs, String forAttrib, PrintStream out )
  {
		for (String attribID: GraphMLNodeExportHandler.getGatheredNodeProperties() ) 
		{
			out.append ( GraphMLUtils.KEY_TAG_START );
			
			writeXMLAttrib ( GraphMLUtils.ID_ATTR, attribID, out ); out.print ( ' ' );
			writeXMLAttrib ( GraphMLUtils.FOR_ATTR, forAttrib , out ); out.print ( ' ' );
			
			// TODO: for the time being, we don't support typing for the key / data 
			// maybe we could add it via
			writeXMLAttrib ( GraphMLUtils.ATTR_NAME_ATTR, attribID , out ); 
			
			out.println ( " />" );
		}
  }
}
