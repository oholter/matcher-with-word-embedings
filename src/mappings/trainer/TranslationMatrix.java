package mappings.trainer;

import java.io.IOException;

import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;

public class TranslationMatrix {

	WordEmbeddingsTrainer trainer;
	DoubleMatrix translationMatrix;
	int columns;

	public TranslationMatrix(WordEmbeddingsTrainer trainer, String[] sourceWords, String[] targetWords) throws IOException {
		super();
		this.trainer = trainer;
//		this.columns = model.getLayerSize();
		this.columns = trainer.layerSize;
		calculateTranslationMatrix(sourceWords, targetWords);
	}

	public DoubleMatrix createVectorMatrix(String[] words) {
		DoubleMatrix m = new DoubleMatrix(words.length, columns);
		for (int i = 0; i < words.length; i++) {
			try {
				m.putRow(i, new DoubleMatrix(trainer.getWordVector(words[i])));
			} catch (NullPointerException npe) {
				System.out.println("NOT IN DICT: " + words[i]);
//				System.exit(0); objectProperties will not be in dict, ignore them
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