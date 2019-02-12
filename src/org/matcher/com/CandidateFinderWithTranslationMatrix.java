package org.matcher.com;

import java.io.File;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trainer.com.GradientDescent;
import org.trainer.com.OntologyProjector;
import org.trainer.com.OntologyReader;
import org.trainer.com.TranslationMatrix;
import org.trainer.com.Vector;
import org.trainer.com.WordEmbeddingsTrainer;

public class CandidateFinderWithTranslationMatrix extends CandidateFinder {
	final double ANCHOR_SIMILARITY = 0.95;
	final double MAX_ERROR = 1E-3;
	final int NUM_ITERATIONS = 150000;
	final double TRAINING_RATE = 0.2;
	final String PRETRAINED_MODEL_PATH = "/home/ole/master/word2vec/models/fil9.model";
	OWLOntology anchorOntology;
	String nameSpace1;

	final String currentDir;
	double[] anchorVector;
	ArrayList<ArrayList<Double>> anchorDifference;

	ArrayList<String> anchorsFromFirstOntology;
	ArrayList<String> anchorsFromSecondOntology;
	ArrayList<Double> differenceVectors;

	double[][] anchorVectorsFromFirstOntology;
	double[][] anchorVectorsFromSecondOntology;
//	double[][] translationMatrix;
	GradientDescent gradientDescent;
	TranslationMatrix translationMatrix;

	public CandidateFinderWithTranslationMatrix(OWLOntology o1, OWLOntology o2, String modelPath, double distLimit,
			String nameSpace1) throws Exception {
		super(o1, o2, modelPath, distLimit);
		currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
		trainer = new WordEmbeddingsTrainer("/home/ole/master/test_onto/merged.ttl", modelPath);
		trainer.loadModel();
		anchorsFromFirstOntology = new ArrayList<>();
		anchorsFromSecondOntology = new ArrayList<>();
		this.nameSpace1 = nameSpace1;
	}

