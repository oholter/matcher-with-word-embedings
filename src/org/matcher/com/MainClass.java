package org.matcher.com;

import org.semanticweb.owlapi.model.OWLOntology;

public class MainClass {

	public static void main(String[] args) throws Exception {
		OntologyReader reader = new OntologyReader();
		reader.setFname("/home/ole/master/test_onto/ekaw.owl");
		reader.readOntology();
		OWLOntology ekaw = reader.getOntology();
		
		reader.setFname("/home/ole/master/test_onto/cmt.owl");
		reader.readOntology();
		OWLOntology sofsem = reader.getOntology();
		
		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[]{ekaw, sofsem});
		OntologyReader.writeOntology(mergedOnto, "file:/home/ole/master/test_onto/merged.owl", "owl");
		
		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/merged.owl");
		projector.projectOntology();
		projector.saveModel("/home/ole/master/test_onto/merged.ttl");
		
//		CandidateFinder finder = new CandidateFinder(ekaw, sofsem);
//		finder.createMappings();
//		OWLOntology o = finder.getMappings();
//		OntologyReader.writeOntology(o, "file:/home/ole/master/test_onto/conference_mappings.owl");
	}

}
