package uk.ac.rothamsted.rdf.neo4j.load.support;

public class GraphMLConfiguration {
	
	public static final String NODE_FILE_EXTENSION = "-Nodes-tmp.graphml"; 
	public static final String EDGE_FILE_EXTENSION = "-Edges-tmp.graphml"; 
	
	/** The actual configuration **/ 
	
	public static String outputFile = null; 
	public static String defaultNodeLabel = "graphMLNode";
	
	public static String getOutputFile() {
		return outputFile;
	}
	public static void setOutputFile(String outputFile) {
		GraphMLConfiguration.outputFile = outputFile;
	}
	public static String getNodeDefaultLabel() {
		return defaultNodeLabel;
	}
	public static void setNodeDefaultLabel(String defaultNodeLabel) {
		GraphMLConfiguration.defaultNodeLabel = defaultNodeLabel;
	} 
}
