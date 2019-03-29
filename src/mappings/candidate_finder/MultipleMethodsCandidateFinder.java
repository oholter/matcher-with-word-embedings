package mappings.candidate_finder;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.BasicConfigurator;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.AlignmentsReader;
import io.OAEIAlignmentsReader;
import io.OntologyReader;
import mappings.evaluation.ClassMappingsEvaluator;
import mappings.evaluation.MappingsEvaluator;
import mappings.trainer.OntologyProjector;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.StringUtils;
import mappings.utils.TestRunUtils;
import mappings.utils.VectorUtils;
import mappings.walks_generator.Walks;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public class MultipleMethodsCandidateFinder extends AnchorsCandidateFinder {

	public WordEmbeddingsTrainer owl2vecTrainer;
	public WordEmbeddingsTrainer labelTrainer;
	public WordEmbeddingsTrainer pretrainedTrainer;
	PrintWriter resultWriter;
	PrintWriter referenceWriter;
	String resultFilePath;
	String referenceFilePath;
	List<MappingObjectStr> possibleCandidates;
	List<MappingObjectStr> finalMappings;
	double structuralSimilarityLimit = 0.5;

	public MultipleMethodsCandidateFinder(OWLOntology onto1, OWLOntology onto2, OWLOntology mergedOnto,
			String modelPath, double distLimit, String resultFilePath, String referenceFilePath) throws Exception {
//		public AnchorsCandidateFinder(OWLOntology o1, OWLOntology o2, OWLOntology mergedOnto, String modelPath,
//				double distLimit) throws Exception {
		super(onto1, onto2, mergedOnto, modelPath, distLimit);
		this.resultFilePath = resultFilePath;
		this.referenceFilePath = referenceFilePath;
		finalMappings = new ArrayList<>();
		possibleCandidates = new ArrayList<>();
		System.out.println(onto1.getOWLOntologyManager());
		System.out.println(onto2.getOWLOntologyManager());
	}

	public void createMappings() throws Exception {
		resultWriter = openWriter();
		referenceWriter = openWriter();

		generateClassCandidates();

		closeWriter(resultWriter);
		closeWriter(referenceWriter);

		for (MappingObjectStr mapping : finalMappings) {
			output.addClassMapping2Output(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2(),
					mapping.getMappingDirection(), mapping.getConfidence());
		}

		output.saveOutputFile();

	}

	public PrintWriter openWriter() {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new File(resultFilePath));
		} catch (Exception e) {
			e.printStackTrace();
		}

		writer.println("a,b,owl2vec,label,pretrained");
		return writer;
	}

	public void closeWriter(PrintWriter writer) {
		writer.close();
	}

	public void findPossibleStructuralCandidates() {
		int numCandidates = 0;

		for (OWLClass classFromFirstOntology : onto1.getClassesInSignature()) {
			String firstIri = classFromFirstOntology.getIRI().toString();

			for (OWLClass classFromSecondOntology : onto2.getClassesInSignature()) {
				String secondIri = classFromSecondOntology.getIRI().toString();

				double sim = 0;

				sim = VectorUtils.cosineSimilarity(owl2vecTrainer.getWordVector(firstIri),
						owl2vecTrainer.getWordVector(secondIri));
				if (Double.isNaN(sim)) {
					sim = 0;
				}

				if (sim > structuralSimilarityLimit) {
					possibleCandidates.add(new MappingObjectStr(firstIri, secondIri));
					numCandidates++;
//					System.out.println(firstIri + " : " + secondIri);
				}
			} // end classFromSecondOntology

		} // finished classFromFirstOntology
		System.out.println("Found " + numCandidates + " possible candidates using structural embeddings");
	}

	public void findEqualNameCandidates() {
		int numEqualNameCandidates = 0;
		for (MappingObjectStr mapping : possibleCandidates) {
			String firstIri = mapping.getIRIStrEnt1();
			String secondIri = mapping.getIRIStrEnt2();

			Set<String> firstSet = StringUtils.uri2Set(firstIri);
			Set<String> secondSet = StringUtils.uri2Set(secondIri);

			if (firstSet.equals(secondSet)) {
				mapping.setConfidenceMapping(1.0);
				finalMappings.add(mapping);
				numEqualNameCandidates++;
				System.out.println(firstIri + " : " + secondIri);
			}

		}

		// no need to further consider the mappings containing these iris
		for (MappingObjectStr mapping : finalMappings) {
			possibleCandidates.removeIf(b -> b.getIRIStrEnt1().equals(mapping.getIRIStrEnt1())
					|| b.getIRIStrEnt2().equals(mapping.getIRIStrEnt2()));
		}
		System.out.println("number of equal name candidates = " + numEqualNameCandidates);
		System.out.println("remaining possible candidates: ");
		for (MappingObjectStr candidate : possibleCandidates) {
			System.out.println(candidate.getIRIStrEnt1() + " : " + candidate.getIRIStrEnt2());
		}
	}

	public void annotateCloseNameCandidates() {
		for (MappingObjectStr candidate : possibleCandidates) {
			String firstIri = candidate.getIRIStrEnt1();
			String secondIri = candidate.getIRIStrEnt2();

			Set<String> firstSet = StringUtils.uri2Set(firstIri);
			Set<String> secondSet = StringUtils.uri2Set(secondIri);

			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			OWLDataFactory firstFactory = manager.getOWLDataFactory();
			OWLDataFactory secondFactory = manager.getOWLDataFactory();

			OWLClass firstClass = firstFactory.getOWLClass(IRI.create(firstIri));
			String firstLabelString = findAnnotation(firstClass, onto1, "label");
			if (firstLabelString != null) {
				Set<String> firstLabelSet;
				Set<String> firstLabel = StringUtils.string2Set(firstLabelString);
				firstSet.addAll(firstLabel);
			}
			String firstCommentString = findAnnotation(firstClass, onto1, "comment");
			if (firstCommentString != null) {
				Set<String> firstComment = StringUtils.string2Set(firstCommentString);
				firstSet.addAll(firstComment);
			}

			OWLClass secondClass = secondFactory.getOWLClass(IRI.create(secondIri));
			String secondLabelString = findAnnotation(secondClass, onto2, "label");
			if (secondLabelString != null) {
				Set<String> secondLabel = StringUtils.string2Set(secondLabelString);
				secondSet.addAll(secondLabel);
			}
			String secondCommentString = findAnnotation(secondClass, onto2, "comment");
			if (secondCommentString != null) {
				Set<String> secondComment = StringUtils.string2Set(secondCommentString);
				secondSet.addAll(secondComment);
			}

			double[] labelFirstAverageVector = labelTrainer.getAverageVector(firstSet);
			double[] labelSecondAverageVector = labelTrainer.getAverageVector(secondSet);
			double labelSim = VectorUtils.cosineSimilarity(labelFirstAverageVector, labelSecondAverageVector);

			double[] pretrainedFirstAverageVector = pretrainedTrainer.getAverageVector(firstSet);
			double[] pretrainedSecondAverageVector = pretrainedTrainer.getAverageVector(secondSet);
			double pretrainedSim = VectorUtils.cosineSimilarity(pretrainedFirstAverageVector,
					pretrainedSecondAverageVector);

			System.out.println(firstIri + " : " + secondIri + " got " + labelSim + " and " + pretrainedSim);

			double pretrainedWeight = 0.4;
			double labelWeight = 0.6;

			double sim = (labelWeight * labelSim + pretrainedWeight * pretrainedSim);

			if (sim > 0.70) {
				candidate.setConfidenceMapping(sim);
			}
		}
	}

	public void addBestCandidates() {
		// remove all candidates that are not annotated, all these should have conf -1
		possibleCandidates.removeIf(b -> b.getConfidence() < 0);

		// sorting possibleCandidates on confidence
		possibleCandidates = possibleCandidates.stream()
				.sorted((o1, o2) -> o1.getConfidence() > o2.getConfidence() ? -1 : 1).collect(Collectors.toList());

		System.out.println("NUMBERS should come out descending:");
		// pick the one with the highest sim for each uri
		while (possibleCandidates.size() > 0) {
			Optional<MappingObjectStr> opCandidate = possibleCandidates.stream().findFirst();
			MappingObjectStr candidate = opCandidate.get();
			finalMappings.add(candidate); // this should be the best!
			System.out.println(candidate.getConfidence());

			// remove all possible canidates with the first and second uri
			possibleCandidates.removeIf(b -> b.getIRIStrEnt1().equals(candidate.getIRIStrEnt1())
					|| b.getIRIStrEnt2().equals(candidate.getIRIStrEnt2()));
		}
	}

	public void findClusters() {
		int maxClusterSize = 15;
		double cutoff = 0.5;
		int minIntersectionSize = 1;

		WordVectors model = owl2vecTrainer.getModel();
		ArrayList<Set<String>> allSets = new ArrayList<Set<String>>();

		for (OWLClass classFromFirstOntology : onto1.getClassesInSignature()) {
			String firstIri = classFromFirstOntology.getIRI().toString();
			Collection<String> closestWords = model.wordsNearest(firstIri, maxClusterSize);
			Set<String> set = closestWords.stream().filter(iri -> model.similarity(firstIri, iri) > cutoff)
					.collect(Collectors.toSet());
			if (!set.isEmpty()) {
				allSets.add(set);
			}
		}

		for (Set<String> set : allSets) {
			System.out.println(set.toString());
		}

		boolean[] merged = new boolean[allSets.size()];

		for (int i = 0; i < allSets.size(); i++) {
			if (merged[i]) {
//				System.out.println("is merged: " + i);
				continue; // already merged skip this
			}
			Set<String> set1 = allSets.get(i);
			for (int j = i+1; j < allSets.size(); j++) {
				if (!merged[j]) {
					Set<String> set2 = allSets.get(j);
					Set<String> intersection = set2.stream().filter(e -> set1.contains(e)).collect(Collectors.toSet());
//					System.out.println(intersection.size());
					if (intersection.size() >= minIntersectionSize) {
						System.out.println("Merging: " + i + " and " + j);
						set1.addAll(set2);
						merged[j] = true;
					}
				}
			}
		}

		ArrayList<Set<String>> clusters = new ArrayList<>();
		for (int i = 0; i < allSets.size(); i++) {
			if (!merged[i]) {
				clusters.add(allSets.get(i));
			}
		}

		System.out.println("After merging the sets");
		for (Set<String> set : clusters) {
			System.out.println(set.toString());
		}
	}

	public void generateClassCandidates() {
		findClusters();
//		findPossibleStructuralCandidates();
//		findEqualNameCandidates();
//		annotateCloseNameCandidates();
//		addBestCandidates();

		System.out.println("Final candidates: ");
		for (MappingObjectStr mapping : finalMappings) {
			String firstIri = mapping.getIRIStrEnt1();
			String secondIri = mapping.getIRIStrEnt2();
			double conf = mapping.getConfidence();
			System.out.println(firstIri + " : " + secondIri + " got" + conf);
		}

	}

	public void generateDataProperties() {

	}

	public void generateObjectProperties() {

	}

	public void setOwl2VecTrainer(WordEmbeddingsTrainer trainer) {
		this.owl2vecTrainer = trainer;
	}

	public void setLabelTrainer(WordEmbeddingsTrainer trainer) {
		this.labelTrainer = trainer;
	}

	public void setPretrainedTrainer(WordEmbeddingsTrainer trainer) {
		this.pretrainedTrainer = trainer;
	}

	public static void main(String[] args) throws Exception {
		String firstOntologyFile = TestRunUtils.firstOntologyFile;
		String secondOntologyFile = TestRunUtils.secondOntologyFile;
		String referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
		String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
		double equalityThreshold = TestRunUtils.equalityThreshold;
		double fractionOfMappings = TestRunUtils.fractionOfMappings;
		String walksType = TestRunUtils.walksType;
		String resultFilePath = TestRunUtils.resultFilePath;
		String refFilePath = TestRunUtils.referenceFilePath;
		String modelPath = TestRunUtils.modelPath;
		String word2vecModelPath = TestRunUtils.word2vecModelPath;
		String mergedOwlPath = TestRunUtils.mergedOwlPath;
		String pretrainedModelOutputPath = TestRunUtils.pretrainedModelOutputPath;

		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
		BasicConfigurator.configure();

		OntologyReader reader = new OntologyReader();
		reader.setFname(firstOntologyFile);
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname(secondOntologyFile);
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });

