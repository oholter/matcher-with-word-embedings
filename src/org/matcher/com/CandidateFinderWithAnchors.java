package org.matcher.com;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
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
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trainer.com.OntologyProjector;
import org.trainer.com.OntologyReader;
import org.trainer.com.TranslationMatrix;
import org.trainer.com.Vector;
import org.trainer.com.WordEmbeddingsTrainer;

import edit_distance.EditDistance;
import io.AlignmentsReader;
import io.OAEIAlignmentOutput;
import io.OAEIAlignmentsReader;
import io.Utilities;
import mapping.object.MappingObjectStr;
import mappings.evaluation.ClassMappingsEvaluator;
import mappings.evaluation.MappingsEvaluator;

public class CandidateFinderWithAnchors extends CandidateFinder {
	final double ANCHOR_SIMILARITY = 0.95;
	final double TRAINING_RATE = 0.2;
	final String PRETRAINED_MODEL_PATH = "/home/ole/master/word2vec/models/fil9.model";
	double subClassSimilarity;

	String currentDir;
	OWLOntology mergedOnto;
	OWLOntology anchorOntology;
	TranslationMatrix superClassMatrix;

	OAEIAlignmentOutput output = new OAEIAlignmentOutput("/home/ole/master/mappings", "ekaw", "ekaw2");

	public CandidateFinderWithAnchors(OWLOntology o1, OWLOntology o2, OWLOntology mergedOnto, String modelPath,
			double distLimit, double subClassSimilarity) throws Exception {
		super(o1, o2, modelPath, distLimit);
		this.mergedOnto = mergedOnto;
		currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
//		trainer = new WordEmbeddingsTrainer("/home/ole/master/test_onto/merged.ttl", modelPath);
//		trainer.loadModel();
		this.modelPath = modelPath;
		this.subClassSimilarity = subClassSimilarity;

	}

	public void setTrainer(WordEmbeddingsTrainer trainer) {
		this.trainer = trainer;
	}