	public void createMappings() {
		mappingsManager = OWLManager.createOWLOntologyManager();
		mappingsFactory = mappingsManager.getOWLDataFactory();

		try {
			mappings = mappingsManager.createOntology();
			listAnchorVectors(mappings);
			System.out.println(anchorsFromFirstOntology.size() + ": " + anchorsFromSecondOntology.size());
//			createTranslationMatrix();
			translationMatrix = new TranslationMatrix(trainer.getModel(),
					anchorsFromFirstOntology.toArray(new String[0]), anchorsFromSecondOntology.toArray(new String[0]));

//			System.out.println("similarity between http://ekaw#Person and http://cmt#Person is: " + Vector
//					.cosineSimilarity(translationMatrix.estimateTarget(trainer.getWordVector("http://ekaw#Person")),
//							trainer.getWordVector("http://cmt#Person")));

			generateClassCandidates();
			generateObjectProperties();
//			generateDataProperties();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addAnchor(String firstIRI, AxiomType type, String secondIRI) throws Exception {
		if (anchorOntology == null) {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			anchorOntology = man.createOntology();
		}
		OWLOntologyManager man = anchorOntology.getOWLOntologyManager();
		OWLDataFactory factory = man.getOWLDataFactory();

		if (type == AxiomType.EQUIVALENT_CLASSES) {
			OWLClass cl1 = factory.getOWLClass(IRI.create(firstIRI));
			OWLClass cl2 = factory.getOWLClass(IRI.create(secondIRI));
			OWLAxiom ax = factory.getOWLEquivalentClassesAxiom(cl1, cl2);
			man.addAxiom(anchorOntology, ax);
		} else if (type == AxiomType.EQUIVALENT_OBJECT_PROPERTIES) {
			OWLObjectProperty p1 = factory.getOWLObjectProperty(IRI.create(firstIRI));
			OWLObjectProperty p2 = factory.getOWLObjectProperty(IRI.create(secondIRI));
			OWLAxiom ax = factory.getOWLEquivalentObjectPropertiesAxiom(p1, p2);
			man.addAxiom(anchorOntology, ax);
		} else if (type == AxiomType.EQUIVALENT_DATA_PROPERTIES) {
			OWLDataProperty p1 = factory.getOWLDataProperty(IRI.create(firstIRI));
			OWLDataProperty p2 = factory.getOWLDataProperty(IRI.create(secondIRI));
			OWLAxiom ax = factory.getOWLEquivalentDataPropertiesAxiom(p1, p2);
			man.addAxiom(anchorOntology, ax);
		} else if (type == AxiomType.SUBCLASS_OF) {
			OWLClass cl1 = factory.getOWLClass(IRI.create(firstIRI));
			OWLClass cl2 = factory.getOWLClass(IRI.create(secondIRI));
			OWLAxiom ax = factory.getOWLSubClassOfAxiom(cl1, cl2);
			man.addAxiom(anchorOntology, ax);
		}
	}

	public void findAnchors() throws Exception {
		org.matcher.com.CandidateFinderWithPretrainedWordVectors anchorFinder = new org.matcher.com.CandidateFinderWithPretrainedWordVectors(
				onto1, onto2, ANCHOR_SIMILARITY, PRETRAINED_MODEL_PATH);
		anchorFinder.createMappings();
		anchorOntology = anchorFinder.getMappings();
	}

	public void listAnchorVectors(OWLOntology onto) {
		OWLOntologyManager mergedManager = OWLManager.createOWLOntologyManager();
		Set<OWLEquivalentClassesAxiom> equivalentClasses = anchorOntology.getAxioms(AxiomType.EQUIVALENT_CLASSES);
		Set<OWLEquivalentObjectPropertiesAxiom> equivalentObjectProperties = anchorOntology
				.getAxioms(AxiomType.EQUIVALENT_OBJECT_PROPERTIES);
		Set<OWLEquivalentDataPropertiesAxiom> equivalentDataProperties = anchorOntology
				.getAxioms(AxiomType.EQUIVALENT_DATA_PROPERTIES);

		int numAnchors = 0;

		Set<OWLClass> classes = anchorOntology.getClassesInSignature();

		for (OWLEquivalentClassesAxiom eqClassAxiom : equivalentClasses) {
//			mergedManager.addAxiom(onto, eqClassAxiom);
			Stream<OWLClass> strm = eqClassAxiom.getNamedClasses().stream();
			OWLClass[] arr = strm.toArray(OWLClass[]::new);
			if (arr.length == 2) {
				OWLClass c1;
				OWLClass c2;
				if (arr[0].getIRI().toString().contains(nameSpace1)) {
					c1 = arr[0];
					c2 = arr[1];
				} else {
					c1 = arr[1];
					c2 = arr[0];
				}

				anchorsFromFirstOntology.add(c1.getIRI().toString());
				anchorsFromSecondOntology.add(c2.getIRI().toString());
				System.out.println("Added: " + c1.getIRI().toString());
				System.out.println("Added: " + c2.getIRI().toString());
				numAnchors++;
			}

		}
		for (OWLEquivalentObjectPropertiesAxiom eqObjectPropertyAxiom : equivalentObjectProperties) {
			Stream<OWLObjectProperty> strm = eqObjectPropertyAxiom.getObjectPropertiesInSignature().stream();
			OWLObjectProperty[] arr = strm.toArray(OWLObjectProperty[]::new);
			if (arr.length == 2) {
				OWLObjectProperty p1;
				OWLObjectProperty p2;
				if (arr[0].getIRI().toString().contains(nameSpace1)) {
					p1 = arr[0];
					p2 = arr[1];
				} else {
					p1 = arr[1];
					p2 = arr[0];
				}

				anchorsFromFirstOntology.add(p1.getIRI().toString());
				anchorsFromSecondOntology.add(p2.getIRI().toString());
				System.out.println("Added: " + p1.getIRI().toString());
				System.out.println("Added: " + p2.getIRI().toString());
				numAnchors++;
			}
//			mergedManager.addAxiom(onto, eqObjectPropertyAxiom);
//			numAnchors++;
		}
		for (OWLEquivalentDataPropertiesAxiom eqDataPropertyAxiom : equivalentDataProperties) {
			Stream<OWLDataProperty> strm = eqDataPropertyAxiom.getDataPropertiesInSignature().stream();
			OWLDataProperty[] arr = strm.toArray(OWLDataProperty[]::new);
			if (arr.length == 2) {
				OWLDataProperty p1;
				OWLDataProperty p2;
				if (arr[0].getIRI().toString().contains(nameSpace1)) {
					p1 = arr[0];
					p2 = arr[1];
				} else {
					p1 = arr[1];
					p2 = arr[0];
				}

				anchorsFromFirstOntology.add(p1.getIRI().toString());
				anchorsFromSecondOntology.add(p2.getIRI().toString());
				System.out.println("Added: " + p1.getIRI().toString());
				System.out.println("Added: " + p2.getIRI().toString());
				numAnchors++;
			}
//			mergedManager.addAxiom(onto, eqDataPropertyAxiom);
//			numAnchors++;
		}
		System.out.println("listAnchorVectors(): Added " + numAnchors + " anchors");
	}

	/**
	 * creating vector matrices using the anchor words and calculating the
	 * difference between the vectors
	 */
	public void createTranslationMatrix() {
		anchorVectorsFromFirstOntology = new double[anchorsFromFirstOntology.size()][];
		anchorVectorsFromSecondOntology = new double[anchorsFromSecondOntology.size()][];

		for (int i = 0; i < anchorsFromFirstOntology.size(); i++) {
			anchorVectorsFromFirstOntology[i] = trainer.getModel().getWordVector(anchorsFromFirstOntology.get(i));
		}

		for (int i = 0; i < anchorsFromSecondOntology.size(); i++) {
			anchorVectorsFromSecondOntology[i] = trainer.getModel().getWordVector(anchorsFromSecondOntology.get(i));
		}

		gradientDescent = new GradientDescent(anchorVectorsFromFirstOntology, anchorVectorsFromSecondOntology,
				TRAINING_RATE, NUM_ITERATIONS, MAX_ERROR);
		System.out.println("Before training");
//		gradientDescent.printMatrix();
		gradientDescent.solve();
//		translationMatrix = gradientDescent.getMatrix();
		System.out.println("After training");
//		gradientDescent.printMatrix();
		System.out.println("ERROR: " + gradientDescent.calculateError());

//		for (int i = 0; i < anchorVectorsFromFirstOntology.length; i++) {
//			vectorDifferences[i] = trainer.subtractTwoVectors(anchorVectorsFromSecondOntology[i], anchorVectorsFromFirstOntology[i]);
//		}
//		
//		for (int i = 0; i < vectorDifferences.length; i++) {
//			System.out.println("Anchor vectors from first ontology:");
//			Arrays.stream(anchorVectorsFromFirstOntology[i]).forEach(System.out::println);
//			System.out.println("Anchor vectors from second ontology:");
//			Arrays.stream(anchorVectorsFromSecondOntology[i]).forEach(System.out::println);
//			System.out.println("Difference: ");
//			Arrays.stream(vectorDifferences[i]).forEach(System.out::println);
//		}
	}

//	public void createRegressionModel() {
//		OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
//        for (int i = 0; i < anchorVectorsFromFirstOntology.length; i++) {
//        	regression.
//        	regression.addData(anchorVectorsFromFirstOntology[i], anchorVectorsFromSecondOntology[i]);
//        }
//	}

	/** This will generate at most one candidate for each class **/
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
				iriCosine = Vector.cosineSimilarity(
						translationMatrix.estimateTarget(trainer.getWordVector(iriFromFirstOntology)),
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

				if (currentSimilarity > maxSimilarity) {
					maxSimilarity = currentSimilarity;
					candidate = classFromSecondOntology;
				}
			} // end classFromSecondOntology

			if (maxSimilarity > distLimit) {
				OWLEquivalentClassesAxiom equivalentClassAxiom = mappingsFactory
						.getOWLEquivalentClassesAxiom(classFromFirstOntology, candidate);
				mappingsManager.addAxiom(mappings, equivalentClassAxiom);

				OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
				OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
						confidenceLiteral);
				OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
						.getOWLAnnotationAssertionAxiom(classFromFirstOntology.getIRI(), annotation);

				mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
				usedClassesFromSecondOntology.add(candidate);

				System.out.println("Found mapping: " + equivalentClassAxiom + " distance: " + maxSimilarity);
				numCandidates++;
			} // finished if
		} // finished classFromFirstOntology

		System.out.println("Found " + numCandidates + " class candidates:");

	} // finished generateBestClassCandidates()

	/** This will generate all candidates **/
	public void generateAllClassCandidates() {
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
				iriCosine = Vector.cosineSimilarity(
						translationMatrix.estimateTarget(trainer.getWordVector(iriFromFirstOntology)),
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

				if (currentSimilarity > distLimit) {
					OWLEquivalentClassesAxiom equivalentClassAxiom = mappingsFactory
							.getOWLEquivalentClassesAxiom(classFromFirstOntology, classFromSecondOntology);
					mappingsManager.addAxiom(mappings, equivalentClassAxiom);

					OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
					OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
							confidenceLiteral);
					OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
							.getOWLAnnotationAssertionAxiom(classFromFirstOntology.getIRI(), annotation);

					mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
//					usedClassesFromSecondOntology.add(candidate);

					System.out.println("Found mapping: " + equivalentClassAxiom + " distance: " + currentSimilarity);
					numCandidates++;
				}
			} // end classFromSecondOntology

