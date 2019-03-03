package mappings.candidate_finder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import mappings.trainer.TranslationMatrix;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.VectorUtils;

public abstract class AnchorsCandidateFinder extends CandidateFinder {
	final double ANCHOR_SIMILARITY = 0.95;
	final double TRAINING_RATE = 0.2;
	final String PRETRAINED_MODEL_PATH = "/home/ole/master/word2vec/models/fil9.model";

	String currentDir;
	OWLOntology mergedOnto;
	OWLOntology anchorOntology;
	TranslationMatrix superClassMatrix;

	public AnchorsCandidateFinder(OWLOntology o1, OWLOntology o2, OWLOntology mergedOnto, String modelPath,
			double distLimit) throws Exception {
		super(o1, o2, modelPath, distLimit);
		this.mergedOnto = mergedOnto;
		currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
//		trainer = new WordEmbeddingsTrainer("/home/ole/master/test_onto/merged.ttl", modelPath);
//		trainer.loadModel();
		this.modelPath = modelPath;
	}

	public abstract void generateClassCandidates();

	public void setTrainer(WordEmbeddingsTrainer trainer) {
		this.trainer = trainer;
	}

	public void findAnchors() throws Exception {

		mappings.candidate_finder.PretrainedVectorCandidateFinder anchorFinder = new mappings.candidate_finder.PretrainedVectorCandidateFinder(
				onto1, onto2, ANCHOR_SIMILARITY, PRETRAINED_MODEL_PATH);
		anchorFinder.createMappings();
		anchorOntology = anchorFinder.getMappings();

	}

	public void addAnchor(String firstIRI, String secondIRI) throws Exception {
		if (anchorOntology == null) {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			anchorOntology = man.createOntology();
		}
		OWLOntologyManager man = anchorOntology.getOWLOntologyManager();
		OWLDataFactory factory = man.getOWLDataFactory();

		IRI iri = IRI.create(firstIRI);
		boolean isClass = mergedOnto.containsClassInSignature(iri);
		boolean isObjectProperty = mergedOnto.containsObjectPropertyInSignature(iri);
		boolean isDataProperty = mergedOnto.containsDataPropertyInSignature(iri);
//		System.out.println("is class: " + isClass);
//		System.out.println("is object property: " + isObjectProperty);
//		System.out.println("is data property: " + isDataProperty);

		if (isClass) {
			OWLClass cl1 = factory.getOWLClass(IRI.create(firstIRI));
			OWLClass cl2 = factory.getOWLClass(IRI.create(secondIRI));
			OWLAxiom ax = factory.getOWLEquivalentClassesAxiom(cl1, cl2);
			man.addAxiom(anchorOntology, ax);
		} else if (isObjectProperty) {
			OWLObjectProperty p1 = factory.getOWLObjectProperty(IRI.create(firstIRI));
			OWLObjectProperty p2 = factory.getOWLObjectProperty(IRI.create(secondIRI));
			OWLAxiom ax = factory.getOWLEquivalentObjectPropertiesAxiom(p1, p2);
			man.addAxiom(anchorOntology, ax);
		} else if (isDataProperty) {
			OWLDataProperty p1 = factory.getOWLDataProperty(IRI.create(firstIRI));
			OWLDataProperty p2 = factory.getOWLDataProperty(IRI.create(secondIRI));
			OWLAxiom ax = factory.getOWLEquivalentDataPropertiesAxiom(p1, p2);
			man.addAxiom(anchorOntology, ax);
		} else {
			System.out.println("Not able to find type of axiom");
		}
	}

