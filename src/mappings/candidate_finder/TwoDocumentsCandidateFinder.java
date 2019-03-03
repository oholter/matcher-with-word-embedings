package mappings.candidate_finder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edit_distance.EditDistance;
import io.AlignmentsReader;
import io.OAEIAlignmentsReader;
import io.OntologyReader;
import mapping.object.MappingObjectStr;
import mappings.evaluation.ClassMappingsEvaluator;
import mappings.evaluation.MappingsEvaluator;
import mappings.trainer.OntologyProjector;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.AlignmentUtilities;
import mappings.utils.TestRunUtils;
import mappings.utils.VectorUtils;
import mappings.walks_generator.Walks;

public class TwoDocumentsCandidateFinder extends DisambiguateClassAnchorsFinder {

	public WordEmbeddingsTrainer labelTrainer;
	private double labelEqualityThreshold;

	public TwoDocumentsCandidateFinder(OWLOntology o1, OWLOntology o2, OWLOntology mergedOnto, String modelPath,
			double distLimit, double labelEqualityThreshold) throws Exception {
		super(o1, o2, mergedOnto, modelPath, distLimit);
		this.labelEqualityThreshold = labelEqualityThreshold;
	}

	/**
	 * Using a combination of string similarity and vector similarity to improve
	 * confidence
	 **/
	public OWLClass disambiguateClassCandidates(OWLClass classToMatch, ArrayList<OWLClass> candidates) {
		String iriFromClassToMatch = normalizeIRI(classToMatch.getIRI().getFragment());
		String labelFromClassToMatch = normalizeString(findAnnotation(classToMatch, onto1, "label"));
		String commentFromClassToMatch = normalizeString(findAnnotation(classToMatch, onto1, "comment"));
		double bestMatchScore = 0;
		OWLClass currentBestCandidate = null;

		for (OWLClass candidate : candidates) {
			double cosineSimilarity = VectorUtils.cosineSimilarity(
					trainer.getWordVector(classToMatch.getIRI().toString()),
					trainer.getWordVector(candidate.getIRI().toString()));
			if (Double.isNaN(cosineSimilarity)) {
				cosineSimilarity = 0;
			}

			String iriFromCandidate = normalizeIRI(candidate.getIRI().getFragment());
			String labelFromCandidate = normalizeString(findAnnotation(candidate, onto2, "label"));
			String commentFromCandidate = normalizeString(findAnnotation(candidate, onto2, "comment"));

			double matchScore;
			double bestED;
			double edMatch;
			double iriSimilarity_string = 0;
			double labelSimilarity_string = 0;
			double commentSimilarity_string = 0;
			
			if (iriFromClassToMatch != null && iriFromCandidate != null) {
			iriSimilarity_string = labelTrainer.getAvgVectorCosine(iriFromClassToMatch.split(" "),
					iriFromCandidate.split(" "));
			}
			if (labelFromClassToMatch != null && labelFromCandidate != null) {
			labelSimilarity_string = labelTrainer.getAvgVectorCosine(labelFromClassToMatch.split(" "),
					labelFromCandidate.split(" "));
			}
			if (commentFromClassToMatch != null && commentFromCandidate != null)
			commentSimilarity_string = labelTrainer.getAvgVectorCosine(commentFromClassToMatch.split(" "),
					commentFromCandidate.split(" "));

			double bestMatch = Math.max(iriSimilarity_string,
					Math.max(labelSimilarity_string, commentSimilarity_string));

			if (bestMatch > bestMatchScore) {
				bestMatchScore = bestMatch;
				currentBestCandidate = candidate;
			}
//			System.out.println("This is two documents");
			System.out.println("Disambiguating: " + classToMatch + " current: " + candidate + " score: " + bestMatch);
		}
		if (bestMatchScore > labelEqualityThreshold) {
			return currentBestCandidate;
		}
		return null;
	}

	public void setLabelTrainer(WordEmbeddingsTrainer labelTrainer) {
		this.labelTrainer = labelTrainer;
	}

	public static void main(String[] args) throws Exception {
		String firstOntologyFile = TestRunUtils.firstOntologyFile;
		String secondOntologyFile = TestRunUtils.secondOntologyFile;
		String referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
		String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
		double equalityThreshold = TestRunUtils.equalityThreshold;
		double fractionOfMappings = TestRunUtils.fractionOfMappings;
		String walksType = "TwoDocuments";

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

		// For training of ontology start:
		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });
		TwoDocumentsCandidateFinder finder = new TwoDocumentsCandidateFinder(onto1, onto2, mergedOnto,
				currentDir + "/temp/out.txt", equalityThreshold, TestRunUtils.labelEqualityThreshold);

		/* Adding anchors, by using word embeddings or by manually adding them */
//		finder.findAnchors(); /* this will use word embeddings to find anchors */

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
		OntologyReader.writeOntology(mergedOnto, TestRunUtils.mergedOwlPath, "owl");

		OntologyProjector projector = new OntologyProjector(TestRunUtils.mergedOwlPath);
		projector.projectOntology();
		projector.saveModel("/home/ole/master/test_onto/merged.ttl");

		Walks walks = new Walks("/home/ole/master/test_onto/merged.ttl", walksType);
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
		evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile, finder.output.returnAlignmentFile().getFile(),
				finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");
	}
}
