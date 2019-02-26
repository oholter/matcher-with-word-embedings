package mappings.candidate_finder;

import java.util.ArrayList;

import org.apache.log4j.BasicConfigurator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.OntologyReader;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.AlignmentUtilities;
import mappings.utils.TestRunUtils;

public class PretrainedVectorCandidateFinder extends CandidateFinder {

	public PretrainedVectorCandidateFinder(OWLOntology o1, OWLOntology o2, double distLimit, String modelPath)
			throws Exception {
		super(o1, o2, modelPath, distLimit);
		trainer = new WordEmbeddingsTrainer("/home/ole/out.csv", modelPath);
		trainer.loadModel();
	}

	public void createMappings() {
		mappingsManager = OWLManager.createOWLOntologyManager();
		mappingsFactory = mappingsManager.getOWLDataFactory();

		try {
			mappings = mappingsManager.createOntology();
			generateClassCandidates();
			generateObjectProperties();
			generateDataProperties();
			System.out.println(getMappings());
			output.saveOutputFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void addAnchor(String uri1, String uri2) {
		// do nothing, does not need anchors
	}
	
	public void addAnchorsToOntology(OWLOntology onto) {
		// do nothing
	}
	
	public void setTrainer(WordEmbeddingsTrainer trainer) {
		this.trainer = trainer;
	}

	public void generateClassCandidates() {
		int numCandidates = 0;
		ArrayList<OWLClass> usedClassesFromSecondOntology = new ArrayList<>();

		for (OWLClass classFromFirstOntology : onto1.getClassesInSignature()) {
			double maxSimilarity = 0;
			OWLClass candidate = null;
			String iriFromFirstOntology = normalizeIRI(classFromFirstOntology.getIRI().getFragment());
			String labelFromFirstOntology = normalizeString(findAnnotation(classFromFirstOntology, onto1, "label"));
			String commentFromFirstOntology = normalizeString(findAnnotation(classFromFirstOntology, onto1, "comment"));

			for (OWLClass classFromSecondOntology : onto2.getClassesInSignature()) {
				if (usedClassesFromSecondOntology.contains(classFromSecondOntology)) {
					continue; // this class is already added
				}
				String iriFromSecondOntology = normalizeIRI(classFromSecondOntology.getIRI().getFragment());
				String labelFromSecondOntology = normalizeString(
						findAnnotation(classFromSecondOntology, onto2, "label"));
				String commentFromSecondOntology = normalizeString(
						findAnnotation(classFromSecondOntology, onto2, "comment"));

				double iriCosine = trainer.getCosine(iriFromFirstOntology, iriFromSecondOntology);
				if (Double.isNaN(iriCosine)) {
					iriCosine = 0;
				}

				double labelCosine = 0;
				if (labelFromFirstOntology != null && labelFromSecondOntology != null) {
					labelCosine = trainer.getAvgVectorCosine(labelFromFirstOntology.split(" "),
							labelFromSecondOntology.split(" "));
				}
				double commentCosine = 0;
				if (commentFromFirstOntology != null && commentFromSecondOntology != null) {
					commentCosine = trainer.getAvgVectorCosine(commentFromFirstOntology.split(" "),
							commentFromSecondOntology.split(" "));
				}

				double currentSimilarity = max(iriCosine, labelCosine, commentCosine);

				if (currentSimilarity > maxSimilarity) {
					maxSimilarity = currentSimilarity;
					candidate = classFromSecondOntology;
				}
			} // end classFromSecondOntology

			if (maxSimilarity > distLimit) {
				try {
					output.addClassMapping2Output(iriFromFirstOntology, candidate.getIRI().toString(), AlignmentUtilities.EQ,
							maxSimilarity);
				} catch (Exception e) {
					e.printStackTrace();
				}				OWLEquivalentClassesAxiom equivalentClassAxiom = mappingsFactory
						.getOWLEquivalentClassesAxiom(classFromFirstOntology, candidate);
//				mappings.add(equivalentClassAxiom); owlapi5
				mappingsManager.addAxiom(mappings, equivalentClassAxiom);

				OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
				OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
						confidenceLiteral);
				OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
						.getOWLAnnotationAssertionAxiom(classFromFirstOntology.getIRI(), annotation);

//				mappings.add(annotationAssertionAxiom);
				mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
				usedClassesFromSecondOntology.add(candidate);

//				System.out.println("Found mapping: " + equivalentClassAxiom);
				numCandidates++;
			}
		} // finished classFromFirstOntology

		System.out.println("Found " + numCandidates + " class candidates:");
	} // finished generateClassCandidates()

	public void generateObjectProperties() {
		int numCandidates = 0;
		ArrayList<OWLObjectProperty> usedPropertiesFromSecondOntology = new ArrayList<>();

		for (OWLObjectProperty propertyFromFirstOntology : onto1.getObjectPropertiesInSignature()) {
			String iriFromFirstOntology = normalizeIRI(propertyFromFirstOntology.getIRI().getFragment());
			String labelFromFirstOntology = normalizeString(findAnnotation(propertyFromFirstOntology, onto1, "label"));
			String commentFromFirstOntology = normalizeString(
					findAnnotation(propertyFromFirstOntology, onto1, "comment"));
			double maxSimilarity = 0;
			OWLObjectProperty candidate = null;

			for (OWLObjectProperty propertyFromSecondOntology : onto2.getObjectPropertiesInSignature()) {
				if (usedPropertiesFromSecondOntology.contains(propertyFromSecondOntology)) {
					continue; // already used
				}
				String iriFromSecondOntology = normalizeIRI(propertyFromSecondOntology.getIRI().getFragment());
				String labelFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "label"));
				String commentFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "comment"));

