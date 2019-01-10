package org.structure_with_anchors.matcher.com;

import java.io.File;
import java.util.ArrayList;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.trainer.com.GradientDescent;
import org.trainer.com.Vector;
import org.trainer.com.WordEmbeddingsTrainer;

// TRY:

// 1 Get the difference between each of the values in the anchor sets
// 2 get the average vector of the differences 
// 3 predict the translation by adding the average difference between the sets

// OR:
// 1 Get the average of all vectors in the first ontology - representing the onto
// 2 Get the average of all vectors in the second ontology - representing the onto
// 3 ekaw#Paper - ekaw-onto-vetctor + cmt-onto-vector = cmt#Paper

// OR:
// 1 create a "translation matrix" with the anchors
// 2 use linear projection to create an estimate between the two "vector spaces"
// 3 use the function to "translate" between the vectors

import edit_distance.EditDistance;

public class CandidateFinder3 {
	OWLOntology onto1;
	OWLOntology onto2;
	OWLOntology mergedOnto;
	OWLOntology mappings;
	OWLDataFactory mappingsFactory;
	OWLOntologyManager mappingsManager;
	final double DIST_LIMIT = 0.4;
	final double ANCHOR_SIMILARITY = 0.8;
	final double MAX_ERROR = 1E-3;
	final int NUM_ITERATIONS = 150000;
	final double TRAINING_RATE = 0.2;

	WordEmbeddingsTrainer trainer;
	final String modelPath;
	final String currentDir;
	double[] anchorVector;
	ArrayList<ArrayList<Double>> anchorDifference;

	ArrayList<String> anchorsFromFirstOntology;
	ArrayList<String> anchorsFromSecondOntology;
	ArrayList<Double> differenceVectors;

	public CandidateFinder3(OWLOntology o1, OWLOntology o2, OWLOntology mergedOnto, String modelPath) throws Exception {
		onto1 = o1;
		onto2 = o2;
		mergedOnto = mergedOnto;
		this.modelPath = modelPath;
		currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
		trainer = new WordEmbeddingsTrainer("/home/ole/master/test_onto/merged.ttl", modelPath);
		trainer.loadModel();
		anchorsFromFirstOntology = new ArrayList<>();
		anchorsFromSecondOntology = new ArrayList<>();
	}

	public void findAnchors() {
		generateClassCandidates(true); // todo: must separate the class/object/data anchors
		generateObjectProperties(true);
		generateDataProperties(true);
		OWLDataFactory factory = mergedOnto.getOWLOntologyManager().getOWLDataFactory();
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();

		for (int i = 0; i < anchorsFromFirstOntology.size(); i++) {
			OWLEquivalentClassesAxiom eq = factory.getOWLEquivalentClassesAxiom(
					factory.getOWLClass(IRI.create(anchorsFromFirstOntology.get(i))),
					factory.getOWLClass(IRI.create(anchorsFromSecondOntology.get(i))));
			man.addAxiom(mergedOnto, eq);
		}
	}

