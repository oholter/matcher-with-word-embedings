package mappings.evaluation;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.AlignmentsReader;
import io.OAEIAlignmentsReader;
import io.OntologyReader;
import mappings.candidate_finder.AllRelationsAnchorCandidateFinder;
import mappings.candidate_finder.AnchorsCandidateFinder;
import mappings.candidate_finder.BestAnchorsCandidateFinder;
import mappings.candidate_finder.DisambiguateClassAnchorsFinder;
import mappings.candidate_finder.TranslationMatrixCandidateFinder;
import mappings.candidate_finder.TwoDocumentsCandidateFinder;
import mappings.trainer.OntologyProjector;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.TestRunUtils;
import mappings.walks_generator.Walks;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public class testRuns {

	public static PrintWriter out;
	public static String testResultsDir = "/home/ole/master/test_onto/log/";
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
	public static String nameSpaceString2 = TestRunUtils.nameSpaceString2;
	public static String mergedOwlPath = TestRunUtils.mergedOwlPath;
	public static String walksFile = TestRunUtils.walksFile;
	public static String labelsFile = TestRunUtils.labelsFile;
	public static int numWalks = TestRunUtils.numWalks;
	public static int walkDepth = TestRunUtils.walkDepth;
	public static int numThreads = TestRunUtils.numThreads;
	public static int offset = TestRunUtils.offset;
	public static int classLimit = TestRunUtils.classLimit;

	public static File createLogFile(String finderType) throws Exception {
		String fileDir = testResultsDir;
		String fileName = walksType + "_" + finderType + "_" + nameSpaceString + "_" + nameSpaceString2 + ".txt";
		String fullPath = fileDir + fileName;
		File outputFile = new File(fullPath);
		return outputFile;
	}

	public static void main(String[] args) throws Exception {

		int numberOfRuns = 1;
//		String[] anchorCandidateTypes = new String[] { "allrelationsanchorcandidatefinder",
//				"bestcandidatefinder", "disambiguateclassanchorsfinder", twodocuments" };

		String type = "bestcandidatefinder";
//		String type = "twodocuments";
//		String type = "disambiguateclassanchorsfinder";
//		String type = "allrelationsanchorcandidatefinder";
//		String type = "translationmatrixcandidatefinder";

		String[] candidateTypes = new String[] { "translationmatrixcandidatefinder",
				"pretrainedvectorcandidatesfinder" };

		File outputFile = createLogFile(type);
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
	
	public static void deleteRepository(String path) throws Exception {
		String repo = path;
		Path repoPath = Paths.get(repo);
		int numFiles = 0;
		if (Files.exists(repoPath)) {
			for (File f : Files.walk(repoPath).map(Path::toFile).collect(Collectors.toList())) {
				numFiles++;
			}

			System.out.println("num files before: " + numFiles);
//			Files.walk(repoPath).map(Path::toString).forEach(System.out::println);
			Files.walk(repoPath).map(Path::toFile).forEach(File::delete);

			numFiles = 0;
			if (Files.exists(repoPath)) {
				for (File f : Files.walk(repoPath).map(Path::toFile).collect(Collectors.toList())) {
					numFiles++;
				}
			}

			System.out.println("num files after: " + numFiles);
//			System.exit(0);
		}
	}

	public static void runTest(String finderType, int runNo) throws Exception {

		/**
		 * for RDF2Vec - must remove content in repo before each run
		 */
		if (walksType.toLowerCase().equals("rdf2vec")) {
			deleteRepository("/home/ole/master/test_onto/test_repo/");
			deleteRepository("/home/ole/master/test_repo/");
		}
		
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
		Collections.shuffle(mappings);

		for (int i = 0; i < (mappings.size() * fractionOfMappings); i++) {
			MappingObjectStr mapping = mappings.get(i);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}

		finder.addAnchorsToOntology(mergedOnto);
		OntologyReader.writeOntology(mergedOnto, mergedOwlPath, "owl");
		OntologyProjector projector = new OntologyProjector(mergedOwlPath);
		projector.projectOntology();
		projector.saveModel(modelPath);
		Walks walks = new Walks(modelPath, walksType, walksFile, numWalks, walkDepth, numThreads, offset,
				classLimit);
		
		walks.generateWalks();
		String walksFile = walks.getOutputFile();

		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer(walksFile, currentDir + "/temp/out.txt");
		// trainer.train();

		/**
		 * python gensim trainer
		 */
		TestRunUtils.trainEmbeddings(TestRunUtils.embeddingsSystem);

		trainer.loadGensimModel("/home/ole/master/test_onto/model.bin");
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
				TestRunUtils.modelPath, equalityThreshold, TestRunUtils.labelEqualityThreshold);

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

		Walks walks = new Walks(TestRunUtils.modelPath, TestRunUtils.walksType, TestRunUtils.walksFile,
				TestRunUtils.labelsFile, TestRunUtils.numWalks, TestRunUtils.walkDepth, TestRunUtils.numThreads,
				TestRunUtils.offset, TestRunUtils.classLimit);
		walks.generateWalks();
		String walksFile = walks.getOutputFile();
		String labelOutputFile = walks.getLabelOutputFile();

		/**
		 * python gensim trainer
		 */
		TestRunUtils.trainEmbeddings(TestRunUtils.embeddingsSystem);
		TestRunUtils.trainEmbeddings("twodocumentlabels");

		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer(walksFile, TestRunUtils.modelPath);
		trainer.loadGensimModel("/home/ole/master/test_onto/model.bin");
		finder.setTrainer(trainer);

		WordEmbeddingsTrainer labelTrainer = new WordEmbeddingsTrainer(labelOutputFile, TestRunUtils.labelsFile);
		labelTrainer.loadGensimModel("/home/ole/master/test_onto/label.bin");
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
		evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile,
				finder.getOutputAlignment().returnAlignmentFile().getFile(), finder.getOnto1(), finder.getOnto2());
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
		Collections.shuffle(mappings);
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
//		trainer.train();
		/**
		 * python gensim trainer
		 */
		TestRunUtils.trainEmbeddings(TestRunUtils.embeddingsSystem);

		trainer.loadGensimModel("/home/ole/master/test_onto/model.bin");
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
