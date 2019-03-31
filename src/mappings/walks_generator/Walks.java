package mappings.walks_generator;

import java.io.File;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.rdf.model.Model;

import de.dwslab.petar.walks.WalkGeneratorRand;
import mappings.utils.TestRunUtils;

public class Walks {
	private File classpathRoot = new File(ClassLoader.getSystemClassLoader().getResource("").getPath());
	private final String CURRENT_DIR  = classpathRoot.toString();
	private final String REPO_LOCATION = CURRENT_DIR + "/repo";
	private final String TEMP_DIR = CURRENT_DIR + "/temp/";
	private final String TEMP_OUT = TEMP_DIR + "out/";
	private final String TEMP_FILE_NAME = "temp.txt";
	private final String LABEL_TEMP_FILE_NAME = "label.txt";
//	private WalkGenerator walkGenerator;
	private WalkGeneratorRand walkGenerator;
	private int numWalks = 50;
	private int walkDepth = 40;
	private int numThreads = 12;
	private int offset = 0;
	private int classLimit = 10000;
	private int childLimit = 100;
	private Dataset dataset;
	private Model model;
	private String inputFile;
	private String type;
	String outputFile = TEMP_FILE_NAME;
	String labelOutputFile = TEMP_OUT + LABEL_TEMP_FILE_NAME;

	public Walks(String inputFile, String type) {
		this.inputFile = inputFile;
		this.type = type;
		this.walkGenerator = new WalkGeneratorRand();
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
	
	public Walks(String inputFile, String type, String outputFile, int numWalks, int walkDepth, int numThreads, int offset, int classLimit) {
		this(inputFile, type);
		this.outputFile = outputFile;
		this.numWalks = numWalks;
		this.walkDepth = walkDepth;
		this.numThreads = numThreads;
		this.offset = offset;
		this.classLimit = classLimit;
	}
	
	public Walks(String inputFile, String type, String outputFile, String labelOutputFile, int numWalks, int walkDepth, int numThreads, int offset, int classLimit) {
		this(inputFile, type, outputFile, numWalks, walkDepth, numThreads, offset, classLimit);
		this.labelOutputFile = labelOutputFile;
	}

	public String getOutputFile() {
		return TestRunUtils.walksFile;
	}

	public String getLabelOutputFile() {
		return labelOutputFile;
	}

	public void generateWalks() {
//		SynonymsOwl2vec(String in, String outputFilePath, int numThreads, int walkDepth, int limit, int nmWalks,
//				int offset, int childLimit)

		WalksGenerator walks;
		if (type.toLowerCase().equals("rdf2vec")) {
			walks = new Rdf2VecWalksGenerator(inputFile, outputFile, numThreads, walkDepth, classLimit, numWalks,
					offset);
			System.out.println("using RDF2VEC");
		} else if (type.toLowerCase().equals("owl2vec")) {
			walks = new Owl2vecWalksGenerator(inputFile, outputFile, numThreads, walkDepth, classLimit, numWalks,
					offset, childLimit);
			System.out.println("using OWL2Vec");
		} else if (type.toLowerCase().equals("synonymsowl2vec")) {
			walks = new SynonymsOwl2vec(inputFile, outputFile, numThreads, walkDepth, classLimit, numWalks, offset,
					childLimit);
			System.out.println("using synonymsOWL2Vec");
		} else if (type.toLowerCase().equals("twodocuments")) {
			walks = new TwoDocumentsWalksGenerator(inputFile, outputFile, getLabelOutputFile(), numThreads,
					walkDepth, classLimit, numWalks, offset, childLimit);
			System.out.println("Using two documents");
		} else if (type.toLowerCase().equals("subclasswalks")) {
			walks = new SubClassWalksGenerator(inputFile, outputFile, numThreads, walkDepth, classLimit, numWalks,
					offset);
			System.out.println("Using subClassWalks");
		} else if (type.toLowerCase().equals("secondorder")) {
			double p = TestRunUtils.p;
			double q = TestRunUtils.q;
			walks = new SecondOrderWalksGenerator(inputFile, outputFile, labelOutputFile, numThreads, walkDepth, classLimit,
					numWalks, offset, p, q, TestRunUtils.whatToEmbed, TestRunUtils.includeIndividuals, TestRunUtils.includeEdges);
			System.out.println("Using secondOrderWalks");
		}

		else {
			walks = new Owl2vecWalksGenerator(inputFile, outputFile, numThreads, walkDepth, classLimit, numWalks,
					offset, childLimit);
			System.out.println("using OWL2Vec");
		}
		walks.generateWalks();
	}
}
