package mappings.walks_generator;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import constraints.uio.ifi.ontology.toolkit.constraint.utils.Utility;
import mappings.trainer.OntologyProjector;
import mappings.utils.Rdf4j2Jena;
import mappings.utils.StringUtils;
import mappings.utils.TestRunUtils;
import node_graph.Edge;
import node_graph.Node;
import node_graph.NodeGraph;

public class SecondOrderWalksGenerator extends WalksGenerator {
	Logger log = LoggerFactory.getLogger("Logger");
	private double p;
	private double q;
	private NodeGraph graph;
	private Model model;
	private String fileType = "TTL";
	private BufferedWriter outputWriter;
	protected String[] UNDESIRED_CLASSES = { "http://www.w3.org/2002/07/owl#Thing",
			"http://www.w3.org/2002/07/owl#Class" };
	protected String[] UNDESIRED_PROPERTIES = { "http://www.w3.org/2002/07/owl#inverseOf" };
	private String adjacentPropertiesQuery;
	private String synonymsQuery;
	protected int numberOfWritingsToFile = 0;
	private CyclicBarrier cyclicBarrier;
	private int iter = 0;
	private String outputFormat;
	private List<List<Node>> writeBuffer;
	private int walkThreadsFinished = 0;
	private Lock writeBufferLock = new ReentrantLock();

	public SecondOrderWalksGenerator(String inputFile, String outputFile, int numberOfThreads, int walkDepth, int limit,
			int numberOfWalks, int offset, double p, double q, String outputFormat) {
		super(inputFile, outputFile, numberOfThreads, walkDepth, limit, numberOfWalks, offset);
		this.p = p;
		this.q = q;
		this.outputFormat = outputFormat;
		this.adjacentPropertiesQuery = "SELECT DISTINCT ?p ?o WHERE {$CLASS$ ?p ?o .} LIMIT " + limit;
		this.synonymsQuery = "SELECT DISTINCT ?o WHERE {$CLASS$ <http://www.w3.org/2000/01/rdf-schema#label> ?o } "
				+ "LIMIT " + limit;

		BasicConfigurator.configure();
		writeBuffer = new LinkedList<List<Node>>();

		log.info("Initializing the model");
		initializeEmptyModel();
	}

	public void useRdf4jModel(org.eclipse.rdf4j.model.Model rdf4jModel) {
		rdf4jModel.forEach(stmt -> model.add(Rdf4j2Jena.convert(model, stmt)));
	}

	public void generateWalks() {
		log.info("preparing document writer");
		outputWriter = prepareDocumentWriter(outputFilePath);
		log.info("initializing node graph");
		initializeNodeGraph();
		log.info("staring to generate walks");
		walkTheGraph();
		log.info("closing document writer");
		closeDocumentWriter(outputWriter);
	}

	public void initializeEmptyModel() {
		model = ModelFactory.createDefaultModel();
	}

	public void readInputFileToModel(String filePath) {
		log.info("reading input");
		model.read(inputFile, fileType);
	}

