package org.matcher.com;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

import de.dwslab.petar.walks.PathCleaner;
import de.dwslab.petar.walks.WalkGeneratorRand;

public class Walks {
	private final String CURRENT_DIR;
	private final String REPO_LOCATION;
	private final String TEMP_DIR;
	private final String TEMP_IN;
	private final String TEMP_OUT;
	private final String TEMP_FILE_NAME = "temp.txt";
//	private WalkGenerator walkGenerator;
	private WalkGeneratorRand walkGenerator;
	private int numWalks;
	private int walkDepth;
	private int numThreads;
	private int offset;
	private int limit;
	private Dataset dataset;
	private Model model;

	public Walks() {
		File classpathRoot = new File(ClassLoader.getSystemClassLoader().getResource("").getPath());
		CURRENT_DIR = classpathRoot.toString();
		REPO_LOCATION = CURRENT_DIR + "/repo";
//		this.walkGenerator = new WalkGenerator();
		this.walkGenerator = new WalkGeneratorRand();
		this.numWalks = 50;
		this.walkDepth = 4;
		this.numThreads = 8;
		this.offset = 0;
		this.limit = 5000;
		TEMP_DIR = CURRENT_DIR + "/temp/";
		TEMP_IN = TEMP_DIR + "in/";
		TEMP_OUT = TEMP_DIR + "out/";
		try {
			File tempDir = new File(TEMP_DIR);
			File tempInDir = new File(TEMP_IN);
			File tempOutDir = new File(TEMP_OUT);

			if (!tempDir.exists()) {
				tempDir.mkdir();
			}
			if (!tempInDir.exists()) {
				tempInDir.mkdir();
			}
			if (!tempOutDir.exists()) {
				tempOutDir.mkdir();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadFromRdfFile(String rdfFile) {
		dataset = TDBFactory.createDataset(REPO_LOCATION);
		
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
	
	public void cleanDataSet(String rdfFile) {
		Model m = ModelFactory.createDefaultModel();
		m.read(rdfFile);
		String qString = "DELETE {?s ?p ?o} WHERE {?s ?p ?o "
				+ "FILTER ( strstarts(str(?s), \"http://no.sirius.ontology/\") ||"
				+ "strstarts(str(?p), \"http://no.sirius.ontology/\") || "
				+ "strstarts(str(?q), \"http://no.sirius.ontology/\") )"
				+ "}";
		
		System.out.println(qString);
		UpdateRequest query = UpdateFactory.create(qString);
		UpdateAction.execute(query, m);
		System.out.println("File cleaned... ...");
		try {
			m.write(new FileWriter(rdfFile), "TURTLE");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void gzipFile(String fileDir) {
		String filePath = fileDir + TEMP_FILE_NAME;
		FileInputStream inStream;
		OutputStream outStream;
		GZIPOutputStream gOutStream;
		byte[] buffer = new byte[1024];

		try {
			inStream = new FileInputStream(new File(filePath));
			outStream = new FileOutputStream(new File(filePath + ".gz"));
			gOutStream = new GZIPOutputStream(outStream);
			int bytesRead;

			while ((bytesRead = inStream.read(buffer)) > 0) {
				gOutStream.write(buffer, 0, bytesRead);
			}

			inStream.close();
			gOutStream.finish();
			outStream.close();

			File oldFile = new File(filePath);
			oldFile.delete();

			System.out.println("File compressed: " + filePath + ".gz");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void gunzipFile(String fileDir) {
		String filePath = fileDir + TEMP_FILE_NAME + ".gz";
		File oldFile = new File(filePath);
		byte[] buffer = new byte[1024];
	
		try {
			FileInputStream inStream = new FileInputStream(oldFile);
			GZIPInputStream gInStream = new GZIPInputStream(inStream);
			FileOutputStream outStream = new FileOutputStream(fileDir + TEMP_FILE_NAME);
			
			int bytesRead;
			while ((bytesRead = gInStream.read(buffer)) > 0) {
				outStream.write(buffer, 0, bytesRead);
			}
			
			gInStream.close();
			inStream.close();
			outStream.close();
			
			oldFile.delete();
			System.out.println("file decompressed: " + fileDir + TEMP_FILE_NAME);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void generateWalks() {
		walkGenerator.generateWalks(REPO_LOCATION, TEMP_IN + TEMP_FILE_NAME +".gz", numWalks, walkDepth, numThreads, offset,
				limit);
//		gzipFile(TEMP_IN);
		PathCleaner.cleanPaths(TEMP_IN, TEMP_OUT);
		gunzipFile(TEMP_OUT);
	}

	public String getOutputFile() {
		return TEMP_OUT + TEMP_FILE_NAME;
	}

	public static void main(String[] args) {
		Walks p = new Walks();
//		p.cleanDataSet("/home/ole/master/test_onto/merged.ttl");
		p.loadFromRdfFile("/home/ole/master/test_onto/merged.ttl");
//		p.generateWalks();
	}
}