//			if (maxSimilarity > distLimit) {
//				OWLEquivalentClassesAxiom equivalentClassAxiom = mappingsFactory
//						.getOWLEquivalentClassesAxiom(classFromFirstOntology, candidate);
//				mappingsManager.addAxiom(mappings, equivalentClassAxiom);
//
//				OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
//				OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
//						confidenceLiteral);
//				OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
//						.getOWLAnnotationAssertionAxiom(classFromFirstOntology.getIRI(), annotation);
//
//				mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
//				usedClassesFromSecondOntology.add(candidate);
//
//				System.out.println("Found mapping: " + equivalentClassAxiom + " distance: " + maxSimilarity);
//				numCandidates++;
//			}
		} // finished classFromFirstOntology

		System.out.println("Found " + numCandidates + " class candidates:");
	} // finished generateClassCandidates()

	
	/** This will generate all object properties pairs **/
	public void generateAllObjectProperties() {
		int numCandidates = 0;
		ArrayList<String> usedPropertiesFromSecondOntology = new ArrayList<>();

		for (OWLObjectProperty propertyFromFirstOntology : onto1.getObjectPropertiesInSignature()) {
			String iriFromFirstOntology = propertyFromFirstOntology.getIRI().toString();
			OWLObjectProperty candidate = null;

			for (OWLObjectProperty propertyFromSecondOntology : onto2.getObjectPropertiesInSignature()) {
//				if (usedPropertiesFromSecondOntology.contains(propertyFromSecondOntology.getIRI().toString())) {
//					continue; // already used
//				}
				String iriFromSecondOntology = propertyFromSecondOntology.getIRI().toString();
				String labelFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "label"));
				String commentFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "comment"));

