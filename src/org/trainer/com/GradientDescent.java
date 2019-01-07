package org.trainer.com;

public class GradientDescent {

	// meta parameters
	double learningRate;
	int numberOfIterations;

	// the vectors
	double[][] xs;
	double[][] zs;

	int N; // number of samples

	// the translation matrix
	// this must be a N x N matrix
	double[][] w;

	public GradientDescent(double[][] xs, double[][] zs, double learningRate, int numberOfIterations) {
		this.xs = xs;
		this.zs = zs;
		this.learningRate = learningRate;
		this.numberOfIterations = numberOfIterations;
		this.N = xs.length;
		this.w = new double[N][N];
		
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				w[i][j] = 1;
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

			double[] wx = Vector.transform(w, x);
			double dist = Vector.squaredEucledianDistance(wx, z);
			totError += dist;
		}

		return totError / (double) N;
	}

	/**
	 * solving for each value in w for each value x in w the gradient is: dx/dw Sum
	 * || W * x_i - z_i || ^2 => dx/dw = 2 * Sum w * (w * x1 + b * x_2 ... r*x_r -
	 * z_i)
	 */
	public void stepGradient() {
		double[][] gradients = new double[N][N];
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				double ij = w[i][j];
				double tmp = 0;
				for (int k = 0; k < N; k++) {
					tmp += w[i][k];
				}
				gradients[i][j] = ij * tmp;
				gradients[i][j] *= 2;
			}
		}
		
//		System.out.println("gradients");
//		printMatrix(gradients);

		// apply the gradients
		for (int i = 0; i < N; i++) {
			for (int j = 0; j < N; j++) {
				w[i][j] = w[i][j] - (learningRate * gradients[i][j]);
			}
		}
	}

	public void solve() {
		for (int i = 0; i < numberOfIterations; i++) {
			stepGradient();
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
		double[] vectorA1 = { 2, 3 };
		double[] vectorA2 = { 1, -2 };
		double[] vectorB1 = { 3, 4 };
		double[] vectorB2 = { 5, -6 };

		double[][] vectorAs = { vectorA1, vectorA2 };
		double[][] vectorBs = { vectorB1, vectorB2 };

		GradientDescent gd = new GradientDescent(vectorAs, vectorBs, 0.0001, 1000);
		gd.printMatrix();
	
		System.out.println("Solving matrix");
		gd.solve();
		gd.printMatrix();
		System.out.println("Error: " + gd.calculateError());
	}
}
