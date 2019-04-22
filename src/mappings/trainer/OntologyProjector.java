package mappings.trainer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.TreeSet;

import org.eclipse.rdf4j.model.Model;

import uio.ifi.ontology.toolkit.projection.controller.GraphProjectionManager;
import uio.ifi.ontology.toolkit.projection.controller.triplestore.RDFoxProjectionManager;
import uio.ifi.ontology.toolkit.projection.controller.triplestore.RDFoxSessionManager;
import uio.ifi.ontology.toolkit.projection.model.GraphProjection;
import uio.ifi.ontology.toolkit.projection.model.entities.Concept;
import uio.ifi.ontology.toolkit.projection.view.OptiqueVQSAPI;

public class OntologyProjector {

	String filePath;
	GraphProjection graph;
	OptiqueVQSAPI vqs;
	Model model;
	RDFoxSessionManager man = new RDFoxSessionManager();

	public OntologyProjector(String file) {
		this.filePath = file;
	}

	public void projectOntology() throws Exception {
		man.createNewSessionForEmbeddings(filePath);
//		vqs = new OptiqueVQSAPI(man);
//		vqs.loadOntologySession(filePath);
		graph = man.getSession(filePath).getGraph();
		model = graph.getRDFModel();
	}

	/**
	 * RDFox accepts "string" sameAs "string" as a triple in .ttl, this is jena does
	 * not accept this, must remove this from the file before reading with jena
	 * 
	 * @param outFile
	 */
	public void cleanSavedModel(String outFile) {
		try {
			String tmpFile = "tmp.txt";
			File out = new File(outFile);
			File tmp = new File(tmpFile);
			BufferedReader reader = new BufferedReader(new FileReader(out));
			BufferedWriter writer = new BufferedWriter(new FileWriter(tmp));
			String line = null;
			int numLines = 0;
			int eliminatedLines = 0;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("\"") && !(line.endsWith(";"))) {
					String toEliminate = line;
					while (toEliminate != null) {
						eliminatedLines++;
						if (toEliminate.endsWith("\" .")) {
							break;
						}
						System.out.println("eliminated: " + toEliminate);
						toEliminate = reader.readLine();
					}
				} else {
					numLines++;
					writer.write(line + "\n");
//					System.out.println(line + "\n");
				}
			}
			writer.flush();
			writer.close();
			tmp.renameTo(out);
			System.out.println("Written " + numLines + " lines");
			System.out.println("Eliminated " + eliminatedLines + " lines");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public Model getModel() {
		return model;
	}

	public void printConcepts() {
		TreeSet<Concept> concepts = vqs.getSessionManager().getSession("file:/home/ole/master/test_onto/ekaw.owl")
				.getCoreConcepts();

		for (Concept c : concepts) {
			System.out.println(c.getIri());
		}
	}

	public void saveModel(String outFile) throws Exception {
//		graph.saveModel(outFile);
		GraphProjectionManager pMan = man.getSession(filePath);
		RDFoxProjectionManager rMan;
		if (pMan instanceof RDFoxProjectionManager) {
			rMan = (RDFoxProjectionManager) man.getSession(filePath);
			rMan.exportMaterielizationSnapshot(outFile);
			cleanSavedModel(outFile);
		}
		System.out.println("Saved projection as: " + outFile);
	}

}
