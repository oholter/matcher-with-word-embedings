package mappings.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.OntologyReader;
import mappings.candidate_finder.AnchorsCandidateFinder;
import mappings.candidate_finder.BestAnchorsCandidateFinder;
import mappings.trainer.OntologyProjector;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.TestRunUtils;
import mappings.walks_generator.SecondOrderWalksGenerator;
import uk.ac.ox.krr.logmap2.LogMap2_Matcher;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public class LogMapEvaluator {
	private static Logger log = LoggerFactory.getLogger(LogMapEvaluator.class);
	private static String firstOntologyFile = TestRunUtils.firstOntologyFile;
	private static String secondOntologyFile = TestRunUtils.secondOntologyFile;
	private static String referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
	private static String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
	private static double equalityThreshold = TestRunUtils.equalityThreshold;
	private static double fractionOfMappings = TestRunUtils.fractionOfMappings;
	private static String walksType = TestRunUtils.walksType;
	private static String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath())
			.toString();
	private static LogMap2_Matcher logMapMatcher;

	public static void generateWalks() throws Exception {
		long startTime = System.nanoTime();

		OntologyReader reader = new OntologyReader();
		reader.setFname(firstOntologyFile);
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname(secondOntologyFile);
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		// For training of ontology start:
		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });
		AnchorsCandidateFinder finder = new BestAnchorsCandidateFinder(onto1, onto2, mergedOnto,
				currentDir + "/temp/out.txt", equalityThreshold);

		/* Adding anchors, by using word embeddings or by manually adding them */
//		finder.findAnchors(); /* this will use word embeddings to find anchors */

		logMapMatcher = new LogMap2_Matcher("file:" + firstOntologyFile, "file:" + secondOntologyFile,
				"/home/ole/master/test_onto/logmap_out/", true);
		Set<MappingObjectStr> anchors = logMapMatcher.getLogmap2_anchors();

		finder.addAllAnchors(anchors);
		finder.addAnchorsToOntology(mergedOnto);
		OntologyReader.writeOntology(mergedOnto, TestRunUtils.mergedOwlPath, "owl");

		System.out.println("starting projection");

		OntologyProjector projector = new OntologyProjector(TestRunUtils.mergedOwlPath);
		projector.projectOntology();
		projector.saveModel(TestRunUtils.modelPath);
		log.info("Saved model at: " + TestRunUtils.modelPath);
		System.out.println("starting walksgenerator");

//		SecondOrderWalksGenerator(String inputFile, String outputFile, int numberOfThreads, int walkDepth,
//		int limit, int numberOfWalks, int offset, int p, int q)

		SecondOrderWalksGenerator walks = new SecondOrderWalksGenerator(TestRunUtils.modelPath,
				"/home/ole/master/test_onto/walks_out.txt", 12, 40, 100000, 50, 0, 0.2, 5, "fulliri", false);
		walks.generateWalks();

		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1000000;
		System.out.println("duration: " + duration);
	}

	public static void trainEmbeddings() {
		// String command = "top -o %CPU";
		String[] command = { "/home/ole/anaconda3/bin/python",
				"/home/ole/workspace/MatcherWithWordEmbeddings/py/learn/learn_document.py" };
		try {
			Process pr = new ProcessBuilder().command(command).inheritIO().start();
			pr.waitFor();
//			System.out.println(pr.exitValue());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void evaluateEmbeddings() throws Exception {
		if (logMapMatcher == null) {
			logMapMatcher = new LogMap2_Matcher("file:" + firstOntologyFile, "file:" + secondOntologyFile,
					"/home/ole/master/test_onto/logmap_out/", true);
		}

		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer("", currentDir + "/temp/out.txt");
		trainer.loadGensimModel("/home/ole/master/test_onto/model.bin");

		Set<MappingObjectStr> discardedMappings = logMapMatcher.getLogmap2_HardDiscardedMappings();
		discardedMappings.addAll(logMapMatcher.getLogmap2_ConflictiveMappings());
		for (MappingObjectStr mapping : discardedMappings) {
			System.out.print(mapping + " logmap conf: " + mapping.getConfidence());
			System.out.println(
					" Embdding conf: " + trainer.getCosine(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2()));
		}
	}

	public static void main(String[] args) throws Exception {
//		generateWalks();
//		trainEmbeddings();
		evaluateEmbeddings();
	}
}
