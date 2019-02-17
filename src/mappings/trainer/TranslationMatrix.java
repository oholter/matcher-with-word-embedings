package mappings.trainer;

import java.io.IOException;
import java.util.ArrayList;

import org.deeplearning4j.models.word2vec.Word2Vec;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;

public class TranslationMatrix {

	Word2Vec model;
	DoubleMatrix translationMatrix;
	int columns;

	public TranslationMatrix(Word2Vec model, String[] sourceWords, String[] targetWords) throws IOException {
		super();
		this.model = model;
		this.columns = model.getLayerSize();
		calculateTranslationMatrix(sourceWords, targetWords);
	}

	public DoubleMatrix createVectorMatrix(String[] words) {
		DoubleMatrix m = new DoubleMatrix(words.length, columns);
		for (int i = 0; i < words.length; i++) {
			try {
				m.putRow(i, new DoubleMatrix(model.getWordVector(words[i])));
			} catch (NullPointerException npe) {
				System.out.println("NOT IN DICT: " + words[i]);
				System.exit(0);
			}
		}
		return m;
	}

	public void calculateTranslationMatrix(String[] sourceWords, String[] targetWords) throws IOException {
		DoubleMatrix matrixTrainSource = createVectorMatrix(sourceWords);
		DoubleMatrix matrixTrainTarget = createVectorMatrix(targetWords);
		DoubleMatrix pinverseMatrix = Solve.pinv(matrixTrainSource);
		DoubleMatrix translationMatrix = pinverseMatrix.mmul(matrixTrainTarget).transpose();

		this.translationMatrix = translationMatrix;
	}

	public double[] estimateTarget(double[] sourceVector) {
		if (sourceVector == null) {
			return null;
		}
		DoubleMatrix src = new DoubleMatrix(sourceVector);
		return translationMatrix.mmul(src).transpose().toArray();
	}
}