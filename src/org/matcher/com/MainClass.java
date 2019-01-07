package org.matcher.com;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trainer.com.WordEmbeddingsTrainer;

public class MainClass {
	private Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
	static String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();

	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		OntologyReader reader = new OntologyReader();
		reader.setFname("/home/ole/master/test_onto/ekaw.owl");
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname("/home/ole/master/test_onto/cmt.owl");
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		/*
		 * OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new
		 * OWLOntology[]{onto1, onto2}); OntologyReader.writeOntology(mergedOnto,
		 * "file:/home/ole/master/test_onto/merged.owl", "owl");
		 * 
		 * OntologyProjector projector = new
		 * OntologyProjector("file:/home/ole/master/test_onto/merged.owl");
		 * projector.projectOntology();
		 * projector.saveModel("/home/ole/master/test_onto/merged.ttl");
		 * 
		 * Walks walks = new Walks();
		 * walks.cleanDataSet("/home/ole/master/test_onto/merged.ttl");
		 * walks.loadFromRdfFile("/home/ole/master/test_onto/merged.ttl");
		 * walks.generateWalks(); String walksFile = walks.getOutputFile();
		 * 
		 * WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer(walksFile,
		 * currentDir + "/temp/out.txt"); // trainer.stripAccents(); trainer.train();
		 */
		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer("/home/ole/master/test_onto/merged.ttl",
				currentDir + "/temp/out.txt");

		trainer.loadModel();

		CandidateFinder2 finder = new CandidateFinder2(onto1, onto2, currentDir + "/temp/out.txt");
		finder.createMappings();

//		CandidateFinder finder = new CandidateFinder(onto1, onto2);
//		finder.createMappings();
		OWLOntology o = finder.getMappings();
		OntologyReader.writeOntology(o, "file:/home/ole/master/test_onto/conference_mappings.owl", "owl");
	}

}
