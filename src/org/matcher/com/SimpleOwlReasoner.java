package org.matcher.com;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.trainer.com.OntologyReader;

public class SimpleOwlReasoner {
	public static void main(String[] args) throws Exception {
		OntologyReader reader = new OntologyReader();
		reader.setFname("/home/ole/master/test_onto/confious.owl");
		reader.readOntology();
		OWLOntology onto = reader.getOntology();
		OWLOntologyManager man = onto.getOWLOntologyManager();
		OWLDataFactory factory = man.getOWLDataFactory();
		
		
		for (OWLSubClassOfAxiom ax : onto.getAxioms(AxiomType.SUBCLASS_OF)) {
			man.removeAxiom(onto, ax);
		}

		reader.performReasoning();
		System.out.println("Reasoning finished");
	}
}
