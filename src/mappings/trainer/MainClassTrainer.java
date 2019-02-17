package mappings.trainer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.BasicConfigurator;

public class MainClassTrainer {

	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();

//		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer("/home/ole/master/test_onto/out/merged.txt",
//				"/home/ole/master/word2vec/models/merged.model");
//		trainer.stripAccents();
//		trainer.loadModel();
//
//		System.out.println("man, woman: " + trainer.getCosine("man", "woman"));
//		System.out.println("work, job: " + trainer.getCosine("work", "job"));
		
		String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
		System.out.println(currentDir);
		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer("/home/ole/master/test_onto/out/merged.txt", currentDir + "/temp/out.txt");
		System.out.println(trainer.getOutputFilePath());
		trainer.train();
		trainer.loadModel();
//		trainer.tsne();
		ArrayList<String> firstAnchors = new ArrayList<>();
		ArrayList<String> secondAnchors = new ArrayList<>();
		firstAnchors.add("http://cmt#Paper");
		firstAnchors.add("http://cmt#Conference");
		firstAnchors.add("http://cmt#writtenBy");
		firstAnchors.add("http://cmt#Person");
		firstAnchors.add("http://cmt#ConferenceMember");
		
		secondAnchors.add("http://ekaw#Paper");
		secondAnchors.add("http://ekaw#Conference");
		secondAnchors.add("http://ekaw#writtenBy");
		secondAnchors.add("http://ekaw#Person");
		secondAnchors.add("http://ekaw#Conference_Participant");

		String word = "http://cmt#Review";

		String association = trainer.getBestAssociation(firstAnchors, secondAnchors, word);
		System.out.println("Best association to cmt#Review: " + association);
		System.out.println("This has a similarity of: " + trainer.getCosine(word, association));
		
		word = "http://cmt#hasAuthor";
		association = trainer.getBestAssociation(firstAnchors, secondAnchors, word);
		System.out.println("Best association to cmt#hasAuthor: " + association);
		System.out.println("This has a similarity of: " + trainer.getCosine(word, association));
		
		word = "http://cmt#ConferenceMember";
		association = trainer.getBestAssociation(firstAnchors, secondAnchors, word);
		System.out.println("Best association to cmt#ConferenceMember: " + association);
		System.out.println("This has a similarity of: " + trainer.getCosine(word, association));
	}

}
