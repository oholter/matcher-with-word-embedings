package mappings.evaluation;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.AlignmentsReader;
import io.OAEIAlignmentsReader;
import io.OntologyReader;
import mappings.candidate_finder.AnchorsCandidateFinder;
import mappings.candidate_finder.BestAnchorsCandidateFinder;
import mappings.trainer.OntologyProjector;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.StatUtils;
import mappings.utils.StringUtils;
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
	private static PrintWriter out;
	private static HashSet<String> anchorSet;

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
				"/home/ole/master/test_onto/walks_out.txt", 12, 8, 1000000, 10, 0, TestRunUtils.p, TestRunUtils.q, TestRunUtils.whatToEmbed,
				TestRunUtils.includeIndividuals, TestRunUtils.includeEdges);
		walks.generateWalks();

		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1000000;
		printBoth("time to generate walks: " + duration + "ms");
	}

	public static void evaluateEmbeddings() throws Exception {
		if (logMapMatcher == null) {
			logMapMatcher = new LogMap2_Matcher("file:" + firstOntologyFile, "file:" + secondOntologyFile,
					"/home/ole/master/test_onto/logmap_out/", true);
		}

		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer("", currentDir + "/temp/out.txt");
		trainer.loadGensimModel("/home/ole/master/test_onto/model.bin");

		Set<MappingObjectStr> anchors = logMapMatcher.getLogmap2_anchors();
		System.out.println("Using the anchors: ");
		anchors.forEach(System.out::println);

		Set<MappingObjectStr> hardDiscardeMappings = logMapMatcher.getLogmap2_HardDiscardedMappings();
		printResults(hardDiscardeMappings, trainer, "Hard discardet mappings");

		Set<MappingObjectStr> discardetMappings = logMapMatcher.getLogmap2_DiscardedMappings();
		printResults(discardetMappings, trainer, "discarded mappings");

		Set<MappingObjectStr> conflictiveMappings = logMapMatcher.getLogmap2_ConflictiveMappings();
		printResults(conflictiveMappings, trainer, "conflictive mappings");

		printResults(anchors, trainer, "Anchors");

		improveAlignment(trainer);

	}

	public static void printBoth(String string) {
		System.out.println(string);
		out.println(string);

	}

	public static void printResults(Set<MappingObjectStr> mappings, WordEmbeddingsTrainer trainer, String descr)
			throws Exception {
		printBoth("\n" + descr + ":");
		printBoth("< ------ Mapping -------> | sconf |   lconf   ||    emb. cosine");
		double totStrConf = 0;
		double totCosine = 0;
		int numMappings = 0;

		for (MappingObjectStr mapping : mappings) {
			double sconf = mapping.getStructuralConfidenceMapping();
			double lconf = mapping.getLexicalConfidenceMapping();
			double cos = 0;
			if (TestRunUtils.whatToEmbed.toLowerCase().equals("fulluri")) {
				cos = trainer.getCosine(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
			} else if (TestRunUtils.whatToEmbed.toLowerCase().equals("uripart")) {
				cos = trainer.getCosine(StringUtils.normalizeFullIRINoSpace(mapping.getIRIStrEnt1()),
						StringUtils.normalizeFullIRINoSpace(mapping.getIRIStrEnt2()));
			} // todo add more
			if (!Double.isNaN(cos)) { // properties will return NaN
				printBoth(mapping + " ~ " + sconf + " |" + lconf + " || " + cos);
				numMappings++;
				totStrConf += sconf;
				totCosine += cos;
			}
		}

		/**
		 * not possible to compare
		 */

//		if (numMappings > 0) {
//			double diff = (totCosine - totStrConf);
//			double avgDiff = diff / (double) numMappings;
//			printBoth("Avg increase: " + avgDiff);
//		}
	}

	public static void createAnchorSet() {
		anchorSet = new HashSet<>();
		Set<MappingObjectStr> logmapAnchors = logMapMatcher.getLogmap2_anchors();

		Set<String> left = logmapAnchors.stream().map(MappingObjectStr::getIRIStrEnt1).collect(Collectors.toSet());

		Set<String> right = logmapAnchors.stream().map(MappingObjectStr::getIRIStrEnt2).collect(Collectors.toSet());

		anchorSet.addAll(left);
		anchorSet.addAll(right);
	}

	public static Set<MappingObjectStr> findWrongDiscardedMappings(Set<MappingObjectStr> discarded, WordEmbeddingsTrainer trainer) throws Exception {
//		Set<Double> allCosines = discarded.stream().map(m -> trainer.getCosine(m.getIRIStrEnt1(), m.getIRIStrEnt1()))
//				.collect(Collectors.toSet());
		Set<Double> allCosines = discarded.stream()
				.map(m -> (trainer.getCosine(m.getIRIStrEnt1(), m.getIRIStrEnt2())))
				.collect(Collectors.toSet());
//		System.out.println("\nall cosines:");
//		allCosines.forEach(System.out::println);
		double mean = StatUtils.getMean(allCosines);
		double stdDev = StatUtils.getStandardDeviation(allCosines);
//		double cutoff = 0;
//		double cutoff = mean;
//		double cutoff = mean + stdDev;
//		double cutoff = mean + (2 * stdDev);
//		double cutoff = StatUtils.getIQROutLayersCutoff(allCosines, true);
		double cutoff = StatUtils.getTopnCutoff(allCosines, 20);

		// remove missing values for the large bio track
		allCosines = allCosines.stream().filter(n -> ! Double.isNaN(n)).collect(Collectors.toSet());
		
		Set<MappingObjectStr> possible = discarded.stream()
				.filter(m -> trainer.getCosine(m.getIRIStrEnt1(), m.getIRIStrEnt2()) > cutoff)
				.collect(Collectors.toSet());

		// must filter out all of the mappings where on uris are not in the anchor set
		// first need a way to determine contains fast ... hashSet
		if (anchorSet == null) {
			createAnchorSet();
		}
		

		
		// Can discard if already exist in anchor. - not useful for largebio 
//		Set<MappingObjectStr> notInAnchors = possible.parallelStream()
//				.filter(m -> !anchorSet.contains(m.getIRIStrEnt1()) && !anchorSet.contains(m.getIRIStrEnt2()))
//				.collect(Collectors.toSet());

		System.out.println("mean: " + mean + ", stdDev: " + stdDev + ", " + "cutoff: " + cutoff);
		
		return possible;
	}

	public static Set<MappingObjectStr> findDubiousAnchors(WordEmbeddingsTrainer trainer) throws Exception {
		Set<MappingObjectStr> lmapAnchors = logMapMatcher.getLogmap2_anchors();
		Set<Double> allCosines = lmapAnchors.stream()
				.map(m -> (trainer.getCosine(m.getIRIStrEnt1(), m.getIRIStrEnt2())))
				.collect(Collectors.toSet());
//		System.out.println("\nAll cosines:");
//		allCosines.forEach(System.out::println);
		double mean = StatUtils.getMean(allCosines);
		double stdDev = StatUtils.getStandardDeviation(allCosines);
//		double cutoff = 1;
//		double cutoff = mean;
//		double cutoff = (mean - stdDev);
//		double cutoff = mean - (2 * stdDev);
//		double cutoff = StatUtils.getIQROutLayersCutoff(allCosines, false);
		double cutoff = StatUtils.getBotnCutoff(allCosines, 20);

		
		// remove missing vocabulary for the largebio track
		allCosines = allCosines.stream().filter(n -> ! Double.isNaN(n)).collect(Collectors.toSet());

		Set<MappingObjectStr> dubious = lmapAnchors.parallelStream()
				.filter(m -> trainer.getCosine(m.getIRIStrEnt1(), m.getIRIStrEnt2()) < cutoff)
				.collect(Collectors.toSet());
		printResults(dubious, trainer, "Dubious anchors");
		System.out.println("mean: " + mean + ", stdDev: " + stdDev + ", " + "cutoff: " + cutoff);
		
		return dubious;
	}
	
	public static void improveAlignment(WordEmbeddingsTrainer trainer) throws Exception {
		Set<MappingObjectStr> dAnchors = findDubiousAnchors(trainer);
		Set<MappingObjectStr> whard = findWrongDiscardedMappings(logMapMatcher.getLogmap2_HardDiscardedMappings(), trainer);
		Set<MappingObjectStr> wconflict = findWrongDiscardedMappings(logMapMatcher.getLogmap2_ConflictiveMappings(), trainer);
		Set<MappingObjectStr> wdiscarded = findWrongDiscardedMappings(logMapMatcher.getLogmap2_DiscardedMappings(), trainer);
		
		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(TestRunUtils.referenceAlignmentsFile);
		alignmentsReader.readMappings();
		
		Set<MappingObjectStr> gs = alignmentsReader.getMappingsAsSet();
		
		double anchorsSize = dAnchors.size();
		dAnchors = dAnchors.stream().filter(m -> !gs.contains(m)).collect(Collectors.toSet());
		printBoth("\nFiltering potentially bad anchors and discarded mappings");
		printBoth("Anchors --- Before: " + anchorsSize + " after: " + dAnchors.size());
		
		double whardSize = whard.size();
		whard = whard.stream().filter(m -> gs.contains(m)).collect(Collectors.toSet());
		printBoth("Hard --- Before: " + whardSize + " after: " + whard.size());
		
		double wconflictSize = wconflict.size();
		wconflict = wconflict.stream().filter(m -> gs.contains(m)).collect(Collectors.toSet());
		printBoth("Conflict --- Before: " + wconflictSize + " after: " + wconflict.size());

		double wdiscardedSize = wdiscarded.size();
		wdiscarded = wdiscarded.stream().filter(m -> gs.contains(m)).collect(Collectors.toSet());
		printBoth("Discarded --- Before: " + wdiscardedSize + " after: " + wdiscarded.size());
	}

	public static void main(String[] args) throws Exception {
		out = new PrintWriter(new File(TestRunUtils.logFile));
		
//		generateWalks();
//		long startTime = System.nanoTime();
//		TestRunUtils.trainEmbeddings(TestRunUtils.embeddingsSystem);
//		long endTime = System.nanoTime();
//		long duration = (endTime - startTime) / 1000000;
//		printBoth("time to train: " + duration + "ms");

		evaluateEmbeddings();

		out.close();
	}
}
