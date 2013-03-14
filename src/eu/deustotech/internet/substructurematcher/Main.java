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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.semanticweb.owl.align.AlignmentProcess;
import org.semanticweb.owl.align.Cell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fr.inrialpes.exmo.align.impl.BasicParameters;
import fr.inrialpes.exmo.align.impl.method.NameAndPropertyAlignment;

public class Main {

	static String substructureDir = "";
	static Set<String> alignmentClasses = new HashSet<String>();
	static Logger logger = LoggerFactory.getLogger(Main.class);
	static Set<String> entities = new HashSet<String>();
	static Set<LabeledGraph> labeledGraphs = new HashSet<LabeledGraph>();
	
	public static void main(String args[]) {
		
		logger.info("Processing substructures...");
		
		//TODO: use args[]
		loadConfigXML("config.xml");
		
		Set<URI> ontologySet = searchOntologies(substructureDir);
		
		Set<AlignmentProcess> alignmentSet = new HashSet<AlignmentProcess>();
		List<Cell> matchedCells = new ArrayList<Cell>();
		
		for (URI onto1 : ontologySet) {
			for (URI onto2 : ontologySet) {
				if (onto1 != onto2) {
					for (String alignmentClass : alignmentClasses) {
						AlignmentProcess aProcess = applyAlignment(onto1, onto2, alignmentClass);
						alignmentSet.add(aProcess);
						Enumeration<Cell> cells = aProcess.getElements();
						while(cells.hasMoreElements()) {
							Cell cell = cells.nextElement();
							if (entities.contains(cell.getObject1().toString()) || entities.contains(cell.getObject2().toString())) {								
								matchedCells.add(cell);
							}				
						}
					}
				}
			}
		}
		
		logger.info("Matching subgraphs...");
		
		for (LabeledGraph graph1 : labeledGraphs) {
			for (LabeledGraph graph2 : labeledGraphs) {
				if (graph1 != graph2) {
					
				}
			}
		}
		
	
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
			logger.error(e.getMessage());
		} catch (SAXException e) {
			logger.error(e.getMessage());
		} catch (IOException e) {
			logger.error(e.getMessage());
		}
	}

	private static AlignmentProcess applyAlignment(URI onto1, URI onto2, String alignment) {
		AlignmentProcess aProcess = null;
		try {
			Class alignmentClass = Class.forName(alignment);
			aProcess = (AlignmentProcess) alignmentClass.newInstance();
			aProcess =  new  NameAndPropertyAlignment();
			Properties params = new BasicParameters();
			
			logger.info(String.format("Creating alignment between <%s> and <%s>", onto1, onto2));
			
			aProcess.init(onto1, onto2);
			aProcess.align(null, params);
			
		} catch (Exception e) {
			logger.error(e.getMessage());
		}
		return aProcess;
	}

	private static Set<URI> searchOntologies(String substructureDir) {
		File folder = new File(substructureDir);
		
		Set<URI> ontologySet = new HashSet<URI>();
		SecureRandom prng = null;
		try {
			prng = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for (final File fileEntry : folder.listFiles()) {
			try {
				FileInputStream fstream = new FileInputStream(fileEntry.getAbsolutePath());
				DataInputStream inputStream = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
				String strLine;
				LabeledGraph graph = new LabeledGraph(prng.nextInt());
				while((strLine = br.readLine()) != null) {
					URI uri = null;
					if (strLine.startsWith("v")) {
						uri = new URI(strLine.substring(4));
						Vertex vertex = new Vertex(Integer.parseInt(strLine.substring(2, 3)), uri.toString());
						graph.addVertex(vertex);
					} else if (strLine.startsWith("d")) {
						uri = new URI(strLine.substring(6));
						Edge edge = new Edge(Integer.parseInt(strLine.substring(2, 3)), Integer.parseInt(strLine.substring(4, 5)), uri.toString());
						graph.addEdge(edge);
					}
					entities.add(String.format("<%s>", uri));
					
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
				labeledGraphs.add(graph);
			} catch (FileNotFoundException e) {
				logger.error(e.getMessage());
			} catch (IOException e) {
				logger.error(e.getMessage());
			} catch (URISyntaxException e) {
				logger.error(e.getMessage());
			}
		}
		
		return ontologySet;
	}
}