				double iriCosine = trainer.getAvgVectorCosine(iriFromFirstOntology.split(" "),
						iriFromSecondOntology.split(" "));
				if (Double.isNaN(iriCosine)) {
					iriCosine = 0;
				}

				double labelCosine = 0;
				if (labelFromFirstOntology != null && labelFromSecondOntology != null) {
					labelCosine = trainer.getAvgVectorCosine(labelFromFirstOntology.split(" "),
							labelFromSecondOntology.split(" "));
				}
				double commentCosine = 0;
				if (commentFromFirstOntology != null && commentFromSecondOntology != null) {
					commentCosine = trainer.getAvgVectorCosine(commentFromFirstOntology.split(" "),
							commentFromSecondOntology.split(" "));
				}

				double currentSimilarity = max(iriCosine, labelCosine, commentCosine);
//				System.out.println(iriFromFirstOntology + " and " + iriFromSecondOntology + " got " + currentSimilarity);

				if (currentSimilarity > maxSimilarity) {
					maxSimilarity = currentSimilarity;
					candidate = propertyFromSecondOntology;
				}
			} // finished propertyFromSecondOntology
//			System.out.println("Object property max similarity: " + maxSimilarity);

			if (maxSimilarity > distLimit) {
				OWLEquivalentObjectPropertiesAxiom equivalentPropertiesAxiom = mappingsFactory
						.getOWLEquivalentObjectPropertiesAxiom(propertyFromFirstOntology, candidate);
//				mappings.add(equivalentPropertiesAxiom);
				mappingsManager.addAxiom(mappings, equivalentPropertiesAxiom);

				OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
				OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
						confidenceLiteral);
				OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
						.getOWLAnnotationAssertionAxiom(propertyFromFirstOntology.getIRI(), annotation);