	public void createMappings() {
		mappingsManager = OWLManager.createOWLOntologyManager();
		mappingsFactory = mappingsManager.getOWLDataFactory();

		try {
			mappings = mappingsManager.createOntology();
			createTranslationMatrix();

//			System.out.println("similarity between http://ekaw#Person and http://cmt#Person is: " + trainer
//					.cosineSimilarity(Vector.transform(translationMatrix, trainer.getWordVector("http://ekaw#Person")),
//							trainer.getWordVector("http://cmt#Person")));

			generateClassCandidates(false);
			generateObjectProperties(false);
			generateDataProperties(false);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		translationMatrix = gradientDescent.getMatrix();
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

	public OWLOntology getOnto1() {
		return onto1;
	}

	public void setOnto1(OWLOntology onto1) {
		this.onto1 = onto1;
	}

	public OWLOntology getOnto2() {
		return onto2;
	}

	public void setOnto2(OWLOntology onto2) {
		this.onto2 = onto2;
	}

	public OWLOntology getMappings() {
		return mappings;
	}

	public void setMappings(OWLOntology mappings) {
		this.mappings = mappings;
	}

	public String normalizeIRI(String s) {
		if (s != null) {
			s = s.replaceAll(String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])",
					"(?<=[A-Za-z])(?=[^A-Za-z])"), " "); // mixedCase -> multiple words
			s = s.toLowerCase(); // case normalization
			s = s.replaceAll("[_-]", " "); // link normalization
			s = s.replaceAll("[1-9.,]", ""); // remove numbers and punctuation
			s = s.replaceAll("\\s", " "); // blank normalization
			s = s.replaceAll("\\s+", " "); // only one blank
			s = s.trim();
		}
		return s;
	}

	public String normalizeString(String s) {
		if (s != null) {
			s = s.toLowerCase(); // case normalization
			s = s.replaceAll("[_-]", " "); // link normalization
			s = s.replaceAll("[.,]", ""); // remove punctuation
			s = s.replaceAll("\\s", " "); // blank normalization
			s = s.replaceAll("\\s+", " "); // only one blank
			s = s.trim();
		}
		return s;
	}

	public String findAnnotation(OWLNamedObject c, OWLOntology o, String type) {
		String label = null;
		String comment = null;
		for (OWLAnnotationAssertionAxiom annotationAssertionAxiom : o.getAnnotationAssertionAxioms(c.getIRI())) {
			if (annotationAssertionAxiom.getProperty().isLabel()) {
				if (annotationAssertionAxiom.getValue() instanceof OWLLiteral) {
					OWLLiteral literal = (OWLLiteral) annotationAssertionAxiom.getValue();
					label = literal.getLiteral();
				}
			}
			if (annotationAssertionAxiom.getProperty().isComment()) {
				if (annotationAssertionAxiom.getValue() instanceof OWLLiteral) {
					OWLLiteral literal = (OWLLiteral) annotationAssertionAxiom.getValue();
					comment = literal.getLiteral();
				}
			}

			if (type.equals("label")) {
				return label;
			} else if (type.equals("comment")) {
				return comment;
			} else {
				return null;
			}
		} // finished finding annotations for classFromFirstOntology

		return label;
	}

	private double max(double a, double b, double c) {
		return Math.max(a, Math.max(b, c));
	}

	private void generateClassCandidates(boolean anchors) {
		int numCandidates = 0;
		ArrayList<OWLClass> usedClassesFromSecondOntology = new ArrayList<>();

		for (OWLClass classFromFirstOntology : onto1.getClassesInSignature()) {
			double maxSimilarity = 0;
			OWLClass candidate = null;
			String iriFromFirstOntology = classFromFirstOntology.getIRI().toString();
			String labelFromFirstOntology = findAnnotation(classFromFirstOntology, onto1, "label");
			String commentFromFirstOntology = findAnnotation(classFromFirstOntology, onto1, "comment");

			for (OWLClass classFromSecondOntology : onto2.getClassesInSignature()) {
				if (usedClassesFromSecondOntology.contains(classFromSecondOntology)) {
					continue; // this class is already added
				}
				String iriFromSecondOntology = classFromSecondOntology.getIRI().toString();
				String labelFromSecondOntology = findAnnotation(classFromSecondOntology, onto2, "label");
				String commentFromSecondOntology = findAnnotation(classFromSecondOntology, onto2, "comment");

				double iriCosine = 0;
				if (!anchors) {
//					System.out.println("TESTING " + iriFromFirstOntology + " and " + iriFromSecondOntology);
					iriCosine = Vector.cosineSimilarity(
							Vector.transform(translationMatrix, trainer.getWordVector(iriFromFirstOntology)),
							trainer.getWordVector(iriFromSecondOntology));
//					System.out.println("TESTING " + iriFromFirstOntology + " and " + iriFromSecondOntology
//							+ " gives a similarity of: " + iriCosine);
					if (Double.isNaN(iriCosine)) {
						iriCosine = 0;
					}
				} else {
					String normalizedIriFromFirstOntology = normalizeIRI(classFromFirstOntology.getIRI().getFragment());
					String normalizedIriFromSecondOntology = normalizeIRI(
							classFromSecondOntology.getIRI().getFragment());
					int dist = EditDistance.editDistance(normalizedIriFromFirstOntology,
							normalizedIriFromSecondOntology, normalizedIriFromFirstOntology.length(),
							normalizedIriFromSecondOntology.length());
					iriCosine = 1 - (dist / 10.0);
					if (iriCosine < 0) {
						iriCosine = 0;
					}
				}

				double labelCosine = 0;
				if (!anchors) {
					if (labelFromFirstOntology != null && labelFromSecondOntology != null) {
						labelCosine = trainer.getCosine(labelFromFirstOntology, labelFromSecondOntology);
					}
				} else {
					if (labelFromFirstOntology != null && labelFromSecondOntology != null) {
						String normalizedLabelFromFirstOntology = normalizeString(labelFromFirstOntology);
						String normalizedLabelFromSecondOntology = normalizeString(labelFromSecondOntology);
						int dist = EditDistance.editDistance(normalizedLabelFromFirstOntology,
								normalizedLabelFromSecondOntology, normalizedLabelFromFirstOntology.length(),
								normalizedLabelFromSecondOntology.length());
						labelCosine = 1 - (dist / 10.0);
//						System.out.println("Label dist: " + dist);

					}
				}
				double commentCosine = 0;
				if (!anchors) {
					if (commentFromFirstOntology != null && commentFromSecondOntology != null) {
						commentCosine = trainer.getAvgVectorCosine(commentFromFirstOntology.split(" "),
								commentFromSecondOntology.split(" "));
					}
				} else {
					if (commentFromFirstOntology != null && commentFromSecondOntology != null) {
						String normalizedCommentFromFirstOntology = normalizeString(commentFromFirstOntology);
						String normalizedCommentFromSecondOntology = normalizeString(commentFromSecondOntology);
						int dist = EditDistance.editDistance(normalizedCommentFromFirstOntology,
								normalizedCommentFromSecondOntology, normalizedCommentFromFirstOntology.length(),
								normalizedCommentFromSecondOntology.length());
						labelCosine = 1 - (dist / 10.0);
//						System.out.println("Comment dist: " + dist);
					}
				}

				double currentSimilarity = max(iriCosine, labelCosine, commentCosine);

//				System.out.println("Testing: " + iriFromFirstOntology + " AND "
//						+ classFromSecondOntology.getIRI().toString() + " SIMILARITY: " + currentSimilarity);

				if (currentSimilarity > maxSimilarity) {
					maxSimilarity = currentSimilarity;
					candidate = classFromSecondOntology;
				}
			} // end classFromSecondOntology

			if (!anchors) {
				if (maxSimilarity > DIST_LIMIT) {
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

					System.out.println("Found mapping: " + equivalentClassAxiom);
					numCandidates++;
				}
			} // finished !anchors
			else if (maxSimilarity > ANCHOR_SIMILARITY) { // looking for anchors
				anchorsFromFirstOntology.add(iriFromFirstOntology);
				anchorsFromSecondOntology.add(candidate.getIRI().toString());
				System.out.println("Added anchor: " + iriFromFirstOntology + " AND " + candidate.getIRI().toString());
				usedClassesFromSecondOntology.add(candidate);
				numCandidates++;
			} // finished else
		} // finished classFromFirstOntology

		System.out.println("Found " + numCandidates + " class candidates:");
	} // finished generateClassCandidates()

	private void generateObjectProperties(boolean anchors) {
		int numCandidates = 0;
		ArrayList<String> usedPropertiesFromSecondOntology = new ArrayList<>();

		for (OWLObjectProperty propertyFromFirstOntology : onto1.getObjectPropertiesInSignature()) {
			String iriFromFirstOntology = propertyFromFirstOntology.getIRI().toString();
			String labelFromFirstOntology = normalizeString(findAnnotation(propertyFromFirstOntology, onto1, "label"));
			String commentFromFirstOntology = normalizeString(
					findAnnotation(propertyFromFirstOntology, onto1, "comment"));
			double maxSimilarity = 0;
			OWLObjectProperty candidate = null;

			for (OWLObjectProperty propertyFromSecondOntology : onto2.getObjectPropertiesInSignature()) {
				if (usedPropertiesFromSecondOntology.contains(propertyFromSecondOntology.getIRI().toString())) {
					continue; // already used
				}
				String iriFromSecondOntology = propertyFromSecondOntology.getIRI().toString();
				String labelFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "label"));
				String commentFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "comment"));

				double iriCosine = 0;
				if (!anchors) {
//					System.out.println("testing: " + iriFromFirstOntology + " and " + iriFromSecondOntology);
					iriCosine = Vector.cosineSimilarity(
							Vector.transform(translationMatrix, trainer.getWordVector(iriFromFirstOntology)),
							trainer.getWordVector(iriFromSecondOntology));
//					System.out.println("TESTING " + iriFromFirstOntology + " and " + iriFromSecondOntology
//							+ " gives a similarity of: " + iriCosine);
					if (Double.isNaN(iriCosine)) {
						iriCosine = 0;
					}
				} else {
					String normalizedIriFromFirstOntology = normalizeIRI(
							propertyFromFirstOntology.getIRI().getFragment());
					String normalizedIriFromSecondOntology = normalizeIRI(
							propertyFromSecondOntology.getIRI().getFragment());
					int dist = EditDistance.editDistance(normalizedIriFromFirstOntology,
							normalizedIriFromSecondOntology, normalizedIriFromFirstOntology.length(),
							normalizedIriFromSecondOntology.length());
					iriCosine = 1 - (dist / 10.0);
					if (iriCosine < 0) {
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
				}
			} // finished propertyFromSecondOntology
