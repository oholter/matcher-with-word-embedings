package mappings.walks_generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import io.AlignmentsReader;
import io.OAEIAlignmentsReader;
import io.OntologyReader;
import mappings.candidate_finder.AnchorsCandidateFinder;
import mappings.candidate_finder.BestAnchorsCandidateFinder;
import mappings.trainer.OntologyProjector;
import mappings.utils.TestRunUtils;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public class Rdf2VecWalksGenerator extends WalksGenerator {
	private final String CURRENT_DIR;
	private final String REPO_LOCATION;
	private final String TEMP_DIR;
	private final String TEMP_IN;
	private final String TEMP_OUT;
	private final String TEMP_FILE_NAME = "temp.txt";
//	private WalkGenerator walkGenerator;
	private WalkGeneratorRand walkGenerator;
	private Dataset dataset;
	private Model model;
	private static Logger log = LoggerFactory.getLogger(BestAnchorsCandidateFinder.class);


//	public WalksGenerator(String inputFile, String outputFile, int numberOfThreads, int walkDepth,
//			int limit, int numberOfWalks, int offset) {
	public Rdf2VecWalksGenerator(String inputFile, String outputFile, int numberOfThreads, int walkDepth, int limit,
			int numberOfWalks, int offset) {
		super(inputFile, outputFile, numberOfThreads, walkDepth, limit, numberOfWalks, offset);
		File classpathRoot = new File(ClassLoader.getSystemClassLoader().getResource("").getPath());
		this.CURRENT_DIR = classpathRoot.toString();
//		REPO_LOCATION = CURRENT_DIR + "/repo";
		this.REPO_LOCATION = "/home/ole/master/test_onto/test_repo/";
//		this.walkGenerator = new WalkGenerator();
		this.walkGenerator = new WalkGeneratorRand();
		this.TEMP_DIR = CURRENT_DIR + "/temp/";
		this.TEMP_IN = TEMP_DIR + "in/";
		this.TEMP_OUT = TEMP_DIR + "out/";
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

		loadFromRdfFile();
	}

	public void loadFromRdfFile() {
		dataset = TDBFactory.createDataset(REPO_LOCATION);

		dataset.begin(ReadWrite.WRITE);
		model = dataset.getDefaultModel();
		model.read(inputFile, "TURTLE");
		System.out.println("Repo location: " + REPO_LOCATION);
		System.out.println("Model size: " + model.size());

		Iterator<String> str = dataset.listNames();
		while (str.hasNext()) {
			System.out.println(str.next());
		}

		dataset.commit();
		dataset.end();
		dataset.close();
		System.out.println("Closed dataset");
		System.out.println("number of walks: " + numberOfWalks);
	}

	public void cleanDataSet() {
		Model m = ModelFactory.createDefaultModel();
		m.read(inputFile);
		String qString = "DELETE {?s ?p ?o} WHERE {?s ?p ?o "
				+ "FILTER ( strstarts(str(?s), \"http://no.sirius.ontology/\") ||"
				+ "strstarts(str(?p), \"http://no.sirius.ontology/\") || "
				+ "strstarts(str(?q), \"http://no.sirius.ontology/\") )" + "}";

		System.out.println(qString);
		UpdateRequest query = UpdateFactory.create(qString);
		UpdateAction.execute(query, m);
		System.out.println("File cleaned... ...");
		try {
			m.write(new FileWriter(inputFile), "TURTLE");
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
//			System.out.println("file decompressed: " + fileDir + TEMP_FILE_NAME);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void generateWalks() {
		walkGenerator.generateWalks(REPO_LOCATION, TEMP_IN + TEMP_FILE_NAME + ".gz", numberOfWalks, walkDepth,
				numberOfThreads, offset, limit);
//		gzipFile(TEMP_IN);
		PathCleaner.cleanPaths(TEMP_IN, TEMP_OUT);
		gunzipFile(TEMP_OUT);
		File theFile = new File(TEMP_OUT + TEMP_FILE_NAME);
		theFile.renameTo(new File(outputFilePath));
		walkGenerator = null;
	}

	public String getOutputFile() {
//		return TEMP_OUT + TEMP_FILE_NAME;
		return outputFilePath;
	}

	public static void main(String[] args) throws Exception {
		String firstOntologyFile = TestRunUtils.firstOntologyFile;
		String secondOntologyFile = TestRunUtils.secondOntologyFile;
		String referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
		String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
		double equalityThreshold = TestRunUtils.equalityThreshold;
		double fractionOfMappings = TestRunUtils.fractionOfMappings;
		String walksType = TestRunUtils.walksType;
		String walksFile = TestRunUtils.walksFile;
		String modelPath = TestRunUtils.modelPath;

		String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();

		OntologyReader reader = new OntologyReader();
		reader.setFname(firstOntologyFile);
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();
		log.info("Read onto: " + firstOntologyFile);

		reader.setFname(secondOntologyFile);
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();
		log.info("Read onto: " + secondOntologyFile);

		// For training of ontology start:
		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });
		AnchorsCandidateFinder finder = new BestAnchorsCandidateFinder(onto1, onto2, mergedOnto,
				currentDir + "/temp/out.txt", equalityThreshold);

		/* Adding anchors by reading an alignments file */
		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, onto1, onto2);

		List<MappingObjectStr> mappings = alignmentsReader.getMappings();
		Collections.shuffle(mappings);
		for (int i = 0; i < (mappings.size() * fractionOfMappings); i++) {
			MappingObjectStr mapping = mappings.get(i);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}

		finder.addAnchorsToOntology(mergedOnto);
		OntologyReader.writeOntology(mergedOnto, "file:/home/ole/master/test_onto/merged.owl", "owl");

		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/merged.owl");
		projector.projectOntology();
		projector.saveModel(TestRunUtils.modelPath);

//		String inputFile, String outputFile, int numberOfThreads, int walkDepth,
//				int limit, int numberOfWalks, int offset
		Rdf2VecWalksGenerator p = new Rdf2VecWalksGenerator(modelPath, walksFile, 12, 4, 1000, 100, 0);
		p.generateWalks();
	}
}
