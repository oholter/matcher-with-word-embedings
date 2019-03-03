package mappings.evaluation;

import java.util.ArrayList;
import java.util.List;

import io.RelatedConceptsReader;
import mappings.trainer.WordEmbeddingsTrainer;

public class EmbeddingsEvaluator {
	// * read file with clusters of related concepts in the ontology
	// * run the embeddings and see if they are indeed closely related
	// * test concepts that should not be related and see that they are NOT related

	WordEmbeddingsTrainer trainer;
	String conceptsFile;
	RelatedConceptsReader conceptsReader;
	double equalityThreshold;

	public EmbeddingsEvaluator(WordEmbeddingsTrainer trainer, String conceptsFile, double equalityThreshold) {
		this.trainer = trainer;
		this.conceptsFile = conceptsFile;
		this.conceptsReader = new RelatedConceptsReader(conceptsFile);
		conceptsReader.readConcepts();
		this.equalityThreshold = equalityThreshold;

	}

	public void evaluate() {
		evaluateSubClassConcepts();
		evaluatePropertyConcepts();
		evaluateUnrelatedConcepts();
	}

	public void evaluateSubClassConcepts() {
		List<List<String>> subClassConcepts = conceptsReader.getSubClassConcepts();
		double totAvg = 0;

		for (int i = 0; i < subClassConcepts.size(); i++) {
			List<String> concepts = subClassConcepts.get(i);
			List<String> concepts2 = subClassConcepts.get(i);
			List<String> usedC = new ArrayList<String>();
			String mainConcept = concepts.get(0);

			double sum = 0;
			int numComparisons = 0;
			for (String c : concepts) {
				usedC.add(c);
				for (String d : concepts2) {
					if (!usedC.contains(d)) {
						double cosine = trainer.getCosine(c, d);
						if (cosine < 0.5) {
							System.out.println("LOW: " + c + " : " + d + " : " + cosine);
						}
						if (cosine > 0.99) {
							System.out.println("HIGH: " + c + " : " + d + " : " + cosine);

						}
						if (Double.isNaN(cosine)) {
							System.out.println("ERROR: " + c + " : " + d + " : " + cosine);
						}
						numComparisons++;
						sum += cosine;
					} // finished if
				} // finished comparing d
			} // end c
			double avg = sum / numComparisons;
			System.out.println("AVG " + mainConcept + " : " + avg);
			totAvg += avg;
		}
		
		totAvg /= subClassConcepts.size();
		System.out.println("TOTAL: " + totAvg);
	}
	
	public void evaluatePropertyConcepts() {
		List<List<String>> propertyConcepts = conceptsReader.getPropertyConcepts();

		for (int i = 0; i < propertyConcepts.size()/2; i++) {
			List<String> concepts = propertyConcepts.get(i);
			List<String> concepts2 = propertyConcepts.get(i);
			String mainConcept = concepts.get(0);
			List<String> usedC = new ArrayList<String>();

			double sum = 0;
			int numComparisons = 0;
			for (String c : concepts) {
				usedC.add(c);
				for (String d : concepts2) {
					if (!usedC.contains(d)) {
						double cosine = trainer.getCosine(c, d);
						if (cosine < 0.5) {
							System.out.println("LOW: " + c + " : " + d + " : " + cosine);
						}
						if (Double.isNaN(cosine)) {
							System.out.println("ERROR: " + c + " : " + d + " : " + cosine);
						}
						numComparisons++;
						sum += cosine;
					} // finished if
				} // finished comparing d
			} // end c
			double avg = sum / numComparisons;
			System.out.println("AVG prop: " + mainConcept + " : " + avg);
		}
	}
	
	public void evaluateUnrelatedConcepts() {
		List<List<String>> unrelatedConcepts = conceptsReader.getUnrelatedConcepts();

		for (int i = 0; i < unrelatedConcepts.size(); i++) {
			List<String> concepts = unrelatedConcepts.get(i);
			List<String> concepts2 = unrelatedConcepts.get(i);
			String mainConcept = concepts.get(0);
			List<String> usedC = new ArrayList<String>();

			double sum = 0;
			int numComparisons = 0;
			for (String c : concepts) {
				usedC.add(c);
				for (String d : concepts2) {
					if (!usedC.contains(d)) {
						double cosine = trainer.getCosine(c, d);
						if (cosine > 0.5) {
							System.out.println("HIGH: " + c + " : " + d + " : " + cosine);
						}
						if (Double.isNaN(cosine)) {
							System.out.println("ERROR: " + c + " : " + d + " : " + cosine);
						}
						numComparisons++;
						sum += cosine;
					} // finished if
				} // finished comparing d
			} // end c
			double avg = sum / numComparisons;
			System.out.println("AVG UNRELATED: " + mainConcept + " : " + avg);
		}
	}

}