//			System.out.println("Object property max similarity: " + maxSimilarity);

			if (!anchors) {
				if (maxSimilarity > DIST_LIMIT) {
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

					System.out.println("Found mapping: " + equivalentClassAxiom);
					numCandidates++;
				}
			} // finished !anchors
			else if (maxSimilarity > ANCHOR_SIMILARITY) { // looking for anchors
				usedPropertiesFromSecondOntology.add(candidate.getIRI().toString());
				anchorsFromFirstOntology.add(iriFromFirstOntology);
				anchorsFromSecondOntology.add(candidate.getIRI().toString());
				System.out.println("Added anchor: " + iriFromFirstOntology + " AND " + candidate.getIRI().toString());
				numCandidates++;
			} // finished else
		} // finished propertyFromFirstOntology
		System.out.println("Found " + numCandidates + " object property candidates:");
	} // finished generateObjectProperties()

	private void generateDataProperties(boolean anchors) {
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
				if (usedPropertyFromSecondOntology.contains(propertyFromSecondOntology)) {
					continue;
				}
				String iriFromSecondOntology = propertyFromSecondOntology.getIRI().toString();
				String labelFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "label"));
				String commentFromSecondOntology = normalizeString(
						findAnnotation(propertyFromSecondOntology, onto2, "comment"));

				double iriCosine = 0;
				if (!anchors) {
//					System.out.println("testing: " + iriFromFirstOntology + " and " + iriFromSecondOntology);
					iriCosine = Vector.cosineSimilarity(
							Vector.transform(translationMatrix, trainer.getWordVector(iriFromFirstOntology)),
							trainer.getWordVector(iriFromSecondOntology));
//					System.out.println("TESTING " + iriFromFirstOntology + " and " + iriFromSecondOntology
//							+ " gives a similarity of: " + iriCosine);
					if (Double.isNaN(iriCosine)) {
						iriCosine = 0;
					}
				} else {
					String normalizedIriFromFirstOntology = normalizeIRI(
							propertyFromFirstOntology.getIRI().getFragment());
					String normalizedIriFromSecondOntology = normalizeIRI(
							propertyFromSecondOntology.getIRI().getFragment());
					int dist = EditDistance.editDistance(normalizedIriFromFirstOntology,
							normalizedIriFromSecondOntology, normalizedIriFromFirstOntology.length(),
							normalizedIriFromSecondOntology.length());
					iriCosine = 1 - (dist / 10.0);
					if (iriCosine < 0) {
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
				}
			} // finished propertyFromSecondOntology
