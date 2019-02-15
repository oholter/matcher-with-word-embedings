package mappings.evaluation;

import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLOntology;
import org.trainer.com.OntologyReader;

import io.OAEIAlignmentsReader;
import io.Utilities;
import mapping.object.MappingObjectStr;

/**
 * this mappings evaluator does only consider class alignment when calculating
 * the evaluation metrics, this is done by first removing all properties from
 * the original mappings
 * 
 * @author ole
 *
 */
public class ClassMappingsEvaluator extends MappingsEvaluator {

	public ClassMappingsEvaluator(String referenceAlignmentFileName, String alignmentFileName,
			OWLOntology firstOntology, OWLOntology secondOntology) {
		super();
		this.referenceAlignmentFileName = referenceAlignmentFileName;
		this.alignmentFileName = alignmentFileName;
		this.referenceAlignmentReader = new OAEIAlignmentsReader(referenceAlignmentFileName, firstOntology,
				secondOntology);
		this.alignmentReader = new OAEIAlignmentsReader(alignmentFileName, firstOntology, secondOntology);
		referenceAlignment = referenceAlignmentReader.getMappings();
		alignment = alignmentReader.getMappings();
		removePropertiesFromReference();
		removePropertiesFromAlignment();
	}

	public ArrayList<MappingObjectStr> removeAllProperties(List<MappingObjectStr> mappings) {
		ArrayList<MappingObjectStr> newAlignment = new ArrayList<>();
		for (MappingObjectStr mapping : mappings) {
			if (mapping.getTypeOfMapping() == Utilities.CLASSES) {
				newAlignment.add(mapping);
			}
			else {
//				System.out.println("type of mapping: " + mapping.getTypeOfMapping());
			}
		}
		return newAlignment;
	}

	public void removePropertiesFromAlignment() {
		alignment = removeAllProperties(alignment);
		System.out.println("Alignment size after removing properties: " + alignment.size());
	}

	public void removePropertiesFromReference() {
		referenceAlignment = removeAllProperties(referenceAlignment);
		System.out.println("Reference alignment size after removing properties: " + referenceAlignment.size());
	}

	public static void main(String[] args) throws Exception {
		OntologyReader reader = new OntologyReader();
		reader.setFname("/home/ole/master/test_onto/cmt.owl");
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader = new OntologyReader();
		reader.setFname("/home/ole/master/test_onto/ekaw.owl");
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		MappingsEvaluator evaluator = new ClassMappingsEvaluator(
				"/home/ole/master/test_onto/reference_alignments/cmt-ekaw.rdf", "/tmp/mappings8686184173808360372.rdf",
				onto1, onto2);
		System.out.println("Precision: " + evaluator.calculatePrecision());
		System.out.println("Recall: " + evaluator.calculateRecall());
		System.out.println("F-measure " + evaluator.calculateFMeasure());
	}

}
