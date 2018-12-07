package org.matcher.com;

import org.semanticweb.owlapi.model.OWLOntology;

public class MainClass {

	public static void main(String[] args) throws Exception {
		OntologyReader reader = new OntologyReader();
		reader.setFname("/home/ole/master/test_onto/product.owl");
		reader.readOntology();
		OWLOntology ekaw = reader.getOntology();
		
		reader.setFname("/home/ole/master/test_onto/volume.owl");
		reader.readOntology();
		OWLOntology sofsem = reader.getOntology();
		
		CandidateFinder finder = new CandidateFinder(ekaw, sofsem);
		finder.createMappings();
		OWLOntology o = finder.getMappings();
		OntologyReader.writeOntology(o, "file:/home/ole/master/test_onto/conference_mappings.owl");
	}

}
