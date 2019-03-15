package org.trainertest.com;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.AlignmentsReader;
import io.OAEIAlignmentsReader;
import io.OntologyReader;
import mapping.object.MappingObjectStr;
import mappings.candidate_finder.AllRelationsAnchorCandidateFinder;
import mappings.candidate_finder.AnchorsCandidateFinder;
import mappings.candidate_finder.BestAnchorsCandidateFinder;
import mappings.candidate_finder.DisambiguateClassAnchorsFinder;
import mappings.candidate_finder.TranslationMatrixCandidateFinder;
import mappings.candidate_finder.TwoDocumentsCandidateFinder;
import mappings.evaluation.ClassMappingsEvaluator;
import mappings.evaluation.MappingsEvaluator;
import mappings.trainer.OntologyProjector;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.TestRunUtils;
import mappings.walks_generator.Walks;

public class testRuns {

	public static PrintWriter out;
	public static String testResultsFile = "/home/ole/master/test_onto/test_runs.txt";
	public static double[] precisions;
	public static double[] recalls;
	public static double[] fmeasures;
	public static double fractionOfMappings = TestRunUtils.fractionOfMappings;
	public static Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
	public static String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
	
	public static String firstOntologyFile = TestRunUtils.firstOntologyFile;
	public static String secondOntologyFile = TestRunUtils.secondOntologyFile;
	public static String referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
	public static String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
	public static double equalityThreshold = TestRunUtils.equalityThreshold;
	public static String walksType = TestRunUtils.walksType;
	public static String modelPath = TestRunUtils.modelPath;
	public static String nameSpaceString = TestRunUtils.nameSpaceString1;
	public static String mergedOwlPath = TestRunUtils.mergedOwlPath;
	

	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();

		int numberOfRuns = 1;
//		String[] anchorCandidateTypes = new String[] { "allrelationsanchorcandidatefinder",
//				"bestcandidatefinder", "disambiguateclassanchorsfinder", twodocuments" };

		String type = "bestcandidatefinder";

		String[] candidateTypes = new String[] { "translationmatrixcandidatefinder", "pretrainedvectorcandidatesfinder" };

		File outputFile = new File(testResultsFile);
		out = new PrintWriter(outputFile);
//		out.println("--------------------------------------------");
//		out.println();
		out.println("anchors,precision,recall,fmeasure");

		// This will create 5 lines, each with 0.2 less mappings
		for (; fractionOfMappings > 0; fractionOfMappings -= 0.2) {
			precisions = new double[numberOfRuns];
			recalls = new double[numberOfRuns];
			fmeasures = new double[numberOfRuns];

//			out.println(type.toUpperCase() + ":");

			for (int i = 0; i < numberOfRuns; i++) {
//				out.print("" + (i+1) + ": ");
				long startTime = System.nanoTime();
				System.out.println("Now starting fraction :" + fractionOfMappings + " test nr: " + (i + 1));
				runTest(type, i);
				long endTime = System.nanoTime();
				long elapsedTimeSec = (endTime - startTime) / 1000000000;
				System.out.println("execution time: " + elapsedTimeSec + " sec");
			}
			double totPrecision = 0;
			double totRecall = 0;
			double totFmeasure = 0;

			for (double d : precisions) {
				totPrecision += d;
			}

			for (double d : recalls) {
				totRecall += d;
			}

			for (double d : fmeasures) {
				totFmeasure += d;
			}
//			out.println();
//			out.println("average scores");
			out.printf("%.0f", fractionOfMappings * 100);
			out.print(",");
			out.printf("%.2f", totPrecision / numberOfRuns);
			out.print(",");
			out.printf("%.2f", totRecall / numberOfRuns);
			out.print(",");
			out.printf("%.2f", totFmeasure / numberOfRuns);
			out.println();
		}

		out.close();
	}

	public static void runTest(String finderType, int runNo) throws Exception {


		OntologyReader reader = new OntologyReader();
		reader.setFname(firstOntologyFile);
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname(secondOntologyFile);
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		AnchorsCandidateFinder finder = null;

		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });

		if (finderType.toLowerCase().equals("allrelationsanchorcandidatefinder")) {
			finder = new AllRelationsAnchorCandidateFinder(onto1, onto2, mergedOnto, modelPath, equalityThreshold);
//			out.println("AllRelationsAnchorCandidateFinder");
		} else if (finderType.toLowerCase().equals("bestcandidatefinder")) {
//			out.println("BestRelationsAnchorCandidateFinder");
			finder = new BestAnchorsCandidateFinder(onto1, onto2, mergedOnto, modelPath, equalityThreshold);
		} else if (finderType.toLowerCase().equals("disambiguateclassanchorsfinder")) {
			finder = new DisambiguateClassAnchorsFinder(onto1, onto2, mergedOnto, modelPath, equalityThreshold);
//			out.println("DisambiguateRelationsAnchorCandidateFinder");
		} else if (finderType.toLowerCase().equals("translationmatrixcandidatefinder")) {
			runTranslationMatrixTest(runNo);
			return;
		} else if (finderType.toLowerCase().equals("twodocuments")) {
			runTwoDocumentsTest(runNo);
			return;
		} else {
			System.out.println("Unknown candidate finder: " + finderType);
			System.exit(0);
		}

		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, onto1, onto2);
		List<MappingObjectStr> mappings = alignmentsReader.getMappings();
		for (int i = 0; i < (mappings.size() * fractionOfMappings); i++) {
			MappingObjectStr mapping = mappings.get(i);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}

		finder.addAnchorsToOntology(mergedOnto);
		OntologyReader.writeOntology(mergedOnto, mergedOwlPath, "owl");
		OntologyProjector projector = new OntologyProjector(mergedOwlPath);
		projector.projectOntology();
		projector.saveModel(modelPath);
		Walks walks = new Walks("/home/ole/master/test_onto/merged.ttl", walksType);
		walks.generateWalks();
		String walksFile = walks.getOutputFile();

		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer(walksFile, currentDir + "/temp/out.txt");
		trainer.train();
		finder.setTrainer(trainer);
		finder.createMappings();

		// Evaluation of the system
		System.out.println("--------------------------------------------");
		System.out.println("This system: " + finderType);
		MappingsEvaluator evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile,
				finder.getOutputAlignment().returnAlignmentFile().getFile(), finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
