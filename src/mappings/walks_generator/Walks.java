package mappings.walks_generator;

import java.io.File;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

import de.dwslab.petar.walks.WalkGeneratorRand;
import mappings.utils.TestRunUtils;

public class Walks {
	private final String CURRENT_DIR;
	private final String REPO_LOCATION;
	private final String TEMP_DIR;
	private final String TEMP_OUT;
	private final String TEMP_FILE_NAME = "temp.txt";
	private final String LABEL_TEMP_FILE_NAME = "label.txt";
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
	private String type;
	String outputFile;

	public Walks(String inputFile, String type) {
		this.inputFile = inputFile;
		this.type = type;
		File classpathRoot = new File(ClassLoader.getSystemClassLoader().getResource("").getPath());
		CURRENT_DIR = classpathRoot.toString();
		REPO_LOCATION = CURRENT_DIR + "/repo";
//	this.walkGenerator = new WalkGenerator();
		this.walkGenerator = new WalkGeneratorRand();
		this.numWalks = 50;
		this.walkDepth = 40;
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
		return TestRunUtils.walksFile;
	}

	public String getLabelOutputFile() {
		return TEMP_OUT + LABEL_TEMP_FILE_NAME;
	}

	public void generateWalks() {
//		SynonymsOwl2vec(String in, String outputFilePath, int numThreads, int walkDepth, int limit, int nmWalks,
//				int offset, int childLimit)

		WalksGenerator walks;
		if (type.toLowerCase().equals("rdf2vec")) {
			walks = new Rdf2VecWalksGenerator(inputFile, getOutputFile(), numThreads, walkDepth, classLimit, numWalks,
					offset);
			System.out.println("using RDF2VEC");
		} else if (type.toLowerCase().equals("owl2vec")) {
			walks = new Owl2vecWalksGenerator(inputFile, getOutputFile(), numThreads, walkDepth, classLimit, numWalks,
					offset, childLimit);
			System.out.println("using OWL2Vec");
		} else if (type.toLowerCase().equals("synonymsowl2vec")) {
			walks = new SynonymsOwl2vec(inputFile, getOutputFile(), numThreads, walkDepth, classLimit, numWalks, offset,
					childLimit);
			System.out.println("using synonymsOWL2Vec");
		} else if (type.toLowerCase().equals("twodocuments")) {
			walks = new TwoDocumentsWalksGenerator(inputFile, getOutputFile(), getLabelOutputFile(), numThreads,
					walkDepth, classLimit, numWalks, offset, childLimit);
			System.out.println("Using two documents");
		} else if (type.toLowerCase().equals("subclasswalks")) {
			walks = new SubClassWalksGenerator(inputFile, getOutputFile(), numThreads, walkDepth, classLimit, numWalks,
					offset);
			System.out.println("Using subClassWalks");
		} else if (type.toLowerCase().equals("secondorder")) {
			double p = TestRunUtils.p;
			double q = TestRunUtils.q;
			walks = new SecondOrderWalksGenerator(inputFile, getOutputFile(), numThreads, walkDepth, classLimit,
					numWalks, offset, p, q, TestRunUtils.whatToEmbed, TestRunUtils.includeIndividuals);
			System.out.println("Using secondOrderWalks");
		}

		else {
			walks = new Owl2vecWalksGenerator(inputFile, getOutputFile(), numThreads, walkDepth, classLimit, numWalks,
					offset, childLimit);
			System.out.println("using OWL2Vec");
		}
		walks.generateWalks();
	}
}
