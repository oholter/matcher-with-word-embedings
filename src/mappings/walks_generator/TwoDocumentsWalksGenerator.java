/**
 * This implementation of walk generator creates two documents, one with URIs as structural embeddings and one using labels, textual representation of the URIs and other textual representation of the classes omitting the URIs
 */
package mappings.walks_generator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import directed_graph.WeightedDirectedGraph;
import mappings.trainer.WordEmbeddingsTrainer;

public class TwoDocumentsWalksGenerator extends Owl2vecWalksGenerator {
	private String labelOutputFilePath;
	BufferedWriter labelOutputWriter;

	public TwoDocumentsWalksGenerator(String in, String urisOutputFilePath, String labelOutputFilePath, int numThreads,
			int walkDepth, int limit, int nmWalks, int offset, int childLimit) {
		super(in, urisOutputFilePath, numThreads, walkDepth, limit, nmWalks, offset, childLimit);
		this.labelOutputFilePath = labelOutputFilePath;
	}

	private class TwoDocumentsWalkThread implements Runnable {
		int index;
		int start;
		int end;

		public TwoDocumentsWalkThread(int index) {
			this.index = index;
			int classesPerThread = allClasses.size() / numberOfThreads;
			int rest = allClasses.size() % numberOfThreads;
			this.start = index * classesPerThread + Math.min(rest, index);
			this.end = (index + 1) * classesPerThread + Math.min(rest, (index + 1));
		}

		public void run() {
			for (int classNum = start; classNum < end; classNum++) {
				ArrayList<String> uriLines = new ArrayList<>();
				ArrayList<String> labelLines = new ArrayList<>();
				String className = allClasses.get(classNum);
				WeightedDirectedGraph graph = createWeightedDirectedGraph(className);
				for (int i = 0; i < numberOfWalks; i++) {
					String randomWalk = graph.generateRandomWalk();
					uriLines.add(randomWalk);
				}
				writeToFile(uriLines, outputWriter); // writing the URI document
				for (int i = 0; i < numberOfWalks; i++) {
					String randomWalk = graph.generateLabelRandomWalk();
					labelLines.add(randomWalk);
				}
				writeToLabelFile(labelLines, labelOutputWriter); // writing to the label document
			}
		}
	}

	/**
	 * This method is equal to writeToFile but added as a separate method to improve
	 * parallelization it will not interfere with the other method because it writes to another document
	 **/
	public synchronized void writeToLabelFile(List<String> lines, BufferedWriter writer) {
		for (String str : lines)
			try {
				writer.write(str + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public void generateWalks() {
		initializeEmptyModel();
		readInputFileToModel();
		outputWriter = prepareDocumentWriter(outputFilePath);
		labelOutputWriter = prepareDocumentWriter(labelOutputFilePath);
		walkTheGraph();
		closeDocumentWriter(outputWriter);
		closeDocumentWriter(labelOutputWriter);
		System.out.println("Finished generating walks");
	}

	public void walkTheGraph() {
		allClasses = selectAllClasses();
		System.out.println("Random walks of depth: " + walkDepth);
		WeightedDirectedGraph g = createWeightedDirectedGraph(allClasses.get(4));
		g.printNodes();
		g.printSynonyms();

		Thread[] threads = new Thread[numberOfThreads];

		for (int i = 0; i < numberOfThreads; i++) {
			threads[i] = new Thread(new TwoDocumentsWalkThread(i));
			threads[i].start();
		}

		try {
			for (int i = 0; i < numberOfThreads; i++) {
				threads[i].join();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		BasicConfigurator.configure();

//		RandomWalksGenerator(String inputFile, String outputFile, int numberOfThreads, int walkDepth,
//		int limit, int numberOfWalks, int offset, int childLimit)
		Owl2vecWalksGenerator walks = new TwoDocumentsWalksGenerator("/home/ole/master/test_onto/merged.ttl",
				"/home/ole/master/test_onto/walks_out.txt", "/home/ole/master/test_onto/label_walks_out.txt",  8, 3, 100, 100, 0, 100);
		walks.generateWalks();
	}
}