	public void findAnchors() throws Exception {

		org.matcher.com.CandidateFinderWithPretrainedWordVectors anchorFinder = new org.matcher.com.CandidateFinderWithPretrainedWordVectors(
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

	public void addAnchorsToOntology(OWLOntology onto) {
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
			System.out.println(eqClassAxiom);
			numAnchors++;
		}
		for (OWLEquivalentObjectPropertiesAxiom eqObjectPropertyAxiom : equivalentObjectProperties) {
			mergedManager.addAxiom(onto, eqObjectPropertyAxiom);
			System.out.println(eqObjectPropertyAxiom);
			numAnchors++;
		}
		for (OWLEquivalentDataPropertiesAxiom eqDataPropertyAxiom : equivalentDataProperties) {
			mergedManager.addAxiom(onto, eqDataPropertyAxiom);
			System.out.println(eqDataPropertyAxiom);
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
				if (Vector.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(iriFromSecondOntology)) > distLimit) {
					System.out.println(iriFromSecondOntology);
				}
			}
		}
		System.out.println("Finished lookup ... ... ...");
	}

	/** This generates all class candidates then diambiguate **/
	public void generateAllClassAndDisambiguateCandidates() {
		int numCandidates = 0;

		for (OWLClass classFromFirstOntology : onto1.getClassesInSignature()) {
			ArrayList<OWLClass> candidates = new ArrayList<>();
			String iriFromFirstOntology = classFromFirstOntology.getIRI().toString();

			for (OWLClass classFromSecondOntology : onto2.getClassesInSignature()) {
				String iriFromSecondOntology = classFromSecondOntology.getIRI().toString();

				double iriCosine = 0;
				iriCosine = Vector.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(iriFromSecondOntology));
				if (Double.isNaN(iriCosine)) {
					iriCosine = 0;
				}

				double currentSimilarity = iriCosine; //

//				double secondSuperClassOfFirst = Vector.cosineSimilarity(
//						superClassMatrix.estimateTarget(trainer.getWordVector(iriFromSecondOntology)),
//						trainer.getWordVector(iriFromFirstOntology));

//				double firstSuperClassOfSecond = Vector.cosineSimilarity(
//						superClassMatrix.estimateTarget(trainer.getWordVector(iriFromFirstOntology)),
//						trainer.getWordVector(iriFromSecondOntology));

//				double subClassDist = Math.abs(firstSuperClassOfSecond - secondSuperClassOfFirst);

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
				similarity = Vector.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(candidate.getIRI().toString()));
				if (Double.isNaN(similarity)) {
					similarity = 0;
				}

				try {
					output.addClassMapping2Output(iriFromFirstOntology, candidate.getIRI().toString(), Utilities.EQ,
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

	/** This will add all possible class candidates to the alignment */
	public void generateAllClassCandidates() {
		int numCandidates = 0;

		for (OWLClass classFromFirstOntology : onto1.getClassesInSignature()) {
			ArrayList<OWLClass> candidates = new ArrayList<>();
			String iriFromFirstOntology = classFromFirstOntology.getIRI().toString();

			for (OWLClass classFromSecondOntology : onto2.getClassesInSignature()) {
				String iriFromSecondOntology = classFromSecondOntology.getIRI().toString();

				double iriCosine = 0;
				iriCosine = Vector.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(iriFromSecondOntology));
				if (Double.isNaN(iriCosine)) {
					iriCosine = 0;
				}

				double currentSimilarity = iriCosine; //

				if (currentSimilarity > distLimit) { // candidate
					try {
						output.addClassMapping2Output(iriFromFirstOntology, classFromSecondOntology.getIRI().toString(),
								Utilities.EQ, currentSimilarity);
					} catch (Exception e) {
						e.printStackTrace();
					}
					numCandidates++;
				}

			} // end classFromSecondOntology

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
			double cosineSimilarity = Vector.cosineSimilarity(trainer.getWordVector(classToMatch.getIRI().toString()),
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
				iriCosine = Vector.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
						trainer.getWordVector(iriFromSecondOntology));
//					System.out.println("TESTING " + iriFromFirstOntology + " and " + iriFromSecondOntology
//							+ " gives a similarity of: " + iriCosine);
				if (Double.isNaN(iriCosine)) {
					iriCosine = 0;
				}

				double currentSimilarity = iriCosine;

//				System.out.println("Testing: " + iriFromFirstOntology + " AND "
//						+ classFromSecondOntology.getIRI().toString() + " SIMILARITY: " + currentSimilarity);

//				if (currentSimilarity > maxSimilarity) {
//					maxSimilarity = currentSimilarity;
//					candidate = classFromSecondOntology;
//				}

//				double secondSuperClassOfFirst = Vector.cosineSimilarity(
//						superClassMatrix.estimateTarget(trainer.getWordVector(iriFromSecondOntology)),
//						trainer.getWordVector(iriFromFirstOntology));

//				double firstSuperClassOfSecond = Vector.cosineSimilarity(
//						superClassMatrix.estimateTarget(trainer.getWordVector(iriFromFirstOntology)),
//						trainer.getWordVector(iriFromSecondOntology));

//				double subClassDist = Math.abs(firstSuperClassOfSecond - secondSuperClassOfFirst);
				if (currentSimilarity > maxSimilarity) {
					candidate = classFromSecondOntology;
					maxSimilarity = currentSimilarity;
				}

//				if (secondSuperClassOfFirst > subClassSimilarity) {
//					OWLSubClassOfAxiom ax = mappingsFactory.getOWLSubClassOfAxiom(classFromFirstOntology,
//							classFromSecondOntology);
//					mappingsManager.addAxiom(mappings, ax);
//					System.out.println(iriFromFirstOntology + " subClassOf " + iriFromSecondOntology + ": "
//							+ secondSuperClassOfFirst);
//				}

//				if (firstSuperClassOfSecond > subClassSimilarity) {
//					OWLSubClassOfAxiom ax = mappingsFactory.getOWLSubClassOfAxiom(classFromSecondOntology,
//							classFromFirstOntology);
//					mappingsManager.addAxiom(mappings, ax);
//					System.out.println(iriFromSecondOntology + " subClassOf " + iriFromFirstOntology + ": "
//							+ firstSuperClassOfSecond);
//				}
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
					output.addClassMapping2Output(iriFromFirstOntology, candidate.getIRI().toString(), Utilities.EQ,
							maxSimilarity);
				} catch (Exception e) {
					e.printStackTrace();
				}
//					System.out.println(
//							iriFromFirstOntology + " subClassOf " + iriFromSecondOntology + " gives similarity: "
//									+ Vector.cosineSimilarity(
//											subClassMatrix.estimateTarget(trainer.getWordVector(iriFromSecondOntology)),
//											trainer.getWordVector(iriFromFirstOntology)));
//					System.out.println(
//							iriFromSecondOntology + " subClassOf " + iriFromFirstOntology + " gives similarity: "
//									+ Vector.cosineSimilarity(
//											subClassMatrix.estimateTarget(trainer.getWordVector(iriFromFirstOntology)),
//											trainer.getWordVector(iriFromSecondOntology)));
				numCandidates++;
			}

		} // finished classFromFirstOntology

		System.out.println("Found " + numCandidates + " class candidates:");
	} // finished generateBestClassCandidates()

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
				iriCosine = Vector.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
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
				iriCosine = Vector.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
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
				iriCosine = Vector.cosineSimilarity(trainer.getWordVector(iriFromFirstOntology),
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

	public static void main(String[] args) throws Exception {
		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();

		BasicConfigurator.configure();
		OntologyReader reader = new OntologyReader();
		reader.setFname("/home/ole/master/test_onto/cmt.owl");
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname("/home/ole/master/test_onto/ekaw.owl");
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		// For training of ontology start:
		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });
		CandidateFinderWithAnchors finder = new CandidateFinderWithAnchors(onto1, onto2, mergedOnto,
				currentDir + "/temp/out.txt", 0.82, 0.9);

		/* Adding anchors, by using word embeddings or by manually adding them */
