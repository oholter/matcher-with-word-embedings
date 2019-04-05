package mappings.utils;

public class TestRunUtils {
	
	// EKAW-EKAW
//	public static String firstOntologyFile = "/home/ole/master/test_onto/ekaw.owl";
//	public static String secondOntologyFile = "/home/ole/master/test_onto/ekaw2.owl";
//	public static String nameSpaceString1 = "ekaw";
//	public static String nameSpaceString2 = "ekaw2";
//	public static String baseUriString1 = "http://ekaw";
//	public static String baseUriString2 = "http://ekaw2";
//	public static String referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/ekaw-ekaw2.rdf";

	// EKAW-CMT
//	public static String firstOntologyFile = "/home/ole/master/test_onto/cmt.owl";
//	public static String secondOntologyFile = "/home/ole/master/test_onto/ekaw.owl";
//	public static String nameSpaceString1 = "cmt";
//	public static String nameSpaceString2 = "ekaw";
//	public static String baseUriString1 = "http://cmt";
//	public static String baseUriString2 = "http://ekaw";
//	public static String referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/cmt-ekaw.rdf";

	// PIZZA
//	public static String firstOntologyFile = "";
//	public static String secondOntologyFile = "/home/ole/master/test_onto/pizza.owl";
//	public static String nameSpaceString1 = "";
//	public static String nameSpaceString2 = "pizza.owl";
//	public static String baseUriString1 = "";
//	public static String baseUriString2 = "http://www.co-ode.org/ontologies/pizza/pizza.owl";
//	public static String referenceAlignmentsFile = "";

	// ANATOMY
//	public static String firstOntologyFile = "/home/ole/master/test_onto/mouse.owl";
//	public static String secondOntologyFile = "/home/ole/master/test_onto/human.owl";
//	public static String nameSpaceString1 = "mouse";
//	public static String nameSpaceString2 = "human";
//	public static String baseUriString1 = "http://mouse";
//	public static String baseUriString2 = "http://human";
//	public static String referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/mouse-human.rdf";

	// Largebio small
	public static String firstOntologyFile = "/home/ole/master/test_onto/oaei_FMA_small_overlapping_nci.owl";
	public static String secondOntologyFile = "/home/ole/master/test_onto/oaei_NCI_small_overlapping_fma.owl";
	public static String nameSpaceString1 = "nci";
	public static String nameSpaceString2 = "fma";
	public static String baseUriString1 = "http://nci";
	public static String baseUriString2 = "http://fma";
	public static String referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/oaei_FMA2NCI_UMLS_mappings_with_flagged_repairs.rdf";

	// Largebio complete
//	public static String firstOntologyFile = "/home/ole/master/test_onto/oaei_FMA_whole_ontology.owl";
//	public static String secondOntologyFile = "/home/ole/master/test_onto/oaei_NCI_whole_ontology.owl";
//	public static String nameSpaceString1 = "nci";
//	public static String nameSpaceString2 = "fma";
//	public static String baseUriString1 = "http://nci";
//	public static String baseUriString2 = "http://fma";
//	public static String referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/oaei_FMA2NCI_UMLS_mappings_with_flagged_repairs.rdf";
	
	
	
//	public static String walksType = "synonymsowl2vec";
//	public static String walksType = "owl2vec";
//	public static String walksType = "rdf2vec";
//	public static String walksType = "TwoDocuments";
//	public static String walksType = "subClassWalks";
	public static String walksType = "secondorder";

	public static String whatToEmbed = "fulluri";
	
	public static String embeddingsSystem = "word2vec";
//	public static String embeddingsSystem = "fasttext";

	public static String logMapAlignmentsFile = "/home/ole/master/test_onto/logmap_out/logmap2_mappings.rdf";
	public static String modelPath = "/home/ole/master/test_onto/merged.ttl";
	public static String mergedOwlPath = "file:/home/ole/master/test_onto/merged.owl";
	public static String allConferencePath = "file:/home/ole/master/test_onto/allconf.owl";
	public static String word2vecModelPath = "/home/ole/master/word2vec/models/fil9.model";
//	public static String word2vecModelPath = "/home/ole/Downloads/GoogleNews-vectors-negative300.bin";
	public static String owlOutPath = "file:/home/ole/master/test_onto/out.owl";
	public static String pretrainedModelOutputPath = "file:/home/ole/master/test_onto/out.model";
	public static String walksFile = "/home/ole/master/test_onto/walks_out.txt";
	public static String labelsFile = "/home/ole/master/test_onto/labels_out.txt";
	public static String logFile = "/home/ole/master/test_onto/log.txt";
	public static String walksModel = "/home/ole/master/test_onto/model.bin";
	public static String labelModel = "/home/ole/master/test_onto/labels.bin";

	public static String referenceFilePath = "/home/ole/master/test_onto/ref.txt";
	public static String resultFilePath = "/home/ole/master/test_onto/res.txt";

	public static String relatedConceptsPath = "/home/ole/src/thesis/evaluation/ekaw.xml";

	public static double equalityThreshold = 0.90;
	public static double fractionOfMappings = 0.6;

	public static double labelEqualityThreshold = 0.70; // for the two document

	public static double p = 1; // revisit
	public static double q = 1.0; // in/out
	public static int numWalks = 50;
	public static int walkDepth = 40;
//	public static int walkDepth = 4;
	public static int numThreads = 12;
	public static int offset = 0;
	public static int classLimit = 1000000;
	public static boolean includeIndividuals = false;
	public static boolean includeEdges = false;
	public static boolean cacheEdgeWeights = true;

	public static void trainEmbeddings(String model) {
		// String command = "top -o %CPU";
		String[] command = null;

		if (model.equals("word2vec")) {
			System.out.println("Running Word2Vec");
			command = new String[2];
			command[0] = "/home/ole/anaconda3/bin/python";
			command[1] = "/home/ole/workspace/MatcherWithWordEmbeddings/py/learn/learn_document.py";
		} else if (model.toLowerCase().equals("fasttext")) {
			System.out.println("Running fasttext");
			command = new String[2];
			command[0] = "/home/ole/anaconda3/bin/python";
			command[1] = "/home/ole/workspace/MatcherWithWordEmbeddings/py/learn/fasttext_learn_document.py";
		} else if (model.toLowerCase().equals("starspace")) {
			System.out.println("Running starspace");
			String cmdString = "/home/ole/master/StarSpace/starspace " + "train "
					+ "-trainFile /home/ole/master/test_onto/walks_out.txt "
					+ "-model /home/ole/master/test_onto/cache/starspace.model " + "-dim 50 " + "-loss hinge "
					+ "-thread 10 " + "-similarity cosine " // only used for hinge
					+ "-minCount 1 " + "-ngrams 2 " + "-trainMode 5 " + "-epoch 1 " + "-maxNegSamples 25 " + "-lr 0.01" // learning
																														// rate
					+ "-ws 5"; // windows

			command = cmdString.split(" ");
		}

		try {
			System.out.print("Running command: ");
			Process pr = new ProcessBuilder().command(command).inheritIO().start();
			pr.waitFor();
//		System.out.println(pr.exitValue());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public static void main(String[] args) {
//		trainEmbeddings(embeddingsSystem);
	}
}
