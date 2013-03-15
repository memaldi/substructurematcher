package eu.deustotech.internet.substructurematcher;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
	
	
	public static void main(String args[]) {
		
		logger.info("Processing substructures...");
		
		Map<Tuple<String, String, String>, AlignmentProcess> alignmentMap = new HashMap<Tuple<String, String, String>, AlignmentProcess>();
		
		//TODO: use args[]
		loadConfigXML("config.xml");
		List<LabeledGraph> labeledGraphs = parseGraphs(substructureDir);
		for (LabeledGraph graph1 : labeledGraphs) {
			Set<URI> graph1Ontologies = searchOntologies(graph1);
			for (LabeledGraph graph2 : labeledGraphs) {
				if (!graph1.equals(graph2)) {
					Set<URI> graph2Ontologies = searchOntologies(graph2);
					for (URI ontology1 : graph1Ontologies) {
						for (URI ontology2 : graph2Ontologies) {
							for (String alignmentMethod : alignmentClasses) {
								AlignmentProcess aProcess = null;
								if (!alignmentMap.containsKey(new Tuple<String, String, String>(ontology1.toString(), ontology2.toString(), alignmentMethod))) {
									aProcess = applyAlignment(ontology1, ontology2, alignmentMethod);
									alignmentMap.put(new Tuple<String, String, String>(ontology1.toString(), ontology2.toString(), alignmentMethod), aProcess);
								} else {
									aProcess = alignmentMap.get(new Tuple<String, String, String>(ontology1.toString(), ontology2.toString(), alignmentMethod));					
								}
								Set<String> sourceLabels = getGraphLabels(graph1);
								Set<String> targetLabels = getGraphLabels(graph2);
								Enumeration<Cell> cells = aProcess.getElements();
								
								while (cells.hasMoreElements()) {
									Cell cell = cells.nextElement();
									String label1 = cell.getObject1().toString().replace("<", "").replace(">", "");
									String label2 = cell.getObject2().toString().replace("<", "").replace(">", "");
									if (sourceLabels.contains(label1) && targetLabels.contains(label2)) {
										System.out.println(String.format("%s - %s (%s)", label1, label2, cell.getStrength()));
									}
								}
							}
						}
					}
				}
			}
		}
		System.out.println(alignmentMap.size());
		
	}

	private static Set<String> getGraphLabels(LabeledGraph graph) {
		Set<String> sourceLabels = new HashSet<String>();
		for (Vertex vertex : graph.geVertex()) {
			sourceLabels.add(vertex.getLabel());
		}
		for (Edge edge : graph.getEdges()) {
			sourceLabels.add(edge.getLabel());
		}
		return sourceLabels;
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
	
	private static List<LabeledGraph> parseGraphs(String substructureDir) {
		File folder = new File(substructureDir);
		List<LabeledGraph> graphs = new ArrayList<LabeledGraph>();
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
				}
				graphs.add(graph);
			} catch (FileNotFoundException e) {
				logger.error(e.getMessage());
			} catch (IOException e) {
				logger.error(e.getMessage());
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		return graphs;
	}
	
	private static URI getPrefix(URI uri) {
		try {
			if (uri.toURL().getRef() != null) { 
				return new URI(uri.toString().split("#")[0] + "#");
			} else {
				String [] splittedURL = uri.toString().split("/");
				String ontologyURL = "";
				for (int i = 0; i < splittedURL.length - 1; i++) {
					ontologyURL += splittedURL[i] + "/";
				}
				return new URI(ontologyURL);
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return uri;
	}
	
	private static Set<URI> searchOntologies(LabeledGraph graph) {
		
		Set<URI> ontologySet = new HashSet<URI>();
		try {
			for (Vertex vertex : graph.geVertex()) {
				ontologySet.add(getPrefix(new URI(vertex.getLabel())));
			}
			for (Edge edge : graph.getEdges()) {
				ontologySet.add(getPrefix(new URI(edge.getLabel())));
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		return ontologySet;
	}

	
}
