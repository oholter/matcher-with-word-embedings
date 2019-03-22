package io;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.OWLEntityRenamer;

public class OntologyReader {
	OWLOntologyManager man = OWLManager.createOWLOntologyManager();
	String fname;
	OWLOntology ontology;
	OWLReasoner reasoner;
	OWLDataFactory dataFactory;

	public OntologyReader() {
	}

	public void readOntology() throws Exception {
		File file = new File(fname);
		ontology = man.loadOntologyFromOntologyDocument(file);
		dataFactory = ontology.getOWLOntologyManager().getOWLDataFactory();
	}

	public void performReasoning() {
		OWLReasonerFactory rf = new StructuralReasonerFactory();
		reasoner = rf.createReasoner(ontology);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
	}
	
	public OWLOntology readAllConferenceOntologies() throws Exception {
//		String[] conferenceOntologies = {"cmt", "cocus", "confious", "edas", "ekaw", "iasted", "micro", "paperdyne", "sigkdd", "sofsem"};
		String[] conferenceOntologies = {"cmt", "edas", "ekaw", "iasted", "micro", "sigkdd"};
		OntologyReader reader = new OntologyReader();
		ArrayList<OWLOntology> ontologies = new ArrayList<>();
		String path = "/home/ole/master/test_onto/";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		
		for (String ontologyFile : conferenceOntologies) {
			File f = new File(path + ontologyFile + ".owl");
			OWLOntology onto = man.loadOntologyFromOntologyDocument(f);
			ontologies.add(onto);
		}
		
		return mergeOntologies("merged", ontologies.toArray(new OWLOntology[ontologies.size()])); 
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

	public static void writeOntology(OWLOntology ontology, String fileName, String format) {
		OWLOntologyManager man = ontology.getOWLOntologyManager();
		IRI docIri = IRI.create(fileName);
		OWLDocumentFormat documentFormat;
		if (format.equals("owl")) {
			documentFormat = new OWLXMLDocumentFormat();
		} else if (format.equals("ttl")) {
			documentFormat = new TurtleDocumentFormat();
		} else if (format.equals("rdf")) {
			documentFormat = new RDFXMLDocumentFormat();
		} else {
			documentFormat = new OWLXMLDocumentFormat();
		}
		try {
			man.saveOntology(ontology, documentFormat, docIri);
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println(fileName + " written successfully");
	}

	public static OWLOntology mergeOntologies(String fileName, OWLOntology[] ontologies) throws Exception {
		IRI mergedOntologyIRI = IRI.create("http://mergedontology/" + fileName);
		Set<OWLAxiom> axioms = new HashSet<>();
		Set<OWLImportsDeclaration> imports = new HashSet<OWLImportsDeclaration>();
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology mergedOntology = null;

		try {
			for (OWLOntology ontology : ontologies) {
				axioms.addAll(ontology.getAxioms());
				imports.addAll(ontology.getImportsDeclarations());
				man.removeOntology(ontology);
			}
			mergedOntology = man.createOntology(mergedOntologyIRI);
			man.addAxioms(mergedOntology, axioms);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		// Adding the import declarations
		for (OWLImportsDeclaration decl : imports) {
			man.applyChange(new AddImport(mergedOntology, decl));
		}
		// rename individuals names to use the merged ontology's IRI
		renameIRIs(mergedOntologyIRI, ontologies, man);
		return mergedOntology;
	}

	private static void renameIRIs(IRI newIRI, OWLOntology[] ontologies, OWLOntologyManager man) {
		OWLEntityRenamer renamer = new OWLEntityRenamer(man, man.getOntologies());

		for (OWLOntology ontology : ontologies) {
			for (OWLEntity individual : ontology.getIndividualsInSignature()) {
				// replace the individual's old IRI with the new one E.g:
				// http://ontologyOld#name becomes newIRI#name
				IRI individualName = IRI
						.create(individual.getIRI().toString().replaceFirst("[^*]+(?=#|;)", newIRI.toString()));
				man.applyChanges(renamer.changeIRI(individual.getIRI(), individualName));
			}
		}
	}
}
