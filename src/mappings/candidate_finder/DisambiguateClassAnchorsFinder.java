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
import mapping.object.MappingObjectStr;
import mappings.evaluation.ClassMappingsEvaluator;
import mappings.evaluation.MappingsEvaluator;
import mappings.trainer.OntologyProjector;
import mappings.trainer.OntologyReader;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.AlignmentUtilities;
import mappings.utils.TestRunUtils;
import mappings.utils.VectorUtils;

public class DisambiguateClassAnchorsFinder extends AnchorsCandidateFinder {
	public DisambiguateClassAnchorsFinder(OWLOntology o1, OWLOntology o2, OWLOntology mergedOnto, String modelPath,
			double distLimit) throws Exception {
		super(o1, o2, mergedOnto, modelPath, distLimit);
	}
	
	/** This generates all class candidates then diambiguate **/
	public void generateClassCandidates() {
		int numCandidates = 0;

		for (OWLClass classFromFirstOntology : onto1.getClassesInSignature()) {
			ArrayList<OWLClass> candidates = new ArrayList<>();
			String iriFromFirstOntology = classFromFirstOntology.getIRI().toString();

			for (OWLClass classFromSecondOntology : onto2.getClassesInSignature()) {
				String iriFromSecondOntology = classFromSecondOntology.getIRI().toString();

				double iriCosine = 0;
				iriCosine = VectorUtils.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(iriFromSecondOntology));
				if (Double.isNaN(iriCosine)) {
					iriCosine = 0;
				}

				double currentSimilarity = iriCosine; //

				if (currentSimilarity > distLimit) { // candidate
					candidates.add(classFromSecondOntology);
				}

			} // end classFromSecondOntology

			if (!candidates.isEmpty()) {
				OWLClass candidate = disambiguateClassCandidates(classFromFirstOntology, candidates);
				if (candidate == null) {
					continue;
				}

				double similarity = 0;
				similarity = VectorUtils.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(candidate.getIRI().toString()));
				if (Double.isNaN(similarity)) {
					similarity = 0;
				}

				try {
					output.addClassMapping2Output(iriFromFirstOntology, candidate.getIRI().toString(), AlignmentUtilities.EQ,
							similarity);
				} catch (Exception e) {
					e.printStackTrace();
				}

				OWLEquivalentClassesAxiom equivalentClassAxiom = mappingsFactory
						.getOWLEquivalentClassesAxiom(classFromFirstOntology, candidate);
				mappingsManager.addAxiom(mappings, equivalentClassAxiom);

				OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(similarity);
				OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
						confidenceLiteral);
				OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
						.getOWLAnnotationAssertionAxiom(classFromFirstOntology.getIRI(), annotation);

				mappingsManager.addAxiom(mappings, annotationAssertionAxiom);

				System.out.println("Found mapping: " + equivalentClassAxiom + " distance: " + similarity);
				numCandidates++;
			}

		} // finished classFromFirstOntology

		System.out.println("Found " + numCandidates + " class candidates:");
	} // finished generateAllClassCandidates()
	
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
			double cosineSimilarity = VectorUtils.cosineSimilarity(trainer.getWordVector(classToMatch.getIRI().toString()),
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

			int iriED = EditDistance.editDistance(iriFromClassToMatch, iriFromCandidate, iriFromClassToMatch.length(),
					iriFromCandidate.length());

			if (labelFromCandidate != null && labelFromClassToMatch != null) {
				int labelED = EditDistance.editDistance(labelFromClassToMatch, labelFromCandidate,
						labelFromClassToMatch.length(), labelFromCandidate.length());
				bestED = (double) Math.min(iriED, labelED);
				edMatch = 1 - (bestED / 10);
			} else {
				bestED = (double) iriED;
				edMatch = 1 - (bestED / 10);
			}

			matchScore = (edMatch + cosineSimilarity) / 2;
			if (matchScore > bestMatchScore) {
				bestMatchScore = matchScore;
				currentBestCandidate = candidate;
			}
//			System.out.println("Disambiguating: " + classToMatch + " current: " + candidate + " score: " + matchScore);
		}
		return currentBestCandidate;
	}
	
	public static void main(String[] args) throws Exception {
		String firstOntologyFile = TestRunUtils.firstOntologyFile;
		String secondOntologyFile = TestRunUtils.secondOntologyFile;
		String referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
		String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
		double equalityThreshold = TestRunUtils.equalityThreshold;
		double fractionOfMappings = TestRunUtils.fractionOfMappings;
		String walksType = TestRunUtils.walksType;


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
		AnchorsCandidateFinder finder = new DisambiguateClassAnchorsFinder(onto1, onto2, mergedOnto,
				currentDir + "/temp/out.txt", equalityThreshold);

		/* Adding anchors, by using word embeddings or by manually adding them */
//		finder.findAnchors(); /* this will use word embeddings to find anchors */

		/* Adding anchors by reading an alignments file */
		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, onto1, onto2);

		List<MappingObjectStr> mappings = alignmentsReader.getMappings();
		for (int i = 0; i < (mappings.size() * fractionOfMappings); i++) {
			MappingObjectStr mapping = mappings.get(i);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}

		finder.addAnchorsToOntology(mergedOnto);
		OntologyReader.writeOntology(mergedOnto, "file:/home/ole/master/test_onto/merged.owl", "owl");

		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/merged.owl");
		projector.projectOntology();
		projector.saveModel("/home/ole/master/test_onto/merged.ttl");

		Walks walks = new Walks("/home/ole/master/test_onto/merged.ttl", walksType);
//		Walks_rdf2vec walks = new Walks_rdf2vec();
//		walks.loadFromRdfFile("/home/ole/master/test_onto/merged.ttl");
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
		evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile, finder.output.returnAlignmentFile().getFile(),
				finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");
	}
	
}
