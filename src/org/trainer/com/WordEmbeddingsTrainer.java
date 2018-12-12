package org.trainer.com;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordEmbeddingsTrainer {
	private Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
	String inputFilePath;
	String outputFilePath;
	Word2Vec model;

	public WordEmbeddingsTrainer(String inputFile, String outputFile) throws Exception {
		inputFilePath = new File(inputFile).getAbsolutePath();
		outputFilePath = new File(outputFile).getAbsolutePath();
	}

	public void stripAccents() throws Exception {
		byte[] encoded = Files.readAllBytes(Paths.get(inputFilePath));
		String s = new String(encoded, Charset.defaultCharset());

		s = Normalizer.normalize(s, Normalizer.Form.NFD);
		s = s.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
		Files.write(Paths.get(inputFilePath), s.getBytes());
		System.out.println("Stripped file for accents");
	}

	public void train() throws Exception {

		log.info("Load & Vectorize Sentences....");
		// Strip white space before and after for each line
		SentenceIterator iter = new BasicLineIterator(inputFilePath);
		TokenizerFactory t = new DefaultTokenizerFactory();
		t.setTokenPreProcessor(new CommonPreprocessor());
		log.info("Building model....");
		model = new Word2Vec.Builder().minWordFrequency(5).iterations(1).layerSize(100).seed(42).windowSize(5)
				.iterate(iter).tokenizerFactory(t).build();
		log.info("Fitting w2v model");
		model.fit();

//		  log.info("Closest Words:");
//        Collection<String> lst = model.wordsNearest("mujer", 10);
//        System.out.println(lst);
//        log.info("cosine: ");
//        double coSine = model.similarity("hombre", "varon");
//        System.out.println(coSine);
		log.info("Saving model....");
		WordVectorSerializer.writeWord2VecModel(model, outputFilePath);
		log.info("Model saved");
	}

	public void loadModel() {
		model = WordVectorSerializer.readWord2VecModel(outputFilePath);
		log.info("Model loaded");
	}

	public String getInputFilePath() {
		return inputFilePath;
	}

	public void setInputFilePath(String inputFilePath) {
		this.inputFilePath = inputFilePath;
	}

	public String getOutputFilePath() {
		return outputFilePath;
	}

	public void setOutputFilePath(String outputFilePath) {
		this.outputFilePath = outputFilePath;
	}

	public Word2Vec getModel() {
		return model;
	}

	public void setModel(Word2Vec model) {
		this.model = model;
	}

	public double getCosine(String s1, String s2) {
		return model.similarity(s1, s2);
	}

	public double[] getAverageVector(String[] strings) {
		ArrayList<Double> sumVector = new ArrayList<>();
		int numberOfTokens = 0;

		for (int i = 0; i < strings.length; i++) {
			double[] vector = model.getWordVector(strings[i]);
			if (vector != null) {
				for (int j = 0; j < vector.length; j++) {
					if (j == sumVector.size()) {
						sumVector.add(vector[j]);
					} else {
						double newValue = vector[j] + sumVector.get(j);
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

	public double cosineSimilarity(double[] vectorA, double[] vectorB) {
		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		if (vectorA.length != 0 && vectorB.length != 0) {
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

	public double getAvgVectorCosine(String[] s1, String[] s2) {
		double[] vectorA = getAverageVector(s1);
		double[] vectorB = getAverageVector(s2);
		double cosine = cosineSimilarity(vectorA, vectorB);
		return cosine;
	}

}