//		out.println("Precision: " + evaluator.calculatePrecision() + ", recall: " + evaluator.calculateRecall() + ", F-measure: " + evaluator.calculateFMeasure());
		System.out.println("--------------------------------------------");

		precisions[runNo] = evaluator.calculatePrecision();
		recalls[runNo] = evaluator.calculateRecall();
		fmeasures[runNo] = evaluator.calculateFMeasure();
	}
	
	public static void runTwoDocumentsTest(int runNo) throws Exception {
		OntologyReader reader = new OntologyReader();
		reader.setFname(firstOntologyFile);
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname(secondOntologyFile);
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		// For training of ontology start:
		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });
		TwoDocumentsCandidateFinder finder = new TwoDocumentsCandidateFinder(onto1, onto2, mergedOnto,
				currentDir + "/temp/out.txt", equalityThreshold, TestRunUtils.labelEqualityThreshold);

		/* Adding anchors by reading an alignments file */
		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, onto1, onto2);

		List<MappingObjectStr> mappings = alignmentsReader.getMappings();
		for (int i = 0; i < (mappings.size() * fractionOfMappings); i++) {
			MappingObjectStr mapping = mappings.get(i);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}
		if (fractionOfMappings > 0) {
			finder.addAnchorsToOntology(mergedOnto);
		}
		OntologyReader.writeOntology(mergedOnto, mergedOwlPath, "owl");

		OntologyProjector projector = new OntologyProjector(mergedOwlPath);
		projector.projectOntology();
		projector.saveModel(modelPath);

		Walks walks = new Walks(modelPath, walksType);
		walks.generateWalks();
		String walksFile = walks.getOutputFile();
		String labelOutputFile = walks.getLabelOutputFile();

		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer(walksFile, currentDir + "/temp/out.txt");
		trainer.train();
		finder.setTrainer(trainer);

		WordEmbeddingsTrainer labelTrainer = new WordEmbeddingsTrainer(labelOutputFile,
				currentDir + "/temp/label_out.txt");
		labelTrainer.train();
		finder.setLabelTrainer(labelTrainer);

		finder.createMappings(); // this runs the program

		// evaluating the mappings
		System.out.println("--------------------------------------------");
		System.out.println("The alignments file used to provide anchors: ");
		MappingsEvaluator evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile, logMapAlignmentsFile,
				finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");

		System.out.println("This system:");
		evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile, finder.getOutputAlignment().returnAlignmentFile().getFile(),
				finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");
		
		System.out.println("Dette er dette er dette er two documents!!!!!!!!!");
		
		precisions[runNo] = evaluator.calculatePrecision();
		recalls[runNo] = evaluator.calculateRecall();
		fmeasures[runNo] = evaluator.calculateFMeasure();
	}

	public static void runTranslationMatrixTest(int runNo) throws Exception {
		OntologyReader reader = new OntologyReader();
		reader.setFname(firstOntologyFile);
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname(secondOntologyFile);
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });
		OntologyReader.writeOntology(mergedOnto, mergedOwlPath, "owl");

		TranslationMatrixCandidateFinder finder = new TranslationMatrixCandidateFinder(onto1, onto2, modelPath,
				equalityThreshold, nameSpaceString);

		/* Adding anchors by reading an alignments file */
		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, onto1, onto2);

		List<MappingObjectStr> mappings = alignmentsReader.getMappings();
		for (int i = 0; i < (mappings.size() * fractionOfMappings); i++) {
			MappingObjectStr mapping = mappings.get(i);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}

		OntologyProjector projector = new OntologyProjector(mergedOwlPath);
		projector.projectOntology();
		projector.saveModel(modelPath);

		Walks walks = new Walks(modelPath, walksType);
		walks.generateWalks();
		String walksFile = walks.getOutputFile();

		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer(walksFile, currentDir + "/temp/out.txt");
		trainer.train();
		finder.setTrainer(trainer);

		finder.createMappings(); // this runs the program

		// evaluating the mappings
		System.out.println("--------------------------------------------");
		System.out.println("The alignments file used to provide anchors: ");
		MappingsEvaluator evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile, logMapAlignmentsFile,
				finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");

		System.out.println("This system:");
		evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile,
				finder.getOutputAlignment().returnAlignmentFile().getFile(), finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");
		precisions[runNo] = evaluator.calculatePrecision();
		recalls[runNo] = evaluator.calculateRecall();
		fmeasures[runNo] = evaluator.calculateFMeasure();
	}

}
