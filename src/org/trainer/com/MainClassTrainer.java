package org.trainer.com;

public class MainClassTrainer {

	public static void main(String[] args) throws Exception {
		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer("/home/ole/master/test_onto/out/merged.txt",
				"/home/ole/master/word2vec/models/merged.model");
//		trainer.stripAccents();
//		trainer.train();
		trainer.loadModel();

		System.out.println("man, woman: " + trainer.getCosine("man", "woman"));
		System.out.println("work, job: " + trainer.getCosine("work", "job"));
	}

}
