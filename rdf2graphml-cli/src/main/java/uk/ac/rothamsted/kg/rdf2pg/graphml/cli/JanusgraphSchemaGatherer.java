package uk.ac.rothamsted.kg.rdf2pg.graphml.cli;

import java.io.BufferedOutputStream;

/**
 * TODO: comment me!
 *
 * @author cbobed
 * <dl><dt>Date:</dt><dd>4 Apr 2021</dd></dl>
 *
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;

import javax.xml.parsers.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import uk.ac.rothamsted.kg.rdf2pg.cli.CliCommand;
import uk.ac.rothamsted.kg.rdf2pg.cli.Rdf2PGCli;

public class JanusgraphSchemaGatherer extends CliCommand {

	@Option ( 
		names = { "-i", "--input" }, 
		description = "GraphML file to extract the schema information from",
		required = true
	)
	protected String graphMLFilename = "";
	
	@Option (
		names = { "-o", "--output"}, 
		description = "Output file to write the groovy scrip to", 
		required = true
		)
	protected String groovyFilename = ""; 
	
	@Option (
		names = { "-k", "--key"}, 
		description = "Name of the key property serving as ID to create an index on it if required", 
		required = false
		)
	protected String key = ""; 
	
	@Override
	public final Integer call () throws Exception
	{
		
		long start = System.currentTimeMillis();
		
		HashSet<String> vertexLabels = new HashSet<>(); 
		HashSet<String> vertexProperties = new HashSet<>();
		HashSet<String> edgeLabels = new HashSet<>();
		HashSet<String> edgeProperties = new HashSet<>();
		boolean sanityCheck = true; 
			
		gatherAllInformation(graphMLFilename, vertexLabels, vertexProperties, edgeLabels, edgeProperties); 
		sanityCheck = checkSanity(vertexLabels, vertexProperties, edgeLabels, edgeProperties); 
		if (sanityCheck) {
			writeGroovyScript(groovyFilename, 
									vertexLabels, 
									vertexProperties, 
									edgeLabels, 
									edgeProperties,
									!"".equalsIgnoreCase(key), 
									key);
		}
		// printSchemaInformation(vertexLabels, vertexProperties, edgeLabels, edgeProperties);  
		long end = System.currentTimeMillis();
		System.out.println("took aprox: "+((end-start)/1000)+" s. "); 
		
		return sanityCheck?0:-1; 
	}
		
	public final  Hashtable<String, String> getAttributesAsHashtable (XMLStreamReader xmlr) {
		Hashtable<String, String> values = new Hashtable<String, String>(); 
		for (int i=0; i<xmlr.getAttributeCount(); i++) {
			values.put(xmlr.getAttributeLocalName(i), xmlr.getAttributeValue(i)); 
		}
		return values; 		
	}
	
	public final void gatherAllInformation (String filename, 
												HashSet<String> vertexLabels,
												HashSet<String> vertexProperties, 
												HashSet<String> edgeLabels,
												HashSet<String> edgeProperties ) throws XMLStreamException, FileNotFoundException 
	{
		XMLInputFactory xmlif = XMLInputFactory.newInstance(); 
		XMLStreamReader xmlr = xmlif.createXMLStreamReader(filename,
                new FileInputStream(filename));
		Hashtable<String,String> attribs; 

		// when XMLStreamReader is created, 
			// it is positioned at START_DOCUMENT event.
			int eventType = xmlr.getEventType();
			
			boolean insideNode = false; 
			boolean insideEdge = false;
			// check if there are more events 
			// in the input stream
			while(xmlr.hasNext()) {
				xmlr.next(); 
				eventType = xmlr.getEventType(); 
				 switch (eventType){
		            case XMLEvent.START_ELEMENT:
		            	if ("node".equalsIgnoreCase(xmlr.getLocalName()) ) {
		            		insideNode = true;	
		            		attribs = getAttributesAsHashtable(xmlr);
		            		if (attribs.containsKey("labelV")) {
		            			vertexLabels.add(attribs.get("labelV"));
		            		}
		            		
		            	}
		            	else if ("edge".equalsIgnoreCase(xmlr.getLocalName())) {
		            		insideEdge = true;
		            		attribs = getAttributesAsHashtable(xmlr);
		            		if (attribs.containsKey("labelE")) {
		            			edgeLabels.add(attribs.get("labelE"));
		            		}
		            	}
		            	else if ("key".equalsIgnoreCase(xmlr.getLocalName())) {
	            			attribs = getAttributesAsHashtable(xmlr);
		            		if (attribs.containsKey("for")) {
		            			if ("node".equalsIgnoreCase(attribs.get("for"))) {
		            				// it should be the same as id
		            				vertexProperties.add(attribs.get("attr.name"));
		            				vertexProperties.add(attribs.get("id")); 
		            			}
		            			else if ("edge".equalsIgnoreCase(attribs.get("for"))) {
		            				edgeProperties.add(attribs.get("attr.name")); 
		            				edgeProperties.add(attribs.get("id")); 
		            			}	            			
		            		}
		            	}
		            	else if ("data".equalsIgnoreCase(xmlr.getLocalName())) {
		            		attribs = getAttributesAsHashtable(xmlr); 
		            		if (attribs.containsKey("key")) {
		            			if (insideNode && "labelV".equalsIgnoreCase(attribs.get("key")) && xmlr.hasNext()) {
		            				xmlr.next(); 
		            				vertexLabels.add(xmlr.getText()); 
		            			}
		            			else if (insideNode && !"labelV".equalsIgnoreCase(attribs.get("key"))) {
		            				vertexProperties.add(attribs.get("key")); 
		            			}
		            			else if (insideEdge && "labelE".equalsIgnoreCase(attribs.get("key")) && xmlr.hasNext()) {
		            				xmlr.next(); 
		            				edgeLabels.add(xmlr.getText()); 
		            			}
		            			else if (insideEdge && !"labelE".equalsIgnoreCase(attribs.get("key"))) {
		            				edgeProperties.add(attribs.get("key")); 
		            			}
		            		}	
		            	}
		                break;
		            case XMLEvent.END_ELEMENT:
		            	if (xmlr.getLocalName().equalsIgnoreCase("node")) {
		            		insideNode = false; 
		            	}
		            	else if (xmlr.getLocalName().equalsIgnoreCase("edge")) {
		            		insideEdge = false; 
		            	}
		            	break; 
		            case XMLEvent.PROCESSING_INSTRUCTION:
		                break; 
		            case XMLEvent.CHARACTERS:
		                break; 
		            case XMLEvent.COMMENT:
		            case XMLEvent.START_DOCUMENT:
		            case XMLEvent.END_DOCUMENT:
		            case XMLEvent.ENTITY_REFERENCE:
		            	break; 
		            case XMLEvent.ATTRIBUTE:
		            	System.out.println("attribute"); 
		            	break; 
		            case XMLEvent.DTD:
		            	break; 
		            case XMLEvent.CDATA:
		            	System.out.println("CDATA"); 
		                break; 
		            case XMLEvent.SPACE:
		                break; 
		        }

			}
	}
	
	public final boolean checkSanity (HashSet<String> vertexLabels,  
											HashSet<String> vertexProperties, 
											HashSet<String> edgeLabels, 
											HashSet<String> edgeProperties) 
	{
		String VL_LABEL = "vertexLabels";
		String VP_LABEL = "vertexProperties";  
		String EL_LABEL = "edgeLabels"; 
		// String EP_LABEL = "edgeProperties"; 
		
		boolean everythingOK = true; 
		HashSet<String> auxSet = null; 
		HashMap<String, HashSet<String>> collisions = new HashMap<String, HashSet<String>>(); 
		/* reserved keywords: vertex, element, edge, property, label, key */ 
		
		everythingOK = containsNoReservedWord(vertexLabels); 
		if (!everythingOK) log.error("Vertex labels containing reserved words"); 
		
		everythingOK &= containsNoReservedWord (vertexProperties);
		if (!everythingOK) log.error("Vertex properties containing reserved words");
		
		everythingOK &= containsNoReservedWord (edgeLabels); 
		if (!everythingOK) log.error("Edge labels containing reserved words"); 
		
		everythingOK &= containsNoReservedWord(edgeProperties); 
		if (!everythingOK) log.error("Edge properties containing reserved words"); 
		
		// we check the intersection of the different label names
		// to avoid collisions
		auxSet = new HashSet<String>(vertexLabels); 
		auxSet.retainAll(vertexProperties);
		collisions.put(VL_LABEL, auxSet); 
		everythingOK = auxSet.isEmpty();
		
		auxSet = new HashSet<String>(vertexLabels); 
		auxSet.retainAll(edgeLabels);
		// we keep track of the potential collisions is everything is ok, this should be empty
		collisions.get(VL_LABEL).addAll(auxSet);
		everythingOK = auxSet.isEmpty();
		
		auxSet = new HashSet<String>(vertexLabels); 
		auxSet.retainAll(edgeLabels);
		collisions.get(VL_LABEL).addAll(auxSet); 
		everythingOK = auxSet.isEmpty();
		
		auxSet = new HashSet<String>(vertexProperties); 
		auxSet.retainAll(edgeLabels);
		collisions.put(VP_LABEL, auxSet); 
		everythingOK = auxSet.isEmpty();
	
		auxSet = new HashSet<String>(vertexProperties); 
		auxSet.retainAll(edgeProperties);
		collisions.get(VP_LABEL).addAll(auxSet); 
		everythingOK = auxSet.isEmpty();
			
		auxSet = new HashSet<String>(edgeLabels); 
		auxSet.retainAll(edgeProperties);
		collisions.put(EL_LABEL, auxSet); 
		everythingOK = auxSet.isEmpty();
		
		if (!everythingOK) {
			if (!collisions.get(VL_LABEL).isEmpty()) {
				log.error("collisions with the vertex labels: "); 
				for (String lab: collisions.get(VL_LABEL)) {
					log.error(lab); 
				}
			}
			if (!collisions.get(VP_LABEL).isEmpty()) {
				log.error("collisions with the vertex properties: "); 
				for (String lab: collisions.get(VP_LABEL)) {
					log.error(lab); 
				}
			}
			if (!collisions.get(EL_LABEL).isEmpty()) {
				log.error("collisions with the edge labels: "); 
				for (String lab: collisions.get(EL_LABEL)) {
					log.error(lab); 
				}
			}
		}
		
		return everythingOK; 
	}
	
	public final void writeGroovyScript (String filename, 
											HashSet<String> vertexLabels,
											HashSet<String> vertexProperties, 
											HashSet<String> edgeLabels,
											HashSet<String> edgeProperties, 
											boolean createKeyIndex, 
											String key) throws FileNotFoundException {
		
		try ( PrintStream out = new PrintStream (
									new BufferedOutputStream ( 
										new FileOutputStream (filename ) ) )  				
			)
		{
			// we build the schema with the default settings 
			// some tunning might be required depending on the particular scenario
			out.println("graph.tx().commit()"); 
			out.println("m = graph.openManagement()"); 
			for (String vl: vertexLabels) {
				out.println("m.makeVertexLabel('"+vl+"').make()"); 
			}
			for (String vp: vertexProperties) {
				out.println("m.makePropertyKey('"+vp+"').dataType(String.class).make()"); 
			}
			for (String el: edgeLabels) {
				out.println("m.makeEdgeLabel('"+el+"').multiplicity(MULTI).make()"); 
			}
			for (String ep: edgeProperties) {
				out.println("m.makePropertyKey('"+ep+"').dataType(String.class).make()"); 
			}
			out.println("m.commit()"); 
			out.println(""); 
			
			if (createKeyIndex) {
				out.println("graph.tx().commit()"); 
				out.println("m = graph.openManagement()");
				out.println("prop = m.getPropertyKey('"+key+"')"); 
				out.println("m.buildIndex('"+key+"Comp', Vertex.class).addKey(prop).buildCompositeIndex()"); 
				out.println("prop = m.getPropertyKey('"+key+"')"); 
				out.println("m.buildIndex('"+key+"Mixed', Vertex.class).addKey(prop).buildMixedIndex('search')"); 
				out.println("m.commit()"); 
				
				//Wait for the index to become available

				out.println("ManagementSystem.awaitGraphIndexStatus(graph, '"+key+"Comp').call()");
				out.println("ManagementSystem.awaitGraphIndexStatus(graph, '"+key+"Mixed').call()"); 
				out.println("graph.tx().commit()"); 

				out.println("m = graph.openManagement()"); 

				out.println("idx = m.getGraphIndex('"+key+"Comp')"); 
				out.println("m.updateIndex(idx, SchemaAction.ENABLE_INDEX)"); 
				out.println("idx = m.getGraphIndex('"+key+"Mixed')"); 
				out.println("m.updateIndex(idx, SchemaAction.ENABLE_INDEX)"); 

				out.println("m.commit()"); 
				out.println("graph.tx().commit()"); 
			}
			out.flush();
		}
		
		
	}
	
	public static boolean containsNoReservedWord(HashSet<String> set) {
			
			return !( set.contains("vertex") || 
					set.contains("element") || 
					set.contains("edge") || 
					set.contains("property") || 
					set.contains("label") || 
					set.contains("key") ); 
		}
	
	public final static void printSchemaInformation(HashSet<String> vertexLabels, 
															HashSet<String> vertexProperties, 
															HashSet<String> edgeLabels, 
															HashSet<String> edgeProperties) {
			System.out.println("----------------"); 
			System.out.println("Vertex labels::"); 
			System.out.println("----------------"); 
			for (String s: vertexLabels) {
				System.out.println(s); 
			}
			System.out.println("----------------"); 
			System.out.println("Vertex properties::"); 
			System.out.println("----------------"); 
			for (String s: vertexProperties) {
				System.out.println(s); 
			}
			System.out.println("----------------"); 
			System.out.println("Edge labels::"); 
			System.out.println("----------------"); 
			for (String s: edgeLabels) {
				System.out.println(s); 
			}
			System.out.println("----------------"); 
			System.out.println("Edge Properties::"); 
			System.out.println("----------------"); 
			for (String s: edgeProperties) {
				System.out.println(s); 
			}
		}
	
	
	// The main (it's currently outside spring as it doesn't need any configuration)
	public static void main ( String... args )
	{
		int exitCode = 0; 

		try {
			var cli = new JanusgraphSchemaGatherer() ;
			var cmd = new CommandLine ( cli);
			exitCode = cmd.execute ( args );
		}
		catch ( Throwable ex ) 
		{
			ex.printStackTrace ( System.err );
			exitCode = 1;
		}
		finally 
		{
			System.exit ( exitCode );
		}			
	}

			
}
	

