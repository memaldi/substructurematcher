package eu.deustotech.internet.substructurematcher;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.semanticweb.owl.align.AlignmentProcess;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.inrialpes.exmo.align.impl.BasicParameters;
import fr.inrialpes.exmo.align.impl.method.NameAndPropertyAlignment;

public class Main {

	static String substructureDir = "";
	static Set<String> alignmentClasses = new HashSet<String>();
	
	public static void main(String args[]) {
		
		//TODO: use args[]
		loadConfigXML("config.xml");
		
		Set<URI> ontologySet = searchOntologies(substructureDir);
		
		
		for (URI onto1 : ontologySet) {
			for (URI onto2 : ontologySet) {
				if (onto1 != onto2) {
					for (String alignmentClass : alignmentClasses) {
						AlignmentProcess aProcess = applyAlignment(onto1, onto2, alignmentClass);
					}
				}
			}
		}
		//System.out.println(ontologySet);
	
	}

	private static void loadConfigXML(String configFile) {
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	        Document doc = docBuilder.parse(configFile);
	        
	        substructureDir = doc.getElementsByTagName("substructureDir").item(0).getTextContent();
	        
	        NodeList alignmentNodes = doc.getElementsByTagName("alignment");
	        for (int i = 0; i < alignmentNodes.getLength(); i++) {
	        	Node alignmentNode = alignmentNodes.item(i);
	        	NodeList alignmentChilds = alignmentNode.getChildNodes();
	        	for (int j = 0; j < alignmentChilds.getLength(); j++) {
	        		Node child = alignmentChilds.item(j);
	        		if (child.getNodeName().equals("name")) {
	        			alignmentClasses.add(child.getTextContent());
	        		}
	        	}
	        }
	        
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static AlignmentProcess applyAlignment(URI onto1, URI onto2, String alignment) {
		AlignmentProcess aProcess = null;
		try {
			Class alignmentClass = Class.forName(alignment);
			aProcess = (AlignmentProcess) alignmentClass.newInstance();
			aProcess =  new  NameAndPropertyAlignment();
			Properties params = new BasicParameters();
			
			System.out.println(onto1 + " - " + onto2);
			
			aProcess.init(onto1, onto2);
			aProcess.align(null, params);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Error processing " + onto1 + " - " + onto2);
		}
		return aProcess;
	}

	private static Set<URI> searchOntologies(String substructureDir) {
		File folder = new File(substructureDir);
		
		Set<URI> ontologySet = new HashSet<URI>();
		
		for (final File fileEntry : folder.listFiles()) {
			try {
				FileInputStream fstream = new FileInputStream(fileEntry.getAbsolutePath());
				DataInputStream inputStream = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
				String strLine;
				while((strLine = br.readLine()) != null) {
					URI uri = null;
					if (strLine.startsWith("v")) {
						uri = new URI(strLine.substring(4));
					} else if (strLine.startsWith("d")) {
						uri = new URI(strLine.substring(6));
					}
					
					if (uri.toURL().getRef() != null) { 
						ontologySet.add(new URI(uri.toString().split("#")[0] + "#"));
					} else {
						String [] splittedURL = uri.toString().split("/");
						String ontologyURL = "";
						for (int i = 0; i < splittedURL.length - 1; i++) {
							ontologyURL += splittedURL[i] + "/";
						}
						ontologySet.add(new URI(ontologyURL));
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return ontologySet;
	}
}
