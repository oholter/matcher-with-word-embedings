package org.trainertest.com;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mappings.evaluation.EmbeddingsEvaluator;
import mappings.trainer.OntologyProjector;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.TestRunUtils;
import mappings.walks_generator.Walks;

public class EmbeddingsTest {

	public static void main(String[] args) throws Exception {
		String firstOntologyFile = TestRunUtils.firstOntologyFile;
		String secondOntologyFile = "file:" + TestRunUtils.secondOntologyFile;
		String referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
		String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
		double equalityThreshold = TestRunUtils.equalityThreshold;
		double fractionOfMappings = TestRunUtils.fractionOfMappings;
		String walksType = TestRunUtils.walksType;
		String relatedConceptsPath = TestRunUtils.relatedConceptsPath;
		
		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
		BasicConfigurator.configure();
		
//		OntologyReader reader = new OntologyReader();
//		reader.setFname(secondOntologyFile); // program now tests EKAW only
//		reader.readOntology();
//		OWLOntology onto1 = reader.getOntology();
		
		System.out.println(secondOntologyFile);
		OntologyProjector projector = new OntologyProjector(secondOntologyFile);
		projector.projectOntology();
		projector.saveModel("/home/ole/master/test_onto/ekaw_test.ttl");
		
		Walks walks = new Walks("/home/ole/master/test_onto/ekaw_test.ttl", walksType);
		walks.generateWalks();
		String walksFile = walks.getOutputFile();
		
		String modelsFile = currentDir + "/temp/out.txt";
//		String modelsFile = "/home/ole/src/thesis/py/model.bin";
		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer(walksFile, modelsFile);
		trainer.train();

		EmbeddingsEvaluator evaluator = new EmbeddingsEvaluator(trainer, relatedConceptsPath, equalityThreshold);
		evaluator.evaluate();
	}

}
