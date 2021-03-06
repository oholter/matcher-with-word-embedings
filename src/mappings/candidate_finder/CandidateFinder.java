package mappings.candidate_finder;

import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import io.OAEIAlignmentOutput;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.StringUtils;
import mappings.utils.TestRunUtils;

public abstract class CandidateFinder {
	protected OWLOntology onto1;
	protected OWLOntology onto2;
	protected OWLOntology mappings;
	protected OWLDataFactory mappingsFactory;
	protected OWLOntologyManager mappingsManager;
	protected double distLimit;
	protected WordEmbeddingsTrainer trainer;
	protected String modelPath;
	protected OAEIAlignmentOutput output;

	public abstract void createMappings() throws Exception;

	public abstract void generateClassCandidates();

	public abstract void generateObjectProperties();

	public abstract void generateDataProperties();

	public OAEIAlignmentOutput getOutputAlignment() {
		return output;
	}

	public CandidateFinder(OWLOntology onto1, OWLOntology onto2, String modelPath, double distLimit) throws Exception {
		this.onto1 = onto1;
		this.onto2 = onto2;
		this.modelPath = modelPath;
		this.distLimit = distLimit;
		this.output = new OAEIAlignmentOutput("mappings", TestRunUtils.nameSpaceString1, TestRunUtils.nameSpaceString2);
	}

	public OWLOntology getOnto1() {
		return onto1;
	}

	public OWLOntology getOnto2() {
		return onto1;
	}

	public void setOnto1(OWLOntology onto1) {
		this.onto1 = onto1;
	}

	public void setOnto2(OWLOntology onto2) {
		this.onto2 = onto2;
	}

	public OWLOntology getMappings() {
		return mappings;
	}

	public String normalizeIRI(String s) {
		return StringUtils.normalizeIRI(s);
	}

	public String normalizeString(String s) {
		return StringUtils.normalizeString(s);
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



	protected double max(double a, double b, double c) {
		return Math.max(a, Math.max(b, c));
	}

}
