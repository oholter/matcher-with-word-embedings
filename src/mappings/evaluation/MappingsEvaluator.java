package mappings.evaluation;

import java.util.ArrayList;
import java.util.List;

import io.AlignmentsReader;
import io.OAEIAlignmentsReader;
import mapping.object.MappingObjectStr;

public class MappingsEvaluator {

	AlignmentsReader referenceAlignmentReader;
	AlignmentsReader alignmentReader;
	String referenceAlignmentFileName;
	String alignmentFileName;
	List<MappingObjectStr> referenceAlignment;
	List<MappingObjectStr> alignment;
	List<MappingObjectStr> intersection;
	double ALPHA = 0.5; // to calculate F-Measure

	public MappingsEvaluator(String referenceAlignmentFileName, String alignmentFileName) {
		this.referenceAlignmentFileName = referenceAlignmentFileName;
		this.alignmentFileName = alignmentFileName;
		referenceAlignmentReader = new OAEIAlignmentsReader(referenceAlignmentFileName);
		alignmentReader = new OAEIAlignmentsReader(alignmentFileName);
		referenceAlignment = referenceAlignmentReader.getMappings();
		alignment = alignmentReader.getMappings();
	}
	
	public MappingsEvaluator() {}

	public static List<MappingObjectStr> intersectAlignments(List<MappingObjectStr> referenceAlignment,
			List<MappingObjectStr> alignment) {
		ArrayList<MappingObjectStr> theIntersection = new ArrayList<>();

		for (MappingObjectStr alignmentMapping : alignment) {
			for (MappingObjectStr referenceMapping : referenceAlignment) {
				if (alignmentMapping.equals(referenceMapping)) {
					theIntersection.add(alignmentMapping);
				}
			}
		}
//		theIntersection.forEach(System.out::println);
		return theIntersection;
	}

	/**
	 * P(A,R) = |(R intersect A)| / |A|
	 * 
	 * @return
	 */
	public double calculatePrecision() {
		if (alignment.size() == 0) {
			return 0;
		}
		if (intersection == null) { // lazy
			intersection = MappingsEvaluator.intersectAlignments(referenceAlignment, alignment);
		}
		return intersection.size() / (double) alignment.size();
	}

	/**
	 * R(A, R) = |(R intersect A)| / |R|
	 * 
	 * @return
	 */
	public double calculateRecall() {
		if (referenceAlignment.size() == 0) {
			return 0;
		}
		if (intersection == null) {
			intersection = MappingsEvaluator.intersectAlignments(referenceAlignment, alignment);
		}
		return intersection.size() / (double) referenceAlignment.size();
	}

	/**
	 * P(A, R) * R(A, R) / (1 - a) * P(A, R) + a * R(A,R) 
	 * @return
	 */
	public double calculateFMeasure() {
		double recall = calculateRecall();
		double precision = calculatePrecision();
		double denominator = (1 - ALPHA) * precision + ALPHA * recall;
		if (denominator == 0) {
			return 0;
		}
		double nominator = precision * recall;
		return nominator / denominator;
	}

	public static void main(String[] args) throws Exception {
		MappingsEvaluator evaluator = new MappingsEvaluator(
				"/home/ole/master/test_onto/reference_alignments/cmt-ekaw.rdf", "/tmp/mappings8686184173808360372.rdf");
		System.out.println("Precision: " + evaluator.calculatePrecision());
		System.out.println("Recall: " + evaluator.calculateRecall());
		System.out.println("F-measure " + evaluator.calculateFMeasure());
	}

}
