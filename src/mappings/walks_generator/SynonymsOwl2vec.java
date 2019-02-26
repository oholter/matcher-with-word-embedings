package mappings.walks_generator;

import java.util.ArrayList;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import directed_graph.WeightedDirectedGraph;
import mappings.trainer.WordEmbeddingsTrainer;

public class SynonymsOwl2vec extends Owl2vecWalksGenerator {

	public SynonymsOwl2vec(String in, String outputFilePath, int numThreads, int walkDepth, int limit, int nmWalks,
			int offset, int childLimit) {
		super(in, outputFilePath, numThreads, walkDepth, limit, nmWalks, offset, childLimit);
	}

	private class WalkThread implements Runnable {
		ArrayList<String> lines;
		int index;
		int start;
		int end;

		public WalkThread(int index) {
			this.index = index;
			int classesPerThread = allClasses.size() / numberOfThreads;
			int rest = allClasses.size() % numberOfThreads;
			this.start = index * classesPerThread + Math.min(rest, index);
			this.end = (index + 1) * classesPerThread + Math.min(rest, (index + 1));
		}

		public void run() {
			for (int classNum = start; classNum < end; classNum++) {
				lines = new ArrayList<>();
				String className = allClasses.get(classNum);
				WeightedDirectedGraph graph = createWeightedDirectedGraph(className);
				for (int i = 0; i < numberOfWalks; i++) {
					String randomWalk = graph.generateRandomWalkWithSynonyms();
					lines.add(randomWalk);
				}
				writeToFile(lines, outputWriter);
			}
		}
	}
	
	public void generateWalks() {
		initializeEmptyModel();
		readInputFileToModel();
		outputWriter = prepareDocumentWriter(outputFilePath);
		walkTheGraph();
		closeDocumentWriter(outputWriter);
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
			threads[i] = new Thread(new WalkThread(i));
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
		Owl2vecWalksGenerator walks = new SynonymsOwl2vec("/home/ole/master/test_onto/merged.ttl",
				"/home/ole/master/test_onto/walks_out.txt", 8, 3, 100, 100, 0, 100);
		walks.generateWalks();
	}
}