	public void createSuperClassMatrix() throws Exception {
		ArrayList<String> superClassIRIs = new ArrayList<>();
		ArrayList<String> subClassIRIs = new ArrayList<>();
		int numSubClasses = 0;
		int numSuperClasses = 0;

		for (OWLSubClassOfAxiom ax : mergedOnto.getAxioms(AxiomType.SUBCLASS_OF)) {
			Set<OWLClass> superClasses = ax.getSuperClass().getClassesInSignature();
			Set<OWLClass> subClasses = ax.getSubClass().getClassesInSignature();
			if (superClasses.size() == 1 && subClasses.size() == 1) {
				for (OWLClass cl : superClasses) {
					superClassIRIs.add(cl.getIRI().toString());
					System.out.println("Added superClass " + cl.getIRI().toString());
					numSuperClasses++;
				}
				for (OWLClass cl : subClasses) {
					subClassIRIs.add(cl.getIRI().toString());
					System.out.println("Added subclass " + cl.getIRI().toString());
					numSubClasses++;

				}
			}
		}

		System.out.println("EQUIVALENT AS SUBCLASSES");

		for (OWLEquivalentClassesAxiom eqAx : mergedOnto.getAxioms(AxiomType.EQUIVALENT_CLASSES)) {
			Set<OWLSubClassOfAxiom> axSet = eqAx.asOWLSubClassOfAxioms();
			for (OWLSubClassOfAxiom ax : axSet) {
				Set<OWLClass> superClasses = ax.getSuperClass().getClassesInSignature();
				Set<OWLClass> subClasses = ax.getSubClass().getClassesInSignature();
				if (superClasses.size() == 1 && subClasses.size() == 1) { // TODO - must deal with this limitation
					for (OWLClass cl : superClasses) {
						superClassIRIs.add(cl.getIRI().toString());
						System.out.println("Added superClass " + cl.getIRI().toString());
						numSuperClasses++;
					}
					for (OWLClass cl : subClasses) {
						subClassIRIs.add(cl.getIRI().toString());
						System.out.println("Added subclass " + cl.getIRI().toString());
						numSubClasses++;

					}
				}
			}
		}

		System.out.println("Added " + numSuperClasses + " superClasses and " + numSubClasses + " subClasses");
		superClassMatrix = new TranslationMatrix(trainer.getModel(), superClassIRIs.toArray(new String[0]),
				subClassIRIs.toArray(new String[0]));
	}

	public void addAnchorsToOntology(OWLOntology onto) throws Exception {
		if (anchorOntology == null) {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			anchorOntology = man.createOntology();
		}
		OWLOntologyManager mergedManager = OWLManager.createOWLOntologyManager();
		Set<OWLEquivalentClassesAxiom> equivalentClasses = anchorOntology.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		Set<OWLEquivalentObjectPropertiesAxiom> equivalentObjectProperties = anchorOntology
				.getAxioms(AxiomType.EQUIVALENT_OBJECT_PROPERTIES);
		Set<OWLEquivalentDataPropertiesAxiom> equivalentDataProperties = anchorOntology
				.getAxioms(AxiomType.EQUIVALENT_DATA_PROPERTIES);
		Set<OWLSubClassOfAxiom> subClassOf = anchorOntology.getAxioms(AxiomType.SUBCLASS_OF);

		int numAnchors = 0;

		for (OWLEquivalentClassesAxiom eqClassAxiom : equivalentClasses) {
			mergedManager.addAxiom(onto, eqClassAxiom);
//			System.out.println(eqClassAxiom);
			numAnchors++;
		}
		for (OWLEquivalentObjectPropertiesAxiom eqObjectPropertyAxiom : equivalentObjectProperties) {
			mergedManager.addAxiom(onto, eqObjectPropertyAxiom);
//			System.out.println(eqObjectPropertyAxiom);
			numAnchors++;
		}
		for (OWLEquivalentDataPropertiesAxiom eqDataPropertyAxiom : equivalentDataProperties) {
			mergedManager.addAxiom(onto, eqDataPropertyAxiom);
//			System.out.println(eqDataPropertyAxiom);
			numAnchors++;
		}
		for (OWLSubClassOfAxiom subClAxiom : subClassOf) {
			mergedManager.addAxiom(onto, subClAxiom);
			System.out.println(subClAxiom);
			numAnchors++;
		}
		System.out.println("Added " + numAnchors + " anchors");
	}

