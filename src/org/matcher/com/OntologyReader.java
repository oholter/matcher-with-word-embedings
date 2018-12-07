package org.matcher.com;

import java.io.File;

import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

public class OntologyReader {
	OWLOntologyManager man = OWLManager.createOWLOntologyManager();
	String fname;
	OWLOntology ontology;
	OWLReasoner reasoner;
	OWLDataFactory dataFactory;
	
	public OntologyReader() {}

	public void readOntology() throws Exception {
		File file = new File(fname);
		ontology = man.loadOntologyFromOntologyDocument(file);
		dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
	}
	
	public void performReasoning() {
		OWLReasonerFactory rf = new ReasonerFactory();
		reasoner = rf.createReasoner(ontology);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
	}

	public OWLOntologyManager getMan() {
		return man;
	}

	public void setMan(OWLOntologyManager man) {
		this.man = man;
	}

	public String getFname() {
		return fname;
	}

	public void setFname(String fname) {
		this.fname = fname;
	}

	public OWLOntology getOntology() {
		return ontology;
	}

	public void setOntology(OWLOntology ontology) {
		this.ontology = ontology;
	}

	public OWLReasoner getReasoner() {
		return reasoner;
	}

	public void setReasoner(OWLReasoner reasoner) {
		this.reasoner = reasoner;
	}

	public OWLDataFactory getDataFactory() {
		return dataFactory;
	}

	public void setDataFactory(OWLDataFactory dataFactory) {
		this.dataFactory = dataFactory;
	}
	
	public static void writeOntology(OWLOntology ontology, String fileName) {
		OWLOntologyManager man = ontology.getOWLOntologyManager();
		IRI docIri = IRI.create(fileName);
		try {
			man.saveOntology(ontology, new OWLXMLDocumentFormat(), docIri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println(fileName + " written successfully");
	}
}
