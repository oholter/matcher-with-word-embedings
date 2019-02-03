package org.matcher.com;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trainer.com.WordEmbeddingsTrainer;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ModelMaker;

// 1) Read the .ttl - file
// 2) Create data model Jena out of convenience (could later upgrade to RDFox)
// 3) Use SPARQL-queries to generate random walks
public class RandomWalks {
	private String inputFile;
	private String outputFile;
	private int walkDepth;
	private int limit;
	private int numberOfWalks;
	private Dataset dataset;
	private Model model;
	private String fileType = "TTL";
	private String modelFile = "file://home/ole/test_onto/fileModel";
	
	public RandomWalks(String in, String out, int walkDepth, int limit, int nmWalks) {
		this.inputFile = in;
		this.outputFile = out;
		this.walkDepth = walkDepth;
		this.limit = limit;
		this.numberOfWalks = nmWalks;
	}
	
	public void generateWalks() {
		model = initializeModel(inputFile, fileType);
//		System.out.println(model);
		System.out.println("generated walks");
	}
	
	public Model initializeModel(String file, String type) {
		ModelMaker modelMaker = ModelFactory.createFileModelMaker(modelFile);
		Model model =  modelMaker.createDefaultModel();
		model.read(file, type);
		return model;
	}
	
	public static void main(String[] args) {
		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		BasicConfigurator.configure();

		RandomWalks walks = new RandomWalks("/home/ole/master/test_onto/merged.ttl", "", 2, 2, 2);
		walks.generateWalks();
	}
}
