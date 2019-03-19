package mappings.trainer;

import java.util.TreeSet;

import org.eclipse.rdf4j.model.Model;

import uio.ifi.ontology.toolkit.projection.controller.triplestore.RDFoxSessionManager;
import uio.ifi.ontology.toolkit.projection.model.GraphProjection;
import uio.ifi.ontology.toolkit.projection.model.entities.Concept;
import uio.ifi.ontology.toolkit.projection.view.OptiqueVQSAPI;

public class OntologyProjector {

	String filePath;
	GraphProjection graph;
	OptiqueVQSAPI vqs;
	Model model;

	public OntologyProjector(String file) {
		this.filePath = file;
	}

	public void projectOntology() throws Exception {
		RDFoxSessionManager man = new RDFoxSessionManager();
		man.createNewSessionForEmbeddings(filePath);
//		vqs = new OptiqueVQSAPI(man);
//		vqs.loadOntologySession(filePath);
		graph = man.getSession(filePath).getGraph();
		model = graph.getRDFModel();
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
		graph.saveModel(outFile);
		System.out.println("Saved projection as: " + outFile);

	}

}
