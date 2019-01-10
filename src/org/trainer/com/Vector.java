package org.trainer.com;

import java.util.ArrayList;

public class Vector {
	public static double vectorLength(double[] vec) {
		double len = 0;
		for (int i = 0; i < vec.length; i++) {
			len += (vec[i] * vec[i]);
		}
		return Math.sqrt(len);
	}

	/**
	 * 
	 * d^2(p,q) = (p1 - q1)^2 + (p2 - q2)^2 + ... (pn - qn)^2
	 * 
	 * @param vec1
	 * @param vec2
	 * @return
	 */
	public static double squaredEucledianDistance(double[] vec1, double[] vec2) {
		if (vec1.length == vec2.length) {
			double sum = 0;
			for (int i = 0; i < vec1.length; i++) {
				sum += Math.pow(vec1[i] - vec2[i], 2);
			}
			return sum;
		} else
			return -1;
	}

	public static double cosineSimilarity(double[] vectorA, double[] vectorB) {
		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		if (vectorA != null && vectorA.length != 0 && vectorB != null && vectorB.length != 0) {
			for (int i = 0; i < vectorA.length; i++) {
				dotProduct += vectorA[i] * vectorB[i];
				normA += Math.pow(vectorA[i], 2);
				normB += Math.pow(vectorB[i], 2);
			}
			return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
		} else {
			return 0;
		}
	}

	public double[] getAverageVectorFromDoubles(double[][] vectors) {
		ArrayList<ArrayList<Double>> vectorList = new ArrayList<>();
		for (int i = 0; i < vectors.length; i++) {
			ArrayList<Double> tmp = new ArrayList<>();
			vectorList.add(tmp);
			for (int j = 0; j < vectors[i].length; j++) {
				tmp.add(vectors[i][j]);
			}

		}
		return getAverageVectorFromDoubles(vectorList);
	}

	public static double[] getAverageVectorFromDoubles(ArrayList<ArrayList<Double>> vectors) {
		ArrayList<Double> sumVector = new ArrayList<>();
		int numberOfTokens = 0;

		for (int i = 0; i < vectors.size(); i++) {
			ArrayList<Double> vector = vectors.get(i);
			if (vector != null) {
				for (int j = 0; j < vector.size(); j++) {
					if (j == sumVector.size()) {
						sumVector.add(vector.get(j));
					} else {
						double newValue = vector.get(j) + sumVector.get(j);
						sumVector.set(j, newValue);
					}
				}
			}
			numberOfTokens++;
		}
		double[] avgVector = new double[sumVector.size()];
		for (int i = 0; i < sumVector.size(); i++) {
			avgVector[i] = sumVector.get(i) / numberOfTokens;
		}
		return avgVector;
	}

	public static double[] addTwoVectors(double[] vec1, double[] vec2) {
		double[] res = new double[Math.max(vec1.length, vec2.length)];
		int i = 0;
		while (i < vec1.length) {
			if (i < vec2.length) {
				res[i] = vec1[i] + vec2[i];
			} else {
				res[i] = vec1[i];
			}
			i++;
		}

		while (i < vec2.length) { // if vec2 is longer than vec1
			res[i] = vec2[i];
			i++;
		}

		return res;
	}

	public static double[] subtractTwoVectors(double[] vec1, double[] vec2) {
		double[] res = new double[Math.max(vec1.length, vec2.length)];
		int i = 0;
		while (i < vec1.length) {
			if (i < vec2.length) {
				res[i] = vec1[i] - vec2[i];
			} else {
				res[i] = vec1[i];
			}
			i++;
		}

		while (i < vec2.length) { // if vec2 is longer than vec1
			res[i] = 0 - vec2[i];
			i++;
		}

		return res;
	}

	public static double[] transform(double[][] transformationMatrix, double[] vector) {
		if (vector != null) {
			double[] transformedVector = new double[vector.length];

			for (int i = 0; i < transformationMatrix.length; i++) {
				for (int j = 0; j < transformationMatrix[0].length; j++) {
					transformedVector[i] += transformationMatrix[i][j] * vector[j];
				}
			}
			return transformedVector;
		} else
			return null;
	}
}
