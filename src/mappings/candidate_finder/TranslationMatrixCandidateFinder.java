package mappings.candidate_finder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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

import com.hp.hpl.jena.iri.impl.Test;

import io.AlignmentsReader;
import io.OAEIAlignmentsReader;
import io.OntologyReader;
import mapping.object.MappingObjectStr;
import mappings.evaluation.ClassMappingsEvaluator;
import mappings.evaluation.MappingsEvaluator;
import mappings.trainer.GradientDescent;
import mappings.trainer.OntologyProjector;
import mappings.trainer.TranslationMatrix;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.AlignmentUtilities;
import mappings.utils.TestRunUtils;
import mappings.utils.VectorUtils;
import mappings.walks_generator.Walks;

public class TranslationMatrixCandidateFinder extends CandidateFinder {
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

	public TranslationMatrixCandidateFinder(OWLOntology o1, OWLOntology o2, String modelPath, double distLimit,
			String nameSpace1) throws Exception {
		super(o1, o2, modelPath, distLimit);
		currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
//		trainer = new WordEmbeddingsTrainer("/home/ole/master/test_onto/merged.ttl", modelPath);
//		trainer.loadModel();
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
//			generateObjectProperties();
//			generateDataProperties();
			output.saveOutputFile();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setTrainer(WordEmbeddingsTrainer trainer) {
		this.trainer = trainer;
	}

	public void addAnchor(String firstIRI, String secondIRI) throws Exception {
		if (anchorOntology == null) {
			OWLOntologyManager man = OWLManager.createOWLOntologyManager();
			anchorOntology = man.createOntology();
		}
		OWLOntologyManager man = anchorOntology.getOWLOntologyManager();
		OWLDataFactory factory = man.getOWLDataFactory();

		IRI iri = IRI.create(firstIRI);
		boolean isClass = onto1.containsClassInSignature(iri) || onto2.containsClassInSignature(iri);
		boolean isObjectProperty = onto1.containsObjectPropertyInSignature(iri)
				|| onto2.containsObjectPropertyInSignature(iri);
		boolean isDataProperty = onto1.containsDataPropertyInSignature(iri)
				|| onto2.containsDataPropertyInSignature(iri);

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
			System.out.println("Not able to find type of axiom: " + firstIRI);
		}
	}

	public void findAnchors() throws Exception {
		mappings.candidate_finder.PretrainedVectorCandidateFinder anchorFinder = new mappings.candidate_finder.PretrainedVectorCandidateFinder(
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
			if (trainer.getModel().hasWord(anchorsFromFirstOntology.get(i))) {
				anchorVectorsFromFirstOntology[i] = trainer.getModel().getWordVector(anchorsFromFirstOntology.get(i));
			}
		}

		for (int i = 0; i < anchorsFromSecondOntology.size(); i++) {
			if (trainer.getModel().hasWord(anchorsFromSecondOntology.get(i))) {
				anchorVectorsFromSecondOntology[i] = trainer.getModel().getWordVector(anchorsFromSecondOntology.get(i));
			}
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
				iriCosine = VectorUtils.cosineSimilarity(
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
				try {
					output.addClassMapping2Output(iriFromFirstOntology, candidate.getIRI().toString(),
							AlignmentUtilities.EQ, maxSimilarity);
				} catch (Exception e) {
					e.printStackTrace();
				}
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
				iriCosine = VectorUtils.cosineSimilarity(
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
				double iriCosine = VectorUtils.cosineSimilarity(
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
				double iriCosine = VectorUtils.cosineSimilarity(
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
				iriCosine = VectorUtils.cosineSimilarity(
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
		String firstOntologyFile = TestRunUtils.firstOntologyFile;
		String secondOntologyFile = TestRunUtils.secondOntologyFile;
		String referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
		String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
		double equalityThreshold = TestRunUtils.equalityThreshold;
		double fractionOfMappings = TestRunUtils.fractionOfMappings;
		String walksType = TestRunUtils.walksType;
		String modelPath = TestRunUtils.modelPath;
		String nameSpaceString = TestRunUtils.nameSpaceString1;
		String mergedOwlPath = TestRunUtils.mergedOwlPath;

		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
		BasicConfigurator.configure();

		// 1) Reading in the two ontologies
		OntologyReader reader = new OntologyReader();
		reader.setFname(firstOntologyFile);
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname(secondOntologyFile);
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		// 2) Merging the ontologies and writing it to disk
		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });
		OntologyReader.writeOntology(mergedOnto, mergedOwlPath, "owl");


		// For training of ontology start:
//		public TranslationMatrixCandidateFinder(OWLOntology o1, OWLOntology o2, String modelPath, double distLimit,
//				String nameSpace1) throws Exception {

		// 3) Creating the finder
		TranslationMatrixCandidateFinder finder = new TranslationMatrixCandidateFinder(onto1, onto2, modelPath,
				equalityThreshold, nameSpaceString);

		/* Adding anchors, by using word embeddings or by manually adding them */
//		finder.findAnchors(); /* this will use word embeddings to find anchors */

		// 4) Adding anchors by reading an alignments file */
		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, onto1, onto2);
//		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(
//				"/home/ole/master/logmap_standalone/output/logmap2_mappings.rdf", onto1, onto2);

		List<MappingObjectStr> mappings = alignmentsReader.getMappings();
		for (int i = 0; i < (mappings.size() * fractionOfMappings); i++) {
			MappingObjectStr mapping = mappings.get(i);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}

		// 5) Projecting the merged ontology
		OntologyProjector projector = new OntologyProjector(mergedOwlPath);
		projector.projectOntology();
		projector.saveModel(modelPath);

		Walks walks = new Walks(modelPath, walksType);
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
