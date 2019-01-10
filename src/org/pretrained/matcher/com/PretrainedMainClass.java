package org.pretrained.matcher.com;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trainer.com.OntologyReader;
import org.trainer.com.WordEmbeddingsTrainer;

public class PretrainedMainClass {
	private Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
	static String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();

	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		OntologyReader reader = new OntologyReader();
		reader.setFname("/home/ole/master/test_onto/anatomy-dataset/mouse.owl");
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname("/home/ole/master/test_onto/anatomy-dataset/human.owl");
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer(currentDir + "/temp/out.txt", currentDir + "/temp/fil9.model");
		trainer.loadModel();

		CandidateFinder finder = new CandidateFinder(onto1, onto2);
		finder.createMappings();

//		CandidateFinder finder = new CandidateFinder(onto1, onto2);
//		finder.createMappings();
		OWLOntology o = finder.getMappings();
		OntologyReader.writeOntology(o, "file:/home/ole/master/test_onto/conference_mappings.owl", "owl");
	}

}
