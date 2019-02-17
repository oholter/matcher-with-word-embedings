package mappings.trainer;

import java.util.TreeSet;

import uio.ifi.ontology.toolkit.projection.controller.triplestore.RDFoxSessionManager;
import uio.ifi.ontology.toolkit.projection.model.GraphProjection;
import uio.ifi.ontology.toolkit.projection.model.entities.Concept;
import uio.ifi.ontology.toolkit.projection.view.OptiqueVQSAPI;

public class OntologyProjector {

	String filePath;
	GraphProjection graph;
	OptiqueVQSAPI vqs;

	public OntologyProjector(String file) {
		this.filePath = file;
	}

	public void projectOntology() throws Exception {
		RDFoxSessionManager man = new RDFoxSessionManager();
		vqs = new OptiqueVQSAPI(man);
		vqs.loadOntologySession(filePath);
		graph = vqs.getSessionManager().getSession(filePath).getGraph();

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