	public BufferedWriter prepareDocumentWriter(String outputFilePath) {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath, false), "utf-8"),
					32 * 1024);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("Not found file: " + outputFilePath);
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return writer;
	}

	public void closeDocumentWriter(BufferedWriter writer) {
		try {
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void initializeNodeGraph() {
		List<Node> nodeList = findAllClasses();
//		nodeList.forEach(System.out::println);
		graph = new NodeGraph(nodeList, p, q);
		for (Node n : nodeList) {
			List<Edge> currentEdges = findAdjacentEdges(n);
			n.edges.addAll(currentEdges);
		}
	}

	public List<Node> findAllClasses() {
		List<Node> nodeList = new ArrayList<Node>();
		String queryString = "SELECT DISTINCT ?s WHERE {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> . }"
				+ " OFFSET " + offset + " LIMIT " + limit;
//		String queryString = "SELECT DISTINCT ?s WHERE {?s ?p ?o  " + " } " 
//		System.out.println(queryString);
		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();

		while (results.hasNext()) {
			QuerySolution result = results.next();
			if (StringUtils.isUri(result.get("s").toString())) {
				String currentResult = result.get("s").toString();
				Node newNode = new Node(currentResult);

				if (outputFormat.toLowerCase().equals("allsynonyms")
						|| outputFormat.toLowerCase().equals("onesynonym")) {
					newNode.synonyms = findSynonyms(newNode);
				}
				nodeList.add(newNode);

			}
		}
		qe.close();
		System.out.println("TOTAL CLASSES: " + nodeList.size());
		return nodeList;
	}

	public List<String> findSynonyms(Node node) {
		String className = node.label;
		ArrayList<String> synonyms = new ArrayList<>();
		String queryString = synonymsQuery.replace("$CLASS$", "<" + className + ">");
//		System.out.println(queryString);
		Query query = QueryFactory.create(queryString);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();

		while (results.hasNext()) {
			QuerySolution result = results.next();
			String synonym = result.get("o").asLiteral().getValue().toString();
			synonyms.add(synonym);
//			System.out.println("found synonym: " + synonym);
		}
		qe.close();
//		System.out.println("synonyms: ");
//		synonyms.forEach(System.out::println);

//		synonyms.add(className);

		return synonyms;

	}

	public List<Edge> findAdjacentEdges(Node node) {
		String className = node.label;
		ArrayList<Edge> edgeList = new ArrayList<>();
		String queryString = adjacentPropertiesQuery.replace("$CLASS$", "<" + className + ">");
		try {
			Query query = QueryFactory.create(queryString);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
			ResultSet res = queryExecution.execSelect();
			int numRes = 0;

			while (res.hasNext()) {
				QuerySolution sol = res.next();
				String currentProperty = sol.get("?p").toString();
				boolean undesiredProperty = Arrays.stream(UNDESIRED_PROPERTIES).anyMatch(currentProperty::equals);
				if (!undesiredProperty) {
					Edge currentEdge = new Edge(currentProperty);
					String outNodeString = sol.get("?o").toString();

					if (node.label.equals(outNodeString)) {
						continue; // not adding an edge returning to itself
					}

					Node nextNode;

					// avoid edges to classes of for example owl:Thing
					boolean undesiredClass = Arrays.stream(UNDESIRED_CLASSES).anyMatch(outNodeString::equals);
					if (!undesiredClass) {
						if (graph.containsUri(outNodeString)) {
							nextNode = graph.getNodeFromUri(outNodeString);
						} else {
//							nextNode = new Node(outNodeString); // a literal
							continue;
						}
						currentEdge.outNode = nextNode;
						currentEdge.inNode = node;
						if (currentEdge.weight > 0) {
							edgeList.add(currentEdge);
						}

						// Adding the superClassNodes to the superClass as well as subClassNodes

						if (currentProperty.equals("http://www.w3.org/2000/01/rdf-schema#subClassOf")) {
							Edge superClassEdge = new Edge("http://www.w3.org/2000/01/rdf-schema#superClassOf");
							superClassEdge.inNode = nextNode;
							superClassEdge.outNode = node;
							if (superClassEdge.weight > 0) {
								nextNode.edges.add(superClassEdge);
							}
//							System.out.println("Adding edge: " + superClassEdge.inNode + superClassEdge + superClassEdge.outNode);
						}
					} else {
//						System.out.println("Found undesired class: " + outNodeString);
					}
				} else { // Undesired property
//					System.out.println("Found undesired property: " + currentProperty);
				}
			}
			return edgeList;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("The query that failed was: " + queryString);
			return null;
		}
	}

	public void writeToFile(List<List<Node>> walks, BufferedWriter writer) {
		numberOfWritingsToFile++;
		if (numberOfWritingsToFile % 1000 == 0) {
			log.info("Processed: " + numberOfWritingsToFile + " writings");
		}

		writeBufferLock.lock();
		try {
			while(!walks.isEmpty()) {
				try {
					List<Node> walk = walks.remove(0);
					String str = NodeGraph.nodeListToString(walk, outputFormat);
					writer.write(str + "\n");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} finally {
			writeBufferLock.unlock();
		}
	}

	public synchronized void threadFinished() {
		walkThreadsFinished++;
	}

	public void appendToWriteBuffer(List<List<Node>> walks) {
		writeBufferLock.lock();
		try {
			writeBuffer.addAll(walks);
		} finally {
			writeBufferLock.unlock();
		}
	}

	public void walkTheGraph() {
		Thread[] threads = new Thread[numberOfThreads];

		for (int i = 0; i < numberOfThreads; i++) {
			threads[i] = new Thread(new WalkThread(i));
			threads[i].start();
		}

//		Thread writingThread = new Thread(new FileWriter(outputWriter, writeBuffer));
//		writingThread.start();

		try {
			for (int i = 0; i < numberOfThreads; i++) {
				threads[i].join();
				log.info("joined thread: " + i);
			}

//			writingThread.join();
//			log.info("joined writingThread");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class FileWriter implements Runnable {

		BufferedWriter writer;
		List<List<Node>> writeBuffer;

		public FileWriter(BufferedWriter writer, List<List<Node>> writeBuffer) {
			this.writer = writer;
			this.writeBuffer = writeBuffer;
		}

		@Override
		public void run() {

			
			int numWrites = 0;
			while (walkThreadsFinished < numberOfThreads && !writeBuffer.isEmpty()) {
				numWrites++;
				if (numWrites % 100 == 0) {
					log.info("Written : " + numWrites);
				}
				writeToFile(writeBuffer, writer);
			}

		}
	}

	private class WalkThread implements Runnable {
		int index;
		int start;
		int end;

		public WalkThread(int index) {
			this.index = index;
			int walksPerThread = numberOfWalks / numberOfThreads;
			int rest = numberOfWalks % numberOfThreads;
			this.start = index * walksPerThread + Math.min(rest, index);
			this.end = (index + 1) * walksPerThread + Math.min(rest, (index + 1));
		}

		@Override
		public void run() {
			for (int walkNum = start; walkNum < end; walkNum++) {
				List<List<Node>> walks = new ArrayList<>();
				for (Node node : graph.getNodeList()) {
					List<Node> walk = graph.createWalks(node, walkDepth);
					walks.add(walk);
				}
//				appendToWriteBuffer(walks);
				writeToFile(walks, outputWriter);
			}
			threadFinished();
//			System.out.println("Thread: " + index + " finished");
		}

	}

	public static void main(String[] args) throws Exception {
		long startTime = System.nanoTime();
		System.out.println("starting projection");
//		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/bio_data/go.owl");
//		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/ekaw.owl");
//		projector.projectOntology();
//		projector.saveModel(TestRunUtils.modelPath);
//		System.out.println("staring walksgenerator");
//		org.eclipse.rdf4j.model.Model rdf4jModel = projector.getModel();

//		SecondOrderWalksGenerator(String inputFile, String outputFile, int numberOfThreads, int walkDepth,
//		int limit, int numberOfWalks, int offset, int p, int q)

		SecondOrderWalksGenerator walks = new SecondOrderWalksGenerator("/home/ole/master/bio_data/go.owl",
				"/home/ole/master/test_onto/walks_out.txt", 4, 40, 1000, 50, 0, 1, 0.001, "onesynonym");
//		walks.useRdf4jModel(rdf4jModel);
		walks.readInputFileToModel(TestRunUtils.modelPath);
		walks.generateWalks();
		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1000000;
		System.out.println("duration: " + duration);
	}
}