	public void createMappings() throws Exception {
		mappingsManager = OWLManager.createOWLOntologyManager();
		mappingsFactory = mappingsManager.getOWLDataFactory();

		mappings = mappingsManager.createOntology();

		try {
//			System.out.println(
//					"similarity between http://ekaw#Person and http://cmt#Person is: " + Vector.cosineSimilarity(
//							trainer.getWordVector("http://ekaw#Person"), trainer.getWordVector("http://cmt#Person")));

//			createSuperClassMatrix();
//			generateCandidatesByLookup();
			generateClassCandidates();
//			generateObjectProperties();
//			generateDataProperties();
			output.saveOutputFile();
			System.out.println("saved to: " + output.returnAlignmentFile());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void generateCandidatesByLookup() {
		System.out.println("Lookup ... ... ...");
		for (OWLClass classFromFirstOntology : onto1.getClassesInSignature()) {
			String iriFromFirstOntology = classFromFirstOntology.getIRI().toString();
			Collection<String> irisFromSecondOntology = trainer.getModel().wordsNearest(iriFromFirstOntology, 10);
			System.out.println(iriFromFirstOntology + ": ");
			for (String iriFromSecondOntology : irisFromSecondOntology) {
				if (VectorUtils.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(iriFromSecondOntology)) > distLimit) {
					System.out.println(iriFromSecondOntology);
				}
			}
		}
		System.out.println("Finished lookup ... ... ...");
	}

	/** This will generate all object properties **/
	public void generateAllObjectProperties() {
		int numCandidates = 0;

		for (OWLObjectProperty propertyFromFirstOntology : onto1.getObjectPropertiesInSignature()) {
			String iriFromFirstOntology = propertyFromFirstOntology.getIRI().toString();
			double maxSimilarity = 0;
			ArrayList<OWLObjectProperty> candidates = new ArrayList<>();

			for (OWLObjectProperty propertyFromSecondOntology : onto2.getObjectPropertiesInSignature()) {
				String iriFromSecondOntology = propertyFromSecondOntology.getIRI().toString();

				double iriCosine = 0;
				iriCosine = VectorUtils.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(iriFromSecondOntology));
				if (Double.isNaN(iriCosine)) {
					iriCosine = 0;
				}

				double currentSimilarity = iriCosine;

				if (currentSimilarity > distLimit) {
					OWLEquivalentObjectPropertiesAxiom equivalentClassAxiom = mappingsFactory
							.getOWLEquivalentObjectPropertiesAxiom(propertyFromFirstOntology,
									propertyFromSecondOntology);
					mappingsManager.addAxiom(mappings, equivalentClassAxiom);

					OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(currentSimilarity);
					OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
							confidenceLiteral);
					OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
							.getOWLAnnotationAssertionAxiom(propertyFromFirstOntology.getIRI(), annotation);

					mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
//					usedPropertiesFromSecondOntology.add(candidate.getIRI().toString());

					System.out.println("Found mapping: " + equivalentClassAxiom + " distance: " + currentSimilarity);
					numCandidates++;
				}

			} // finished propertyFromSecondOntology
//			System.out.println("Object property max similarity: " + maxSimilarity);

		} // finished propertyFromFirstOntology
		System.out.println("Found " + numCandidates + " object property candidates:");
	} // finished generateObjectProperties()

	/** This will generate the best object properties pair **/
	public void generateObjectProperties() {
		int numCandidates = 0;
		ArrayList<String> usedPropertiesFromSecondOntology = new ArrayList<>();

		for (OWLObjectProperty propertyFromFirstOntology : onto1.getObjectPropertiesInSignature()) {
			String iriFromFirstOntology = propertyFromFirstOntology.getIRI().toString();
			OWLObjectProperty candidate = null;
			double maxSimilarity = 0;

			for (OWLObjectProperty propertyFromSecondOntology : onto2.getObjectPropertiesInSignature()) {
//				if (usedPropertiesFromSecondOntology.contains(propertyFromSecondOntology.getIRI().toString())) {
//					continue; // already used
//				}
				String iriFromSecondOntology = propertyFromSecondOntology.getIRI().toString();

				double iriCosine = 0;
//				System.out.println("testing: " + iriFromFirstOntology + " and " + iriFromSecondOntology);
				iriCosine = VectorUtils.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(iriFromSecondOntology));
//					System.out.println("TESTING " + iriFromFirstOntology + " and " + iriFromSecondOntology
//							+ " gives a similarity of: " + iriCosine);
				if (Double.isNaN(iriCosine)) {
					iriCosine = 0;
				}

				double currentSimilarity = iriCosine;

//				System.out.println(iriFromFirstOntology + " and " + iriFromSecondOntology + " got " + currentSimilarity);
//
//				if (currentSimilarity > maxSimilarity) {
//					maxSimilarity = currentSimilarity;
//					candidate = propertyFromSecondOntology;
//				}

				if (currentSimilarity > maxSimilarity) {
					candidate = propertyFromSecondOntology;
					maxSimilarity = currentSimilarity;
				}
			} // finished propertyFromSecondOntology
//			System.out.println("Object property max similarity: " + maxSimilarity);
			if (maxSimilarity > distLimit) {
				OWLEquivalentObjectPropertiesAxiom equivalentClassAxiom = mappingsFactory
						.getOWLEquivalentObjectPropertiesAxiom(propertyFromFirstOntology, candidate);
				mappingsManager.addAxiom(mappings, equivalentClassAxiom);

				OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
				OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
						confidenceLiteral);
				OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
						.getOWLAnnotationAssertionAxiom(propertyFromFirstOntology.getIRI(), annotation);

				mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
//					usedPropertiesFromSecondOntology.add(candidate.getIRI().toString());

				System.out.println("Found mapping: " + equivalentClassAxiom + " distance: " + maxSimilarity);
				numCandidates++;
			}
		} // finished propertyFromFirstOntology
		System.out.println("Found " + numCandidates + " object property candidates:");
	} // finished generateBestObjectProperties()

	public void generateDataProperties() {
		int numCandidates = 0;
		ArrayList<OWLDataProperty> usedPropertyFromSecondOntology = new ArrayList<>();

		for (OWLDataProperty propertyFromFirstOntology : onto1.getDataPropertiesInSignature()) {
			String iriFromFirstOntology = propertyFromFirstOntology.getIRI().toString();
			double maxSimilarity = 0;
			OWLDataProperty candidate = null;

			for (OWLDataProperty propertyFromSecondOntology : onto2.getDataPropertiesInSignature()) {
//				if (usedPropertyFromSecondOntology.contains(propertyFromSecondOntology)) {
//					continue;
//				}
				String iriFromSecondOntology = propertyFromSecondOntology.getIRI().toString();

				double iriCosine = 0;
//					System.out.println("testing: " + iriFromFirstOntology + " and " + iriFromSecondOntology);
				iriCosine = VectorUtils.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(iriFromSecondOntology));
//					System.out.println("TESTING " + iriFromFirstOntology + " and " + iriFromSecondOntology
//							+ " gives a similarity of: " + iriCosine);
				if (Double.isNaN(iriCosine)) {
					iriCosine = 0;
				}

				double currentSimilarity = iriCosine;
//				System.out.println(iriFromFirstOntology + " and " + iriFromSecondOntology + " got " + currentSimilarity);

//				if (currentSimilarity > maxSimilarity) {
//					maxSimilarity = currentSimilarity;
//					candidate = propertyFromSecondOntology;
//				}
				if (currentSimilarity > distLimit) {
					OWLEquivalentDataPropertiesAxiom equivalentClassAxiom = mappingsFactory
							.getOWLEquivalentDataPropertiesAxiom(propertyFromFirstOntology, propertyFromSecondOntology);
					mappingsManager.addAxiom(mappings, equivalentClassAxiom);

					OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(currentSimilarity);
					OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
							confidenceLiteral);
					OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
							.getOWLAnnotationAssertionAxiom(propertyFromFirstOntology.getIRI(), annotation);

					mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
//					usedPropertyFromSecondOntology.add(propertyFromSecondOntology);

					System.out.println("Found mapping: " + equivalentClassAxiom + " distance: " + currentSimilarity);
					numCandidates++;
				}
			} // finished propertyFromSecondOntology
//			System.out.println("Data property max similarity: " + maxSimilarity);

		} // finished propertyFromFirstOntology
		System.out.println("Found " + numCandidates + " data property candidates:");
	}
}
