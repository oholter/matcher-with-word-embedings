package org.matcher.com;

import java.io.File;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

import de.dwslab.petar.walks.WalkGeneratorRand;

public class Walks {
	private final String CURRENT_DIR;
	private final String REPO_LOCATION;
	private final String TEMP_DIR;
	private final String TEMP_OUT;
	private final String TEMP_FILE_NAME = "temp.txt";
//	private WalkGenerator walkGenerator;
	private WalkGeneratorRand walkGenerator;
	private int numWalks;
	private int walkDepth;
	private int numThreads;
	private int offset;
	private int classLimit;
	private int childLimit;
	private Dataset dataset;
	private Model model;
	private String inputFile;

	public Walks(String inputFile) {
		this.inputFile = inputFile;
		File classpathRoot = new File(ClassLoader.getSystemClassLoader().getResource("").getPath());
		CURRENT_DIR = classpathRoot.toString();
		REPO_LOCATION = CURRENT_DIR + "/repo";
//	this.walkGenerator = new WalkGenerator();
		this.walkGenerator = new WalkGeneratorRand();
		this.numWalks = 50;
		this.walkDepth = 4;
		this.numThreads = 8;
		this.offset = 0;
		this.classLimit = 100000;
		this.childLimit = 100;
		TEMP_DIR = CURRENT_DIR + "/temp/";
		TEMP_OUT = TEMP_DIR + "out/";
		try {
			File tempDir = new File(TEMP_DIR);
			File tempOutDir = new File(TEMP_OUT);

			if (!tempDir.exists()) {
				tempDir.mkdir();
			}
			if (!tempOutDir.exists()) {
				tempOutDir.mkdir();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getOutputFile() {
		return TEMP_OUT + TEMP_FILE_NAME;
	}

	public void generateWalks() {
		RandomWalksGenerator walks = new RandomWalksGenerator(inputFile, getOutputFile(), numThreads, walkDepth,
				classLimit, childLimit, numWalks);
		walks.generateWalks();
	}
}