//				System.out.println("Found mapping: " + equivalentPropertiesAxiom);
//				System.out.println("With a similarity of: " + maxSimilarity);
//				mappings.add(annotationAssertionAxiom);
				mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
				usedPropertiesFromSecondOntology.add(candidate);

				numCandidates++;
			}
		} // finished propertyFromFirstOntology
		System.out.println("Found " + numCandidates + " object property candidates:");
	} // finished generateObjectProperties()

	public void generateDataProperties() {
		int numCandidates = 0;
		ArrayList<OWLDataProperty> usedPropertyFromSecondOntology = new ArrayList<>();

		for (OWLDataProperty propertyFromFirstOntology : onto1.getDataPropertiesInSignature()) {
			String iriFromFirstOntology = normalizeIRI(propertyFromFirstOntology.getIRI().getFragment());
			String labelFromFirstOntology = normalizeString(findAnnotation(propertyFromFirstOntology, onto1, "label"));
			String commentFromFirstOntology = normalizeString(
					findAnnotation(propertyFromFirstOntology, onto1, "comment"));
			double maxSimilarity = 0;
			OWLDataProperty candidate = null;

			for (OWLDataProperty propertyFromSecondOntology : onto2.getDataPropertiesInSignature()) {
				if (usedPropertyFromSecondOntology.contains(propertyFromSecondOntology)) {
					continue;
				}
				String iriFromSecondOntology = normalizeIRI(propertyFromSecondOntology.getIRI().getFragment());
				String labelFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "label"));
				String commentFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "comment"));

				double iriCosine = trainer.getAvgVectorCosine(iriFromFirstOntology.split(" "),
						iriFromSecondOntology.split(" "));
				if (Double.isNaN(iriCosine)) {
					iriCosine = 0;
				}

				double labelCosine = 0;
				if (labelFromFirstOntology != null && labelFromSecondOntology != null) {
					labelCosine = trainer.getAvgVectorCosine(labelFromFirstOntology.split(" "),
							labelFromSecondOntology.split(" "));
				}
				double commentCosine = 0;
				if (commentFromFirstOntology != null && commentFromSecondOntology != null) {
					commentCosine = trainer.getAvgVectorCosine(commentFromFirstOntology.split(" "),
							commentFromSecondOntology.split(" "));
				}

				double currentSimilarity = max(iriCosine, labelCosine, commentCosine);
//				System.out.println(iriFromFirstOntology + " and " + iriFromSecondOntology + " got " + currentSimilarity);

				if (currentSimilarity > maxSimilarity) {
					maxSimilarity = currentSimilarity;
					candidate = propertyFromSecondOntology;
				}
			} // finished propertyFromSecondOntology
//			System.out.println("Data property max similarity: " + maxSimilarity);
			if (maxSimilarity > distLimit) {
				OWLEquivalentDataPropertiesAxiom equivalentPropertiesAxiom = mappingsFactory
						.getOWLEquivalentDataPropertiesAxiom(propertyFromFirstOntology, candidate);
//				mappings.add(equivalentPropertiesAxiom);
				mappingsManager.addAxiom(mappings, equivalentPropertiesAxiom);

				OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
				OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
						confidenceLiteral);
				OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
						.getOWLAnnotationAssertionAxiom(propertyFromFirstOntology.getIRI(), annotation);

//				System.out.println("Found mapping: " + equivalentPropertiesAxiom);
//				System.out.println("With a similarity of: " + maxSimilarity);
//				mappings.add(annotationAssertionAxiom);
				mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
				usedPropertyFromSecondOntology.add(candidate);

				numCandidates++;
			}
		} // finished propertyFromFirstOntology

		System.out.println("Found " + numCandidates + " data property candidates:");
	}

	public static void main(String[] args) throws Exception {
		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		String firstOntologyFile = TestRunUtils.firstOntologyFile;
		String secondOntologyFile = TestRunUtils.secondOntologyFile;
		String referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
		String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
		double equalityThreshold = TestRunUtils.equalityThreshold;
		double fractionOfMappings = TestRunUtils.fractionOfMappings;
		String walksType = TestRunUtils.walksType;
		BasicConfigurator.configure();

		OntologyReader reader = new OntologyReader();
		reader.setFname(firstOntologyFile);
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname(secondOntologyFile);
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		PretrainedVectorCandidateFinder finder = new PretrainedVectorCandidateFinder(onto1, onto2,
				equalityThreshold, "/home/ole/master/word2vec/models/fil9.model");
		finder.createMappings();
		OWLOntology o = finder.getMappings();
		System.out.println(o);
		OntologyReader.writeOntology(o, TestRunUtils.owlOutPath, "owl");
	}
}
