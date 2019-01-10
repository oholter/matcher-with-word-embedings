package org.pretrained.matcher.com;

import java.util.ArrayList;

import org.semanticweb.owlapi.apibinding.OWLManager;
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
import org.trainer.com.WordEmbeddingsTrainer;


public class CandidateFinder {
	OWLOntology onto1;
	OWLOntology onto2;
	OWLOntology mappings;
	OWLDataFactory mappingsFactory;
	OWLOntologyManager mappingsManager;
	final double DIST_LIMIT = 0.8;
	WordEmbeddingsTrainer trainer;
	final String modelPath = "/home/ole/master/word2vec/models/fil9.model";

	public CandidateFinder(OWLOntology o1, OWLOntology o2) throws Exception {
		onto1 = o1;
		onto2 = o2;
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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

	private void generateClassCandidates() {
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

			if (maxSimilarity > DIST_LIMIT) {
				OWLEquivalentClassesAxiom equivalentClassAxiom = mappingsFactory
						.getOWLEquivalentClassesAxiom(classFromFirstOntology, candidate);
//				mappings.add(equivalentClassAxiom); owlapi5
				mappingsManager.addAxiom(mappings, equivalentClassAxiom);

				OWLLiteral confidenceLiteral = mappingsFactory.getOWLLiteral(maxSimilarity);
				OWLAnnotation annotation = mappingsFactory.getOWLAnnotation(mappingsFactory.getRDFSComment(),
						confidenceLiteral);
				OWLAnnotationAssertionAxiom annotationAssertionAxiom = mappingsFactory
						.getOWLAnnotationAssertionAxiom(classFromFirstOntology.getIRI(), annotation);

//				mappings.add(annotationAssertionAxiom);
				mappingsManager.addAxiom(mappings,  annotationAssertionAxiom);
				usedClassesFromSecondOntology.add(candidate);

//				System.out.println("Found mapping: " + equivalentClassAxiom);
				numCandidates++;
			}
		} // finished classFromFirstOntology

		System.out.println("Found " + numCandidates + " class candidates:");
	} // finished generateClassCandidates()

	private void generateObjectProperties() {
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

				double iriCosine = trainer.getAvgVectorCosine(iriFromFirstOntology.split(" "), iriFromSecondOntology.split(" "));
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

			if (maxSimilarity > DIST_LIMIT) {
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

	private void generateDataProperties() {
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

				double iriCosine = trainer.getAvgVectorCosine(iriFromFirstOntology.split(" "), iriFromSecondOntology.split(" "));
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
			if (maxSimilarity > DIST_LIMIT) {
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
}