//					System.out.println("testing: " + iriFromFirstOntology + " and " + iriFromSecondOntology);
				double iriCosine = Vector.cosineSimilarity(
						translationMatrix.estimateTarget(trainer.getWordVector(iriFromFirstOntology)),
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
					usedPropertiesFromSecondOntology.add(propertyFromSecondOntology.getIRI().toString());

					System.out.println("Found mapping: " + equivalentClassAxiom + " distance: " + currentSimilarity);
					numCandidates++;
				}
			} // finished propertyFromSecondOntology
//			System.out.println("Object property max similarity: " + maxSimilarity);

//			if (maxSimilarity > distLimit) {
//				OWLEquivalentObjectPropertiesAxiom equivalentClassAxiom = mappingsFactory
//						.getOWLEquivalentObjectPropertiesAxiom(propertyFromFirstOntology, candidate);
//				mappingsManager.addAxiom(mappings, equivalentClassAxiom);
//
//				OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
//				OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
//						confidenceLiteral);
//				OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
//						.getOWLAnnotationAssertionAxiom(propertyFromFirstOntology.getIRI(), annotation);
//
//				mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
//				usedPropertiesFromSecondOntology.add(candidate.getIRI().toString());
//
//				System.out.println("Found mapping: " + equivalentClassAxiom);
//				numCandidates++;
//			}
		} // finished propertyFromFirstOntology
		System.out.println("Found " + numCandidates + " object property candidates:");

	} // finished generateAllObjectProperties()
	
	/** This will generate the best object property pair **/
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
				double iriCosine = Vector.cosineSimilarity(
						translationMatrix.estimateTarget(trainer.getWordVector(iriFromFirstOntology)),
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
				if (currentSimilarity > maxSimilarity) {
					maxSimilarity = currentSimilarity;
					candidate = propertyFromSecondOntology;
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
				usedPropertiesFromSecondOntology.add(candidate.getIRI().toString());

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
			String labelFromFirstOntology = normalizeString(findAnnotation(propertyFromFirstOntology, onto1, "label"));
			String commentFromFirstOntology = normalizeString(
					findAnnotation(propertyFromFirstOntology, onto1, "comment"));
			double maxSimilarity = 0;
			OWLDataProperty candidate = null;

			for (OWLDataProperty propertyFromSecondOntology : onto2.getDataPropertiesInSignature()) {
//				if (usedPropertyFromSecondOntology.contains(propertyFromSecondOntology)) {
//					continue;
//				}
				String iriFromSecondOntology = propertyFromSecondOntology.getIRI().toString();
				String labelFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "label"));
				String commentFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "comment"));

				double iriCosine = 0;
