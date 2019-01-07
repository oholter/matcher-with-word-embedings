package org.trainertest.com;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trainer.com.WordEmbeddingsTrainer;

public class trainerTest {
	// public double[] sumVectors(Collection<String> words)
	// public double[] subtractTwoVectors(double[] vec1, double[] vec2)
	// public double[] addTwoVectors(double[] vec1, double[] vec2)
	// public double[] getAverageVector(Collection<String> strings)
	// public double[] getAverageVectorFromDoubles(ArrayList<ArrayList<Double>>
	// vectors)
	// public double[] getAverageVector(String[] strings)
	// public double cosineSimilarity(double[] vectorA, double[] vectorB)
	// public String getBestAssociation(Collection<String> firstAnchors,
	// Collection<String> secondAnchors, String word)
	// public ArrayList<ArrayList<Double>>
	// calculateDifferenceVectors(ArrayList<String> anchorsFromFirstOntology,
	// ArrayList<String> anchorsFromSecondOntology)
	// public double getAvgVectorCosine(String[] s1, String[] s2)

	double[] vec1;
	double[] vec2;
	WordEmbeddingsTrainer trainer;
	Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);

	@BeforeEach
	public void setup() throws Exception {
		vec1 = new double[5];
		vec1[0] = 0.0;
		vec1[1] = 0.5;
		vec1[2] = 0.8;
		vec1[3] = 4.3;
		vec1[4] = 3.3;

		vec2 = new double[5];
		vec2[0] = 9.3;
		vec2[1] = 8.3;
		vec2[2] = 7.1;
		vec2[3] = 7.8;
		vec2[4] = 6.0;
//		String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
		String currentDir = "/home/ole/workspace/MatcherWithWordEmbeddings/target/classes";

		/**
		 * Must first create the word embedding file
		 */

		trainer = new WordEmbeddingsTrainer("/home/ole/master/test_onto/merged.ttl", currentDir + "/temp/out.txt");
//		trainer.stripAccents();
//		trainer.train();
		trainer.loadModel();
	}

	@Test
	public void shouldAddTwoVectors() {
		double[] addedVector = trainer.addTwoVectors(vec1, vec2);
		for (int i = 0; i < vec1.length; i++) {
			assertEquals(addedVector[i], vec1[i] + vec2[i]);
		}
	}

	@Test
	public void shouldSubtractTwoVectors() {
		double[] subtractedVector = trainer.subtractTwoVectors(vec1, vec2);
		for (int i = 0; i < vec1.length; i++) {
			assertEquals(subtractedVector[i], vec1[i] - vec2[i]);
		}
	}

	@Test
	public void shouldGetAverageVectorFromDoubles() {
		ArrayList<ArrayList<Double>> vectors = new ArrayList<>();

		vectors.add(new ArrayList<>());
		vectors.add(new ArrayList<>());

		for (int i = 0; i < vec1.length; i++) {
			vectors.get(0).add(vec1[i]);
			vectors.get(1).add(vec2[i]);
		}
		
		double[] averageVector = trainer.getAverageVectorFromDoubles(vectors);

		double[] realAverages = { 4.65, 4.4, 3.95, 6.05, 4.65 };
		
		for (int i = 0; i < vec1.length; i++) {
			System.out.println("vectors.0: " + vectors.get(0).get(i));
			System.out.println("vectors.1: " + vectors.get(1).get(i));
			System.out.println(realAverages[i] + ": " + averageVector[i]);
			assertEquals(realAverages[i], averageVector[i], 0.0001);
		}
	}

	@Test
	public void shouldGetAverageVectorFromStringArray() {
		String[] words = {"http://ekaw#Person", "http://cmt#Person", "http://ekaw#Track"};
		double[] averageVectors = trainer.getAverageVector(words);
		
		ArrayList<ArrayList<Double>> vectors = new ArrayList<>();
		double[] ekawPersonVector = trainer.getModel().getWordVector("http://ekaw#Person");
		double[] cmtPersonVector = trainer.getModel().getWordVector("http://cmt#Person");
		double[] ekawTrackVector = trainer.getModel().getWordVector("http://ekaw#Track");
		
		vectors.add(trainer.doubleArray2ArrayList(ekawPersonVector));
		vectors.add(trainer.doubleArray2ArrayList(cmtPersonVector));
		vectors.add(trainer.doubleArray2ArrayList(ekawTrackVector));
		
		double[] firstSum = trainer.addTwoVectors(ekawPersonVector, cmtPersonVector);
		double[] secondSum = trainer.addTwoVectors(firstSum, ekawTrackVector);
		double[] realAverages = new double[secondSum.length];
		for (int i = 0; i < realAverages.length; i++) {
			realAverages[i] = secondSum[i] / 3;
		}
		
		for (int i = 0; i < realAverages.length; i++ ) {
//			System.out.println("ekawPersonVector[" + i + "]: " + ekawPersonVector[i]);
//			System.out.println("cmtPersonVector[" + i + "]: " + cmtPersonVector[i]);
//			System.out.println("ekawTrackVector[" + i + "]: " + ekawTrackVector[i]);
//			System.out.println("realAverages[" + i + "]: " + realAverages[i]);
//			System.out.println("averageVectors[" + i + "]: " + averageVectors[i]);
			assertEquals(realAverages[i], averageVectors[i], 0.0001);
		}
	}
	
	@Test
	public void shouldGetAverageVectorFromCollectionOfStrings() {
		ArrayList<String> words = new ArrayList<>();
		words.add("http://ekaw#Person");
		words.add("http://cmt#Person");
		words.add("http://ekaw#Track");
		double[] averageVectors = trainer.getAverageVector(words);
		
		ArrayList<ArrayList<Double>> vectors = new ArrayList<>();
		double[] ekawPersonVector = trainer.getModel().getWordVector("http://ekaw#Person");
		double[] cmtPersonVector = trainer.getModel().getWordVector("http://cmt#Person");
		double[] ekawTrackVector = trainer.getModel().getWordVector("http://ekaw#Track");
		
		vectors.add(trainer.doubleArray2ArrayList(ekawPersonVector));
		vectors.add(trainer.doubleArray2ArrayList(cmtPersonVector));
		vectors.add(trainer.doubleArray2ArrayList(ekawTrackVector));
		
		double[] firstSum = trainer.addTwoVectors(ekawPersonVector, cmtPersonVector);
		double[] secondSum = trainer.addTwoVectors(firstSum, ekawTrackVector);
		double[] realAverages = new double[secondSum.length];
		for (int i = 0; i < realAverages.length; i++) {
			realAverages[i] = secondSum[i] / 3;
		}
		
		for (int i = 0; i < realAverages.length; i++ ) {
			assertEquals(realAverages[i], averageVectors[i], 0.0001);
		}
	}
	
	@Test
	public void cosineSimilarityShouldBe1ForVectorsWithSameLengthAndDirection() {
		double[] ekawPersonVector = trainer.getModel().getWordVector("http://ekaw#Person");
		double cosine = trainer.cosineSimilarity(ekawPersonVector, ekawPersonVector);
		assertEquals(1, cosine);
	}
	
	@Test
	public void cosineSimilarityShouldBeNegativeOneForOppositeVectors() {
		double[] firstVector = {0, 5};
		double[] secondVector = {0, -5};
		double cosine = trainer.cosineSimilarity(firstVector, secondVector);
		assertEquals(-1, cosine);
	}
	
	@Test
	public void cosineSimilarityShouldBeZeroForOrthogonalVectors() {
		double[] firstVector = {0, 5};
		double[] secondVector = {5, 0};
		double cosine = trainer.cosineSimilarity(firstVector, secondVector);
		assertEquals(0, cosine);
	}
	
	@Test
	public void shouldReturnCorrectLength() {
		double[] firstVector = {0, 5};
		double[] secondVector = {-5, 0};
		double[] thirdVector = {4, -6};
		
		double len1 = trainer.vectorLength(firstVector);
		assertEquals(5.0, len1, 0.0001);
		double len2 = trainer.vectorLength(secondVector);
		assertEquals(5.0, len2, 0.0001);
		double len3 = trainer.vectorLength(thirdVector);
		assertEquals(7.2111, len3, 0.001);
	}
}
