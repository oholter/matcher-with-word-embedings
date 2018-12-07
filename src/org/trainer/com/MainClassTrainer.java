package org.trainer.com;

public class MainClassTrainer {

	public static void main(String[] args) throws Exception {
		Trainer trainer = new Trainer("/home/ole/master/fastText/data/fil9", "/home/ole/master/word2vec/models/fil9.model");
//		trainer.stripAccents();
//		trainer.train();
		trainer.loadModel();
		
		System.out.println("man, woman: " + trainer.getCosine("man", "woman"));
		System.out.println("work, job: " + trainer.getCosine("work", "job"));
	}

}
