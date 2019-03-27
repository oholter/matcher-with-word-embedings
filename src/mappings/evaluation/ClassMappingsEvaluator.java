package mappings.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.semanticweb.owlapi.model.OWLOntology;

import io.OAEIAlignmentsReader;
import io.OntologyReader;
import mappings.utils.AlignmentUtilities;
import mappings.utils.TestRunUtils;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

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
		File alignmentFile = new File(alignmentFileName);
		if (alignmentFile.length() != 0) {
			this.alignmentReader = new OAEIAlignmentsReader(alignmentFileName, firstOntology, secondOntology);
			referenceAlignment = referenceAlignmentReader.getMappings();
			alignment = alignmentReader.getMappings();
			removePropertiesFromReference();
			removePropertiesFromAlignment();
		} else {
			System.out.println("No mappings found");
		}
	}

	public ArrayList<MappingObjectStr> removeAllProperties(List<MappingObjectStr> mappings) {
		ArrayList<MappingObjectStr> newAlignment = new ArrayList<>();
		for (MappingObjectStr mapping : mappings) {
			if (mapping.getTypeOfMapping() == AlignmentUtilities.CLASSES) {
				newAlignment.add(mapping);
			} else {
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
		reader.setFname(TestRunUtils.firstOntologyFile);
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname(TestRunUtils.secondOntologyFile);
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();
		ClassMappingsEvaluator evaluator = new ClassMappingsEvaluator(TestRunUtils.referenceAlignmentsFile,
				TestRunUtils.logMapAlignmentsFile, onto1, onto2);
		
		evaluator.printEvaluation();
	}

}