//					System.out.println("testing: " + iriFromFirstOntology + " and " + iriFromSecondOntology);
				iriCosine = Vector.cosineSimilarity(
						translationMatrix.estimateTarget(trainer.getWordVector(iriFromFirstOntology)),
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

					OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
					OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
							confidenceLiteral);
					OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
							.getOWLAnnotationAssertionAxiom(propertyFromFirstOntology.getIRI(), annotation);

					mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
//					usedPropertyFromSecondOntology.add(candidate);

					System.out.println("Found mapping: " + equivalentClassAxiom + " distance: " + currentSimilarity);
					numCandidates++;
				}

			} // finished propertyFromSecondOntology
//			System.out.println("Data property max similarity: " + maxSimilarity);
//			if (maxSimilarity > distLimit) {
//				OWLEquivalentDataPropertiesAxiom equivalentClassAxiom = mappingsFactory
//						.getOWLEquivalentDataPropertiesAxiom(propertyFromFirstOntology, candidate);
//				mappingsManager.addAxiom(mappings, equivalentClassAxiom);
//
//				OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
//				OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
//						confidenceLiteral);
//				OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
//						.getOWLAnnotationAssertionAxiom(propertyFromFirstOntology.getIRI(), annotation);
//
//				mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
//				usedPropertyFromSecondOntology.add(candidate);
//
//				System.out.println("Found mapping: " + equivalentClassAxiom);
//				numCandidates++;
//			}
		} // finished propertyFromFirstOntology
		System.out.println("Found " + numCandidates + " data property candidates:");
	}

	public static void main(String[] args) throws Exception {
		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
		BasicConfigurator.configure();
		OntologyReader reader = new OntologyReader();
		reader.setFname("/home/ole/master/test_onto/ekaw.owl");
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname("/home/ole/master/test_onto/ekaw2.owl");
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		// For training of ontology start:

		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });
		OntologyReader.writeOntology(mergedOnto, "file:/home/ole/master/test_onto/merged.owl", "owl");

		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/merged.owl");
		projector.projectOntology();
		projector.saveModel("/home/ole/master/test_onto/merged.ttl");

		Walks walks = new Walks("/home/ole/master/test_onto/merged.ttl");
		walks.generateWalks();
		String walksFile = walks.getOutputFile();

		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer(walksFile, currentDir + "/temp/out.txt");
//		 trainer.stripAccents(); // optional
		trainer.train(); // optional

		// Finished for traning of ontology

//		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer("/home/ole/master/test_onto/merged.ttl",
//				currentDir + "/temp/out.txt");

//		trainer.loadModel();

		CandidateFinderWithTranslationMatrix finder = new CandidateFinderWithTranslationMatrix(onto1, onto2,
				currentDir + "/temp/out.txt", 0.0, "ekaw");

//		finder.findAnchors(); /* this will use word embeddings to find anchors */

