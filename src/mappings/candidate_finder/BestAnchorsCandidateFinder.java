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

public class BestAnchorsCandidateFinder extends AnchorsCandidateFinder {
	public BestAnchorsCandidateFinder(OWLOntology o1, OWLOntology o2, OWLOntology mergedOnto, String modelPath,
			double distLimit) throws Exception {
		super(o1, o2, mergedOnto, modelPath, distLimit);
	}
	
	/** This generates the best class candidate for each class **/
	public void generateClassCandidates() {
		int numCandidates = 0;
		ArrayList<OWLClass> usedClassesFromSecondOntology = new ArrayList<>();

		for (OWLClass classFromFirstOntology : onto1.getClassesInSignature()) {
			double maxSimilarity = 0;
			OWLClass candidate = null;
			String iriFromFirstOntology = classFromFirstOntology.getIRI().toString();

			for (OWLClass classFromSecondOntology : onto2.getClassesInSignature()) {
//				if (usedClassesFromSecondOntology.contains(classFromSecondOntology)) {
//					continue; // this class is already added
//				}
				String iriFromSecondOntology = classFromSecondOntology.getIRI().toString();

				double iriCosine = 0;
//					System.out.println("TESTING " + iriFromFirstOntology + " and " + iriFromSecondOntology);
				iriCosine = VectorUtils.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(iriFromSecondOntology));
//					System.out.println("TESTING " + iriFromFirstOntology + " and " + iriFromSecondOntology
//							+ " gives a similarity of: " + iriCosine);
				if (Double.isNaN(iriCosine)) {
					iriCosine = 0;
				}

				double currentSimilarity = iriCosine;

				if (currentSimilarity > maxSimilarity) {
					candidate = classFromSecondOntology;
					maxSimilarity = currentSimilarity;
				}

			} // end classFromSecondOntology
			if (maxSimilarity > distLimit) { // candidate
				OWLEquivalentClassesAxiom equivalentClassAxiom = mappingsFactory
						.getOWLEquivalentClassesAxiom(classFromFirstOntology, candidate);
				mappingsManager.addAxiom(mappings, equivalentClassAxiom);

				OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
				OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
						confidenceLiteral);
				OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
						.getOWLAnnotationAssertionAxiom(classFromFirstOntology.getIRI(), annotation);

				mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
//					usedClassesFromSecondOntology.add(classFromSecondOntology);

				System.out.println("Found mapping: " + equivalentClassAxiom + " distance: " + maxSimilarity);
				try {
					output.addClassMapping2Output(iriFromFirstOntology, candidate.getIRI().toString(), AlignmentUtilities.EQ,
							maxSimilarity);
				} catch (Exception e) {
					e.printStackTrace();
				}
				numCandidates++;
			}

		} // finished classFromFirstOntology

		System.out.println("Found " + numCandidates + " class candidates:");
	} // finished generateBestClassCandidates()
	
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
		AnchorsCandidateFinder finder = new BestAnchorsCandidateFinder(onto1, onto2, mergedOnto,
				currentDir + "/temp/out.txt", equalityThreshold);

		/* Adding anchors, by using word embeddings or by manually adding them */
//		finder.findAnchors(); /* this will use word embeddings to find anchors */

		/* Adding anchors by reading an alignments file */
		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(
				referenceAlignmentsFile, onto1, onto2);
//		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(
//				"/home/ole/master/logmap_standalone/output/logmap2_mappings.rdf", onto1, onto2);

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
		MappingsEvaluator evaluator = new ClassMappingsEvaluator(
				referenceAlignmentsFile,
				logMapAlignmentsFile, finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");
		
		System.out.println("This system:");
		evaluator = new ClassMappingsEvaluator(
				referenceAlignmentsFile,
				finder.output.returnAlignmentFile().getFile(), finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");
	}
}
