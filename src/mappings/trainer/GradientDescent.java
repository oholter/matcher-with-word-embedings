package mappings.trainer;

import java.util.Random;

import mappings.utils.VectorUtils;

public class GradientDescent {

	// meta parameters
	double learningRate;
	int numberOfIterations;
	double maxError;

	// the vectors
	double[][] xs;
	double[][] zs;

	int N; // number of samples

	// the translation matrix
	// this must be a N x N matrix
	double[][] w;

	public GradientDescent(double[][] xs, double[][] zs, double learningRate, int numberOfIterations, double maxError) {
		this.xs = xs;
		this.zs = zs;
		this.learningRate = learningRate;
		this.numberOfIterations = numberOfIterations;
		this.N = xs[0].length;
		this.maxError = maxError;
		this.w = new double[N][N];

		Random rand = new Random();
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				w[i][j] = rand.nextDouble();
			}
		}
	}

	/**
	 * Sum ||W * x_i - z_i||^2
	 * 
	 * @return
	 */
	public double calculateError() {
		double totError = 0;

		for (int i = 0; i < xs.length; i++) {
			double[] x = xs[i];
			double[] z = zs[i];

			double[] wx = VectorUtils.transform(w, x);
			double dist = VectorUtils.squaredEucledianDistance(wx, z);
			totError += dist;
		}

		return totError / (double) N;
	}

//	/**
//	 * solving for each value in w for each value x in w the gradient is: dx/dw Sum
//	 * || W * x_i - z_i || ^2 => Sum (2 * ||Wx_i - z_i||) * (dx/da Wx_i-z_i)
//	 * dx/da Wx_i - z_i = dx/da [ 1, 0 ] [ 0, 0 ] * [ x1, x2 ] - [z_1, z_2] 
//	 */
//	public void stepGradient() {
//		double[][] gradients = new double[N][N];
//		for (int i = 0; i < N; i++) {
//			for (int j = 0; j < N; j++) {
//				double ij = w[i][j];
//				double[] vectorX = xs[i];
//				double[] vectorZ = zs[i];
//				double[][] valueMatrix = new double[N][N];
//				valueMatrix[i][j] = 1;
//						
//				double[] vec = Vector.transform(valueMatrix, xs[i]);
//				vec = Vector.subtractTwoVectors(vec, zs[i]);
//				
//				double gradient = Vector.vectorLength(Vector.subtractTwoVectors(Vector.transform(w, vectorX), vectorZ));
////				System.out.println(gradient);
//				gradient *= Vector.vectorLength(vec);
//				gradients[i][j] = gradient;
//			}
//		}

//		System.out.println("gradients");
//		printMatrix(gradients);

//		// apply the gradients
//		for (int i = 0; i < N; i++) {
//			for (int j = 0; j < N; j++) {
//				w[i][j] = w[i][j] - (learningRate * gradients[i][j]);
//			}
//		}
//	}

	public void stepGradient(int step) {
		while (step >= N * N) {
			step -= N * N;
		}
		int row = step / N;
//		System.out.println("step: " + step);
		int col = step % N;

		double currentError = calculateError();
		w[row][col] -= learningRate;
		double errorDiff = currentError - calculateError();
		if (errorDiff > 0) {
			w[row][col] += 2 * (learningRate * errorDiff);
		} else {
			w[row][col] -= ((learningRate * errorDiff) - learningRate);
		}
	}

	public void solve() {
		for (int i = 0; i < numberOfIterations; i++) {
			stepGradient(i);
			double error = calculateError();
			if (i % 1000 == 0) {
				System.out.println("Error: " + error);
			}
			if (error < maxError) {
				break;
			}
		}
	}

	public double[][] getMatrix() {
		return w;
	}

	public void printMatrix() {
		printMatrix(w);
	}

	public void printMatrix(double[][] matrix) {
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				System.out.print(matrix[i][j]);
				if (j != N - 1) {
					System.out.print(", ");
				}
				if (j == N - 1) {
					System.out.println();
				}
			}
		}
	}

	public static void main(String[] args) {
		double[] vectorA1 = { 2, 3, 4, 5 };
		double[] vectorA2 = { 1, -2, -3, -4 };
		double[] vectorB1 = { 3, 4, 5, 6 };
		double[] vectorB2 = { 5, -6, 7, -8 };

		double[][] vectorAs = { vectorA1, vectorA2 };
		double[][] vectorBs = { vectorB1, vectorB2 };

		GradientDescent gd = new GradientDescent(vectorAs, vectorBs, 0.001, 100000, 1E-3);
		gd.printMatrix();

		System.out.println("Solving matrix");
		gd.solve();
		gd.printMatrix();
		System.out.println("Error: " + gd.calculateError());

		System.out.println("Calculating vectorB1: ");
		double[] vectorB1Calc = VectorUtils.transform(gd.getMatrix(), vectorA1);
		System.out.println(vectorB1Calc[0]);
		System.out.println(vectorB1Calc[1]);
		System.out.println(vectorB1Calc[2]);
		System.out.println(vectorB1Calc[3]);

		System.out.println("Calculating vectorB1: ");
		double[] vectorB2Calc = VectorUtils.transform(gd.getMatrix(), vectorA2);
		System.out.println(vectorB2Calc[0]);
		System.out.println(vectorB2Calc[1]);
		System.out.println(vectorB2Calc[2]);
		System.out.println(vectorB2Calc[3]);

	}
}
