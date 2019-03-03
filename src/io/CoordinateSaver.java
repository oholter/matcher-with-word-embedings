package io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.models.word2vec.wordstore.VocabCache;
import org.deeplearning4j.plot.BarnesHutTsne;
import org.nd4j.linalg.api.buffer.DataBuffer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.primitives.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mappings.trainer.WordEmbeddingsTrainer;

public class CoordinateSaver {
	public static void main(String[] args) throws Exception {
		int iterations = 4; // 4
		int learningRate = 500; // 500
		int perplexity = 200; // 5000
		String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
		String modelFile = currentDir + "/temp/out.txt";
		String outFile = currentDir + "/temp/coords.csv";
		
		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		BasicConfigurator.configure();

		Nd4j.setDataType(DataBuffer.Type.DOUBLE);
		List<String> cacheList = new ArrayList<>(); // cacheList is a dynamic array of strings used to hold all words

		// STEP 2: Turn text input into a list of words
		log.info("Load & Vectorize data....");
		File wordFile = new File(modelFile); // Open the file
		// Get the data of all unique word vectors
		Word2Vec vecs = WordVectorSerializer.readWord2VecModel(new File(modelFile));
		VocabCache cache = vecs.getVocab();
		WeightLookupTable<VocabWord> lookupTable = vecs.getLookupTable();
		Pair<WeightLookupTable<VocabWord>, VocabCache> vectors = new Pair<>();
		vectors.setFirst(lookupTable);
		vectors.setSecond(cache);
//		INDArray weights = vectors.getFirst().getSyn0(); // seperate weights of unique words into their own list
		INDArray weights = lookupTable.getWeights();

		for (int i = 0; i < cache.numWords(); i++) // seperate strings of words into their own list
			cacheList.add(cache.wordAtIndex(i));

		// STEP 3: build a dual-tree tsne to use later
		log.info("Build model....");
		BarnesHutTsne tsne = new BarnesHutTsne.Builder().setMaxIter(iterations).theta(0.5).normalize(false)
				.learningRate(learningRate).useAdaGrad(false)
               .perplexity(perplexity)
				.build();

		// STEP 4: establish the tsne values and save them to a file
		log.info("Store TSNE Coordinates for Plotting....");
		(new File(outFile)).getParentFile().mkdirs();

		tsne.fit(weights);
		tsne.saveAsFile(cacheList, outFile);
		System.out.println("coordinates written to: " + outFile);
	}
}