//		finder.findAnchors(); /* this will use word embeddings to find anchors */

		/* Adding anchors by reading an alignments file */
		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(
				"/home/ole/master/test_onto/reference_alignments/cmt-ekaw.rdf", onto1, onto2);
//		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(
//				"/home/ole/master/logmap_standalone/output/logmap2_mappings.rdf", onto1, onto2);

		double fractionOfMappings = 1;
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

		Walks walks = new Walks("/home/ole/master/test_onto/merged.ttl");
//		Walks_rdf2vec walks = new Walks_rdf2vec();
//		walks.loadFromRdfFile("/home/ole/master/test_onto/merged.ttl");
		walks.generateWalks();
		String walksFile = walks.getOutputFile();

		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer(walksFile, currentDir + "/temp/out.txt");
		trainer.train();
//		trainer.loadModel();
		finder.setTrainer(trainer);

		finder.createMappings(); // this runs the program

		// evaluating the mappings
		System.out.println("--------------------------------------------");
		System.out.println("The alignments file used to provide anchors: ");
		MappingsEvaluator evaluator = new ClassMappingsEvaluator(
				"/home/ole/master/test_onto/reference_alignments/cmt-ekaw.rdf",
				"/home/ole/master/logmap_standalone/output/logmap2_mappings.rdf", onto1, onto2);

		System.out.println("Precision: " + evaluator.calculatePrecision());
		System.out.println("Recall: " + evaluator.calculateRecall());
		System.out.println("F-measure: " + evaluator.calculateFMeasure());
		
		System.out.println("--------------------------------------------");
		
		System.out.println("This system:");
		evaluator = new ClassMappingsEvaluator(
				"/home/ole/master/test_onto/reference_alignments/cmt-ekaw.rdf",
				finder.output.returnAlignmentFile().getFile(), onto1, onto2);

		System.out.println("Precision: " + evaluator.calculatePrecision());
		System.out.println("Recall: " + evaluator.calculateRecall());
		System.out.println("F-measure: " + evaluator.calculateFMeasure());
		System.out.println("--------------------------------------------");
	}
}