//			System.out.println("Data property max similarity: " + maxSimilarity);
			if (!anchors) {
				if (maxSimilarity > DIST_LIMIT) {
					OWLEquivalentDataPropertiesAxiom equivalentClassAxiom = mappingsFactory
							.getOWLEquivalentDataPropertiesAxiom(propertyFromFirstOntology, candidate);
					mappingsManager.addAxiom(mappings, equivalentClassAxiom);

					OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
					OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
							confidenceLiteral);
					OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
							.getOWLAnnotationAssertionAxiom(propertyFromFirstOntology.getIRI(), annotation);

					mappingsManager.addAxiom(mappings, annotationAssertionAxiom);
					usedPropertyFromSecondOntology.add(candidate);

					System.out.println("Found mapping: " + equivalentClassAxiom);
					numCandidates++;
				}
			} // finished !anchors
			else if (maxSimilarity > ANCHOR_SIMILARITY) { // looking for anchors
				usedPropertyFromSecondOntology.add(candidate);
				anchorsFromFirstOntology.add(iriFromFirstOntology);
				anchorsFromSecondOntology.add(candidate.getIRI().toString());
				System.out.println("Added anchor: " + iriFromFirstOntology + " AND " + candidate.getIRI().toString());
				numCandidates++;
			} // finished else
		} // finished propertyFromFirstOntology
		System.out.println("Found " + numCandidates + " data property candidates:");
	}
}
