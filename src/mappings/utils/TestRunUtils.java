package mappings.utils;

public class TestRunUtils {
//	public static String firstOntologyFile = "/home/ole/master/test_onto/ekaw.owl";
//	public static String secondOntologyFile = "/home/ole/master/test_onto/ekaw2.owl";
//	public static String nameSpaceString1 = "ekaw";
//	public static String nameSpaceString2 = "ekaw2";
//	public static String baseUriString1 = "http://ekaw";
//	public static String baseUriString2 = "http://ekaw2";
//	public static String referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/ekaw-ekaw2.rdf";
	
	
	// EKAW-CMT
	public static String firstOntologyFile = "/home/ole/master/test_onto/cmt.owl";
	public static String secondOntologyFile = "/home/ole/master/test_onto/ekaw.owl";
	public static String nameSpaceString1 = "cmt";
	public static String nameSpaceString2 = "ekaw";
	public static String baseUriString1 = "http://cmt";
	public static String baseUriString2 = "http://ekaw";
	public static String referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/cmt-ekaw.rdf";
	
	
	// ANATOMY
//	public static String firstOntologyFile = "/home/ole/master/test_onto/mouse.owl";
//	public static String secondOntologyFile = "/home/ole/master/test_onto/human.owl";
//	public static String nameSpaceString1 = "mouse";
//	public static String nameSpaceString2 = "human";
//	public static String baseUriString1 = "http://mouse";
//	public static String baseUriString2 = "http://human";
//	public static String referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/mouse-human.rdf";
	
//	public static String walksType = "synonymsowl2vec";
	public static String walksType = "owl2vec";
//	public static String walksType = "rdf2vec";
//	public static String walksType = "TwoDocuments";
	
	
	public static String logMapAlignmentsFile = "/home/ole/master/logmap_standalone/output/logmap2_mappings.rdf";
	public static String modelPath = "/home/ole/master/test_onto/merged.ttl";
	public static String mergedOwlPath = "file:/home/ole/master/test_onto/merged.owl";
	public static String word2vecModelPath = "/home/ole/master/word2vec/models/fil9.model";
	public static String owlOutPath = "file:/home/ole/master/test_onto/out.owl";
	public static String pretrainedModelOutputPath = owlOutPath = "file:/home/ole/master/test_onto/out.model";
	
	public static String referenceFilePath = "/home/ole/master/test_onto/ref.txt";
	public static String resultFilePath = "/home/ole/master/test_onto/res.txt";

	public static double equalityThreshold = 0.6;
	public static double fractionOfMappings = 0.6;

	public static double labelEqualityThreshold = 0.90; // for the two document 
}
