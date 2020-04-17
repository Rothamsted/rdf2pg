package uk.ac.rothamsted.rdf.neo4j.load.support;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;

public class GraphMLUtils {
	
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
	
	
	/** 
	 * Modification of the the Value method in neo4j driver to map all the values to String
	 * 
	 * @param value
	 * @return
	 */
    public static String value( Object value )
    {
        if ( value == null ) { return "NULL"; }

        if ( value instanceof Boolean ) { return  ((Boolean) value ).toString(); }
        if ( value instanceof String ) { return (String) value; }
        if ( value instanceof Character ) { return ((Character) value ).toString(); }
        if ( value instanceof Long ) { return ((Long) value).toString(); }
        if ( value instanceof Short ) { return ((Short) value).toString(); }
        if ( value instanceof Byte ) { return ((Byte)value).toString(); }
        if ( value instanceof Integer ) { return ((Integer) value).toString(); }
        if ( value instanceof Double ) { return ((Double) value ).toString(); }
        if ( value instanceof Float ) { return ((Float) value).toString(); }
        if ( value instanceof LocalDate ) { return ((LocalDate) value ).toString(); }
        if ( value instanceof OffsetTime ) { return ((OffsetTime) value ).toString(); }
        if ( value instanceof LocalTime ) { return ((LocalTime) value).toString(); }
        if ( value instanceof LocalDateTime ) { return ((LocalDateTime) value).toString(); }
        if ( value instanceof OffsetDateTime ) { return ((OffsetDateTime) value).toString(); }
        if ( value instanceof ZonedDateTime ) { return ((ZonedDateTime) value).toString(); }
        if ( value instanceof Period ) { return ((Period) value).toString(); }
        if ( value instanceof Duration ) { return ((Duration) value).toString(); }
    
        if ( value instanceof byte[] ) { return ( (byte[]) value ).toString(); }
        if ( value instanceof boolean[] ) { return ( (boolean[]) value ).toString(); }
        if ( value instanceof String[] ) { return ( (String[]) value ).toString(); }
        if ( value instanceof long[] ) { return ( (long[]) value ).toString(); }
        if ( value instanceof int[] ) { return ( (int[]) value ).toString(); }
        if ( value instanceof double[] ) { return ( (double[]) value ).toString(); }
        if ( value instanceof float[] ) { return ( (float[]) value ).toString(); }
        if ( value instanceof Object[] ) { return ( Arrays.asList( (Object[]) value ) ).toString(); }

        return "Not Converted"; 
    }
    
    public static String dataValues (Map<String, Object> properties) {
    	StringBuilder strB = new StringBuilder(); 
    	for (String key: properties.keySet()) {
    		strB.append(GraphMLUtils.DATA_TAG_START); 
    		strB.append(GraphMLUtils.KEY_ATTR).append("=\"").append(key).append("\" >");
    		strB.append(value(properties.get(key)).replace("\"", "\\\""));
    		strB.append(GraphMLUtils.DATA_TAG_END); 
    	}
    	return strB.toString(); 
    }
    
}
