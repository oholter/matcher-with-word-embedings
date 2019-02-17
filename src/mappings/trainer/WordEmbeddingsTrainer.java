		package mappings.trainer;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.neo4j.cypher.internal.compiler.v2_3.docgen.plannerDocGen.predicateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WordEmbeddingsTrainer {
	private Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
	String inputFilePath;
	String outputFilePath;
	Word2Vec model;
	int numEpocs = 1;
	int windowSize = 15;
	int numIterations = 1;
	int layerSize = 100;
	int minWordFrequency = 5;
	int seed = 42; // 42

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

		System.out.println("Starting training: " + inputFilePath);
		System.out.println("Load & Vectorize Sentences....");
		// Strip white space before and after for each line
		SentenceIterator iter = new BasicLineIterator(inputFilePath);
		TokenizerFactory t = new DefaultTokenizerFactory();
//		t.setTokenPreProcessor(new CommonPreprocessor());
		log.info("Building model....");
		model = new Word2Vec.Builder().minWordFrequency(minWordFrequency).iterations(numIterations).layerSize(layerSize).seed(seed).windowSize(windowSize)
				.iterate(iter).tokenizerFactory(t).build();
		log.info("Fitting w2v model");
		System.out.println("fitting model");
		for (int i = 0; i < numEpocs; i++) {
			log.info("EPoch: " + i);
			model.fit();
		}
		
		System.out.println("Closest Words:");
		Collection<String> lst = model.wordsNearest("http://cmt#Conference", 10);
		System.out.println(lst);
		System.out.println("cosine: ");
		double coSine = model.similarity("http://cmt#Conference", "http://ekaw#Conference");
		System.out.println(coSine);
		System.out.println("Saving model....");
		File outFile = new File(outputFilePath);
		if (!outFile.getParentFile().exists()) {
			outFile.getParentFile().mkdirs();
		}
		WordVectorSerializer.writeWord2VecModel(model, outputFilePath);
		System.out.println("Model saved: " + outputFilePath);
	}

	public void loadModel() {
		model = WordVectorSerializer.readWord2VecModel(outputFilePath);
		log.info("Model loaded: " + outputFilePath);
		System.out.println("Model loaded " + outputFilePath);
		System.out.println("model has layerSize: " + model.getLayerSize());
		System.out.println(model.getWordVector("http://cmt#Acceptance"));

		WeightLookupTable<VocabWord> s = model.getLookupTable();
		VocabCache<VocabWord> c = s.getVocabCache();
		log.info("num word: " + c.numWords());
//		Collection<VocabWord> col = c.tokens();
//		col.forEach(System.out::println);
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

	/**
	 * Finds how wordvector of s1 + the anchor vector is similar to the s2
	 * 
	 * @param s1
	 * @param s2
	 * @param vector
	 * @return
	 */
	public double getCosine(String s1, String s2, double[] diffVector) {
		return cosineSimilarity(addTwoVectors(model.getWordVector(s1), diffVector), model.getWordVector(s2));
	}

	public double[] sumVectors(Collection<String> words) {
		double[] result;
		ArrayList<Double> sumVector = new ArrayList<>();
		for (String word : words) {
			double[] wordVector = model.getWordVector(word);
			for (int dim = 0; dim < wordVector.length; dim++) {
				if (dim >= sumVector.size()) {
					sumVector.add(wordVector[dim]);
				} else {
					double tmp = sumVector.get(dim) + wordVector[dim];
					sumVector.set(dim, tmp);
				}
			}
		}
		result = sumVector.stream().mapToDouble(d -> d).toArray();
		return result;
	}

	public double[] subtractTwoVectors(double[] vec1, double[] vec2) {
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

	public double[] addTwoVectors(double[] vec1, double[] vec2) {
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

	public double[] getAverageVector(Collection<String> strings) {
		return getAverageVector(strings.stream().toArray(String[]::new));
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
	
	public double[] getAverageVectorFromDoubles(ArrayList<ArrayList<Double>> vectors) {
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

	public double[] getAverageVector(String[] strings) {
		ArrayList<Double> sumVector = new ArrayList<>();
		int numberOfTokens = 0;

		for (int i = 0; i < strings.length; i++) {
			double[] vector = model.getWordVector(strings[i]);
//			System.out.println("strings[" + i + "]: " + strings[i]);
			if (vector != null) {
				for (int j = 0; j < vector.length; j++) {
					if (j == sumVector.size()) {
						sumVector.add(vector[j]);
					} else {
						double newValue = vector[j] + sumVector.get(j);
						sumVector.set(j, newValue);
					}
				}
				numberOfTokens++;
			}
		}

//		System.out.println("NumberOfTokens: " + numberOfTokens);
		double[] avgVector = new double[sumVector.size()];
		for (int i = 0; i < sumVector.size(); i++) {
			avgVector[i] = sumVector.get(i) / numberOfTokens;
		}
		return avgVector;
	}
	
	public double[] getWordVector(String word) {
		return model.getWordVector(word);
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

	public String getBestAssociation(Collection<String> firstAnchors, Collection<String> secondAnchors, String word) {
		firstAnchors.add(word);

		Collection<String> res = model.wordsNearest(firstAnchors, secondAnchors, 10);
		System.out.println("assoc: ");
		res.forEach(System.out::println);
		System.out.println("-------------------------");
		String first = res.stream().findFirst().orElse(null);
//		System.out.println(res);
//		res.forEach(System.out::println);
//		System.out.println(model.getWordVector(firstAnchors.iterator().next()));
		return first;

	}

	public ArrayList<ArrayList<Double>> calculateDifferenceVectors(ArrayList<String> anchorsFromFirstOntology,
			ArrayList<String> anchorsFromSecondOntology) {
		ArrayList<ArrayList<Double>> differenceVectors = new ArrayList<>();
		for (int i = 0; i < anchorsFromFirstOntology.size(); i++) {
			ArrayList<Double> currentDifferenceVector = new ArrayList<>();
			differenceVectors.add(currentDifferenceVector);
			String first = anchorsFromFirstOntology.get(i);
			String second = anchorsFromSecondOntology.get(i);
			double[] firstVector = model.getWordVector(first);
			double[] secondVector = model.getWordVector(second);
			for (int dim = 0; dim < firstVector.length; dim++) {
				currentDifferenceVector.add(secondVector[dim] - firstVector[dim]);
			}
		}
		return differenceVectors;
	}

	public double getAvgVectorCosine(String[] s1, String[] s2) {
		double[] vectorA = getAverageVector(s1);
		double[] vectorB = getAverageVector(s2);
		double cosine = cosineSimilarity(vectorA, vectorB);
		return cosine;
	}
	
	public double[] arrayList2DoubleArray(ArrayList<Double> lst) {
		double[] dbl = new double[lst.size()];
		for (int i = 0; i < lst.size(); i++) {
			dbl[i] = lst.get(i);
		}
		return dbl;
	}
	
	public ArrayList<Double> doubleArray2ArrayList(double[] arr) {
		ArrayList<Double> lst = new ArrayList<>();
		for (int i = 0; i < arr.length; i++) {
			lst.add(arr[i]);
		}
		return lst;
	}
	
	public double vectorLength(double[] vec) {
		double len = 0;
		for (int i = 0; i < vec.length; i++) {
			len += (vec[i] * vec[i]);
		}
		return Math.sqrt(len);
	}
	
	public double squaredEucledianDistance(double[] vec1, double[] vec2) {
		if (vec1.length == vec2.length) {
			double sum = 0;
			for (int i = 0; i < vec1.length; i++) {
				double tmp = vec1[i] - vec2[i];
				sum += (tmp * tmp);
			}
			return sum;
		} else return -1;
	}

}