//		public ThreeMethodsCandidateFinder(OWLOntology onto1, OWLOntology onto2, String modelPath, double distLimit,
//		String resultFilePath, String referenceFilePath)
		MultipleMethodsCandidateFinder finder = new MultipleMethodsCandidateFinder(onto1, onto2, mergedOnto, modelPath,
				equalityThreshold, resultFilePath, refFilePath);

		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, onto1, onto2);
//		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(
//				"/home/ole/master/logmap_standalone/output/logmap2_mappings.rdf", onto1, onto2);

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

		Walks walks = new Walks(modelPath, "twodocuments");
		walks.generateWalks();
		String owl2vecWalksFile = walks.getOutputFile();
		String labelWalksFile = walks.getLabelOutputFile();

		WordEmbeddingsTrainer owl2vecTrainer = new WordEmbeddingsTrainer(owl2vecWalksFile,
//				currentDir + "/temp/out.txt");
				"/home/ole/workspace/MatcherWithWordEmbeddings/py/plot/model.bin");
//		owl2vecTrainer.train();
		owl2vecTrainer.loadModel();
		finder.setOwl2VecTrainer(owl2vecTrainer);

		WordEmbeddingsTrainer labelTrainer = new WordEmbeddingsTrainer(labelWalksFile,
				currentDir + "/temp/label_out.txt");
//		labelTrainer.train();
		labelTrainer.loadModel();
		finder.setLabelTrainer(labelTrainer);

		WordEmbeddingsTrainer pretrainedTrainer = new WordEmbeddingsTrainer(word2vecModelPath, word2vecModelPath);
		pretrainedTrainer.loadModel();
		finder.setPretrainedTrainer(pretrainedTrainer);

		finder.createMappings();

		System.out.println("--------------------------------------------");
		System.out.println("The alignments file used to provide anchors: ");
		MappingsEvaluator evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile, logMapAlignmentsFile,
				finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");

		System.out.println("This system:");
		evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile, finder.output.returnAlignmentFile().getFile(),
				finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");
	}

}
