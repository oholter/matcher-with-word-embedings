package org.matcher.com;

import java.util.Iterator;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.TDBFactory;

import de.dwslab.petar.walks.WalkGenerator;

public class Paths {
	WalkGenerator walkGenerator;
	String repoLocation;
	String outputFile;
	int numWalks;
	int walkDepth;
	int numThreads;
	int offset;
	int limit;
	Dataset dataset;
	Model model;

	public Paths(String repoLocation, String outputFile) {
		this.repoLocation = repoLocation;
		this.outputFile = outputFile;
		this.walkGenerator = new WalkGenerator();
		this.numWalks = 200;
		this.walkDepth = 7;
		this.numThreads = 4;
		this.offset = 0;
		this.limit = 1000000;
	}

	public void loadFromRdfFile(String rdfFile) {
		dataset = TDBFactory.createDataset(repoLocation);
		dataset.begin(ReadWrite.WRITE);
		model = dataset.getDefaultModel();
		model.read(rdfFile, "TURTLE");
		System.out.println(model.size());
		Iterator<String> str = dataset.listNames();
		while (str.hasNext()) {
			System.out.println(str.next());
		}
		dataset.commit();
		dataset.end();
		dataset.close();
		System.out.println("Closed dataset");
	}

	public void generateWalks() {
		walkGenerator.generateWalks(repoLocation, outputFile, numWalks, walkDepth, numThreads, offset, limit);
	}

	public static void main(String[] args) {
		Paths p = new Paths("/home/ole/test/test_repo", "/home/ole/master/test_onto/in/test.txt");
		p.loadFromRdfFile("/home/ole/master/test_onto/merged.ttl");
		p.generateWalks();
	}
}
