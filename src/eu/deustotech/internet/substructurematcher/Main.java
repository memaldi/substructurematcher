package eu.deustotech.internet.substructurematcher;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.semanticweb.owl.align.AlignmentProcess;
import fr.inrialpes.exmo.align.impl.BasicParameters;
import fr.inrialpes.exmo.align.impl.method.NameAndPropertyAlignment;

public class Main {

	public static void main(String args[]) {
		
		Properties configFile = new Properties();
		InputStream in;
		try {
			in = new FileInputStream("config.properties");
			configFile.load(in);
			in.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		String substructureDir = configFile.getProperty("SUBSTRUCTURE_DIR");
		
		//Set<URI> ontologySet = searchOntologies(substructureDir);
		Set<URI> ontologySet = new HashSet<URI>();
		try {
			ontologySet.add(new URI("http://aktors.org/ontology/portal"));
			ontologySet.add(new URI("http://purl.org/ontology/bibo/"));
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for (URI onto1 : ontologySet) {
			for (URI onto2 : ontologySet) {
				if (onto1 != onto2) {
					AlignmentProcess aProcess = applyAlignment(onto1, onto2);
				}
			}
		}
		//System.out.println(ontologySet);
	
	}

	private static AlignmentProcess applyAlignment(URI onto1, URI onto2) {
		AlignmentProcess aProcess = null;
		try {
			Class alignmentClass = Class.forName("fr.inrialpes.exmo.align.impl.method.NameAndPropertyAlignment");
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