//		finder.addAnchor("http://ekaw#Event", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Event");
//		finder.addAnchor("http://ekaw#Location", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Location");
//		finder.addAnchor("http://ekaw#Organisation", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Organisation");
//		finder.addAnchor("http://ekaw#Person", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Person");
//		finder.addAnchor("http://ekaw#Research_Topic", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Research_Topic");
//		finder.addAnchor("http://ekaw#Conference_Participant", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Conference_Participant");
//		finder.addAnchor("http://ekaw#Conference", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Conference");
		finder.addAnchor("http://ekaw#Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Paper");
		finder.addAnchor("http://ekaw#Regular_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Regular_Paper");
		finder.addAnchor("http://ekaw#Agency_Staff_Member", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Agency_Staff_Member");
		finder.addAnchor("http://ekaw#Accepted_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Accepted_Paper");
		finder.addAnchor("http://ekaw#Rejected_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Rejected_Paper");
		finder.addAnchor("http://ekaw#Evaluated_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Evaluated_Paper");
		finder.addAnchor("http://ekaw#Camera_Ready_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Camera_Ready_Paper");
		finder.addAnchor("http://ekaw#Positive_Review", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Positive_Review");
		finder.addAnchor("http://ekaw#Workshop_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Workshop_Paper");
		finder.addAnchor("http://ekaw#Industrial_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Industrial_Paper");
		finder.addAnchor("http://ekaw#Conference_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Conference_Paper");
		finder.addAnchor("http://ekaw#Industrial_Session", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Industrial_Session");
		finder.addAnchor("http://ekaw#Conference_Session", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Conference_Session");
		finder.addAnchor("http://ekaw#Regular_Session", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Regular_Session");
		finder.addAnchor("http://ekaw#Poster_Session", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Poster_Session");
		finder.addAnchor("http://ekaw#Demo_Session", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Demo_Session");
		finder.addAnchor("http://ekaw#Session", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Session");
		finder.addAnchor("http://ekaw#Abstract", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Abstract");
		finder.addAnchor("http://ekaw#Document", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Document");
		finder.addAnchor("http://ekaw#Paper_Author", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Paper_Author");
		finder.addAnchor("http://ekaw#Conference_Trip", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Conference_Trip");
		finder.addAnchor("http://ekaw#Social_Event", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Social_Event");
		finder.addAnchor("http://ekaw#Tutorial_Abstract", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Tutorial_Abstract");
		finder.addAnchor("http://ekaw#Submitted_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Submitted_Paper");
		finder.addAnchor("http://ekaw#Assigned_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Assigned_Paper");
		finder.addAnchor("http://ekaw#Negative_Review", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Negative_Review");
		finder.addAnchor("http://ekaw#Review", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Review");
		finder.addAnchor("http://ekaw#Neutral_Review", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Neutral_Review");
		finder.addAnchor("http://ekaw#Organising_Agency", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Organising_Agency");
		finder.addAnchor("http://ekaw#Academic_Institution", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Academic_Institution");
		finder.addAnchor("http://ekaw#Proceedings_Publisher", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Proceedings_Publisher");
		finder.addAnchor("http://ekaw#Poster_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Poster_Paper");
		finder.addAnchor("http://ekaw#Demo_Paper", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Demo_Paper");
		finder.addAnchor("http://ekaw#Research_Institute", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Research_Institute");
		finder.addAnchor("http://ekaw#Scientific_Event", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Scientific_Event");
		finder.addAnchor("http://ekaw#Invited_Talk_Abstract", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Invited_Talk_Abstract");
		finder.addAnchor("http://ekaw#Workshop_Session", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Workshop_Session");
		finder.addAnchor("http://ekaw#Track", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Track");
		finder.addAnchor("http://ekaw#Invited_Talk", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Invited_Talk");
		finder.addAnchor("http://ekaw#Workshop", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Workshop");
		finder.addAnchor("http://ekaw#Tutorial", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Tutorial");
		finder.addAnchor("http://ekaw#Contributed_Talk", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Contributed_Talk");
		finder.addAnchor("http://ekaw#Demo_Chair", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Demo_Chair");
		finder.addAnchor("http://ekaw#Tutorial_Chair", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Tutorial_Chair");
		finder.addAnchor("http://ekaw#PC_Chair", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#PC_Chair");
		finder.addAnchor("http://ekaw#PC_Member", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#PC_Member");
		finder.addAnchor("http://ekaw#OC_Member", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#OC_Member");
		finder.addAnchor("http://ekaw#Proceedings", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Proceedings");
		finder.addAnchor("http://ekaw#Programme_Brochure", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Programme_Brochure");
		finder.addAnchor("http://ekaw#Flyer", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Flyer");
		finder.addAnchor("http://ekaw#Web_Site", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Web_Site");
		finder.addAnchor("http://ekaw#Multi-author_Volume", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Multi-author_Volume");
		finder.addAnchor("http://ekaw#Individual_Presentation", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Individual_Presentation");
		finder.addAnchor("http://ekaw#OC_Chair", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#OC_Chair");
		finder.addAnchor("http://ekaw#Conference_Banquet", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Conference_Banquet");
		finder.addAnchor("http://ekaw#Workshop_Chair", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Workshop_Chair");
		finder.addAnchor("http://ekaw#Conference_Proceedings", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Conference_Proceedings");
		finder.addAnchor("http://ekaw#Session_Chair", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Session_Chair");
		finder.addAnchor("http://ekaw#University", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#University");
		finder.addAnchor("http://ekaw#Possible_Reviewer", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Possible_Reviewer");
		finder.addAnchor("http://ekaw#Invited_Speaker", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Invited_Speaker");
		finder.addAnchor("http://ekaw#Presenter", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Presenter");
		finder.addAnchor("http://ekaw#Early-Registered_Participant", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Early-Registered_Participant");
		finder.addAnchor("http://ekaw#Late-Registered_Participant", AxiomType.EQUIVALENT_CLASSES, "http://ekaw2#Late-Registered_Participant");



		finder.addAnchor("http://ekaw#hasReview", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#hasReview");
		finder.addAnchor("http://ekaw#writtenBy", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#writtenBy");
		finder.addAnchor("http://ekaw#updatedVersionOf", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#updatedVersionOf");
		finder.addAnchor("http://ekaw#volumeContainsPaper", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#volumeContainsPaper");
		finder.addAnchor("http://ekaw#partOfEvent", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#partOfEvent");
		finder.addAnchor("http://ekaw#hasEvent", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#hasEvent");
		finder.addAnchor("http://ekaw#presentationOfPaper", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#presentationOfPaper");
		finder.addAnchor("http://ekaw#eventOnList", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#eventOnList");
		finder.addAnchor("http://ekaw#listsEvent", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#listsEvent");
		finder.addAnchor("http://ekaw#inverse_of_partOf_7", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#inverse_of_partOf_7");
		finder.addAnchor("http://ekaw#references", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#references");
		finder.addAnchor("http://ekaw#referencedIn", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#referencedIn");
		finder.addAnchor("http://ekaw#reviewOfPaper", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#reviewOfPaper");
		finder.addAnchor("http://ekaw#organisedBy", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#organisedBy");
		finder.addAnchor("http://ekaw#organises", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#organises");
		finder.addAnchor("http://ekaw#paperPresentedAs", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#paperPresentedAs");
		finder.addAnchor("http://ekaw#reviewerOfPaper", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#reviewerOfPaper");
		finder.addAnchor("http://ekaw#locationOf", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#locationOf");
		finder.addAnchor("http://ekaw#heldIn", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#heldIn");
		finder.addAnchor("http://ekaw#publisherOf", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#publisherOf");
		finder.addAnchor("http://ekaw#scientificallyOrganises", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#scientificallyOrganises");
		finder.addAnchor("http://ekaw#scientificallyOrganisedBy", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#scientificallyOrganisedBy");
		finder.addAnchor("http://ekaw#authorOf", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#authorOf");
		finder.addAnchor("http://ekaw#hasUpdatedVersion", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#hasUpdatedVersion");
		finder.addAnchor("http://ekaw#technicallyOrganisedBy", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#technicallyOrganisedBy");
		finder.addAnchor("http://ekaw#technicallyOrganises", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#technicallyOrganises");
		finder.addAnchor("http://ekaw#paperInVolume", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#paperInVolume");
		finder.addAnchor("http://ekaw#coversTopic", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#coversTopic");
		finder.addAnchor("http://ekaw#topicCoveredBy", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#topicCoveredBy");
		finder.addAnchor("http://ekaw#reviewWrittenBy", AxiomType.EQUIVALENT_OBJECT_PROPERTIES, "http://ekaw2#reviewWrittenBy");

		finder.createMappings();

		OWLOntology o = finder.getMappings();
		OntologyReader.writeOntology(o, "file:/home/ole/master/test_onto/conference_mappings.owl", "owl");
	}
}
