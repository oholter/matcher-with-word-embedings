package mappings.walks_generator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
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

import io.OntologyReader;
import mappings.trainer.OntologyProjector;
import mappings.utils.Rdf4j2Jena;
import mappings.utils.StringUtils;
import mappings.utils.TestRunUtils;
import node_graph.Edge;
import node_graph.Element;
import node_graph.Node;
import node_graph.NodeGraph;

public class SecondOrderWalksGenerator extends WalksGenerator {
	private static final Logger log = LoggerFactory.getLogger(SecondOrderWalksGenerator.class);
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
	private String membersQuery = "SELECT DISTINCT ?e WHERE {?e a $CLASS$}";
	private int processedClasses = 0;
	private int numberOfClasses = 0;
	private int currentWalkNumber = 0;
	private CyclicBarrier cyclicBarrier;
	private int iter = 0;
	private String outputFormat;
	private List<List<Node>> writeBuffer;
	private int walkThreadsFinished = 0;
	private Lock writeBufferLock = new ReentrantLock();
	private boolean includeIndividuals;
	private String[] validOutputFormats = { "fulluri", "uripart", "onesynonym", "allsynonyms", "words",
			"allsynonymsanduri", "gouripart", "twodocuments", "uripartnonormalized" };
	final private String[] synonymOutputFormats = { "onesynonym", "allsynonyms", "allsynonymsanduri", "twodocuments" };
	private String secondOutputFile;
	private BufferedWriter secondOutputWriter;
	private boolean includeUriPartInSynonyms = false;
	private boolean includeCommentsInSynonyms = false;
	private boolean includeEdges;

	public SecondOrderWalksGenerator(String inputFile, String outputFile, int numberOfThreads, int walkDepth, int limit,
			int numberOfWalks, int offset, double p, double q, String outputFormat, boolean includeIndividuals, boolean includeEdges) {
		super(inputFile, outputFile, numberOfThreads, walkDepth, limit, numberOfWalks, offset);
		boolean validOutputFormat = Arrays.stream(validOutputFormats).anyMatch(outputFormat.toLowerCase()::equals);
		if (!validOutputFormat) {
			throw new IllegalArgumentException("Output format: " + outputFormat + " is not known");
		}

		this.p = p;
		this.q = q;
		this.outputFormat = outputFormat;
		this.adjacentPropertiesQuery = "SELECT DISTINCT ?p ?o WHERE {$CLASS$ ?p ?o .} LIMIT " + limit;
		if (includeCommentsInSynonyms) {
			this.synonymsQuery = "SELECT DISTINCT ?o WHERE { { $CLASS$ <http://www.w3.org/2000/01/rdf-schema#label> ?o } "
					+ "UNION " + "{ $CLASS$ <http://www.w3.org/2000/01/rdf-schema#comment> ?o } . } " + "LIMIT "
					+ limit;
		} else {
			this.synonymsQuery = "SELECT DISTINCT ?o WHERE { $CLASS$ <http://www.w3.org/2000/01/rdf-schema#label> ?o } "
					+ "LIMIT " + limit;
		}
		this.includeIndividuals = includeIndividuals;
		this.includeEdges = includeEdges;

		writeBuffer = new LinkedList<List<Node>>();
		log.info("Initializing the model");
		initializeEmptyModel();
	}

	/*
	 * Constructor for twodocuments type output format
	 */
	public SecondOrderWalksGenerator(String inputFile, String outputFile, String secondOutputFile, int numberOfThreads,
			int walkDepth, int limit, int numberOfWalks, int offset, double p, double q, String outputFormat,
			boolean includeIndividuals, boolean includeEdges) {
		this(inputFile, outputFile, numberOfThreads, walkDepth, limit, numberOfWalks, offset, p, q, outputFormat,
				includeIndividuals, includeEdges);

		this.secondOutputFile = secondOutputFile;
	}

	public void useRdf4jModel(org.eclipse.rdf4j.model.Model rdf4jModel) {
		rdf4jModel.forEach(stmt -> model.add(Rdf4j2Jena.convert(model, stmt)));
	}

	public void generateWalks() {
		readInputFileToModel(inputFile);
		log.info("preparing document writer");
		outputWriter = prepareDocumentWriter(outputFilePath);
		if (outputFormat.toLowerCase().equals("twodocuments")) {
			secondOutputWriter = prepareDocumentWriter(secondOutputFile);
		}
		log.info("initializing node graph");
		initializeNodeGraph();
		log.info("staring to generate walks");
		walkTheGraph();
		log.info("closing document writer");
		closeDocumentWriter(outputWriter);
		if (outputFormat.toLowerCase().equals("twodocuments")) {
			closeDocumentWriter(secondOutputWriter);
		}
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
			log.info("written to file");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void initializeNodeGraph() {
		List<Node> nodeList = findAllClasses();
//		nodeList.forEach(System.out::println);
		graph = new NodeGraph(nodeList, p, q, includeEdges);
		for (Node n : nodeList) {
			List<Edge> currentEdges = findAdjacentEdges(n);
			n.edges.addAll(currentEdges);
			if (includeIndividuals) {
				List<Edge> memberEdges = findMembersOfClass(n);
				n.edges.addAll(memberEdges);
			}
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

				boolean needSynonyms = Arrays.stream(synonymOutputFormats).anyMatch(outputFormat.toLowerCase()::equals);
				if (needSynonyms) {
					newNode.synonyms = findSynonyms(newNode);
					if (includeUriPartInSynonyms) {
						newNode.synonyms.add(StringUtils.normalizeFullIRINoSpace(newNode.label));
					}
				}
				nodeList.add(newNode);

			}
		}
		qe.close();
		numberOfClasses = nodeList.size();
		System.out.println("TOTAL CLASSES: " + numberOfClasses);
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
			String synonym = result.get("?o").asLiteral().getValue().toString();
			if (!StringUtils.isUri(synonym)) {
				synonyms.add(StringUtils.normalizeLiteral(synonym));
//				System.out.println("found synonym: " + synonym);
			}
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

	public List<Edge> findMembersOfClass(Node node) {
		String className = node.label;
		ArrayList<Edge> members = new ArrayList<>();
		String queryString = membersQuery.replace("$CLASS$", "<" + className + ">");
		try {
			Query query = QueryFactory.create(queryString);
			QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
			ResultSet res = queryExecution.execSelect();
			int numRes = 0;

			while (res.hasNext()) {
				QuerySolution sol = res.next();
				String currentMemberString = sol.get("?e").toString();
				Node memberNode = new Node(currentMemberString);
				Edge memberEdge = new Edge("hasMember");
				memberEdge.inNode = node;
				memberEdge.outNode = memberNode;
				members.add(memberEdge);
			}
			return members;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("The query that failed was: " + queryString);
			return null;
		}
	}

	public void writeToFile(List<List<Element>> walks, BufferedWriter writer) {

		writeBufferLock.lock();
		try {
			processedClasses += walks.size();
			if (processedClasses >= numberOfClasses) {
				processedClasses -= numberOfClasses;
				currentWalkNumber++;
			}
			log.info("Finished: walk number: " + currentWalkNumber);
			while (!walks.isEmpty()) {
				try {
					List<Element> walk = walks.remove(0);
					String str = NodeGraph.walk2String(walk, outputFormat);

					// must split the string into the two components
					if (outputFormat.toLowerCase().equals("twodocuments")) {
						String[] parts = str.split("\n");
						List<String[]> tokens = Arrays.stream(parts).map(p -> p.split("->"))
								.collect(Collectors.toList());
						str = tokens.stream().map(t -> t[0]).collect(Collectors.joining(" "));
//						System.out.println("normal writer: " + str);
						String labelstr = tokens.stream().map(t -> t[1]).collect(Collectors.joining(" "));
//						System.out.println("label writer: " + labelstr);
						secondOutputWriter.write(labelstr + "\n");
					}

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
		List<List<Element>> writeBuffer;

		public FileWriter(BufferedWriter writer, List<List<Element>> writeBuffer) {
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
				if (outputFormat.toLowerCase().equals("twodocuments")) {
					writeToFile(writeBuffer, secondOutputWriter);
				}
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
				List<List<Element>> walks = new ArrayList<>();
				for (Node node : graph.getNodeList()) {
					List<Element> walk = graph.createWalks(node, walkDepth);
					walks.add(walk);
				}
				writeToFile(walks, outputWriter);
			}
			threadFinished();
//			System.out.println("Thread: " + index + " finished");
		}

	}

	public static void main(String[] args) throws Exception {
		long startTime = System.nanoTime();

//		addProteinInteractions("/home/ole/master/bio_data/go2.owl");

		System.out.println("starting projection");

//		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/pizza.owl");
//		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/NTNames.owl");
//		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/foaf.rdf");
//		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/human.owl");
//		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/bio_data/go.owl");
		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/ekaw.owl");
		projector.projectOntology();
		projector.saveModel(TestRunUtils.modelPath);
		System.out.println("starting walksgenerator");
//		org.eclipse.rdf4j.model.Model rdf4jModel = projector.getModel();

//		SecondOrderWalksGenerator(String inputFile, String outputFile, int numberOfThreads, int walkDepth,
//		int limit, int numberOfWalks, int offset, int p, int q)

		SecondOrderWalksGenerator walks = new SecondOrderWalksGenerator(TestRunUtils.modelPath,
				"/home/ole/master/test_onto/walks_out.txt", "/home/ole/master/test_onto/labels_out.txt", 12, 40, 100000,
				100, 0, 1, 1, "fulluri", false, true);
//		walks.useRdf4jModel(rdf4jModel);
		walks.generateWalks();

//		Walks walks = new Walks(TestRunUtils.modelPath, "secondorder");
//		walks.generateWalks();

		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1000000;
		System.out.println("duration: " + duration);
	}

	public static void addProteinInteractions(String ontoPath) throws Exception {

		OntologyReader reader = new OntologyReader();
		reader.setFname(ontoPath);
		reader.readOntology();
		OWLOntology onto = reader.getOntology();
		IRI ontoIRI = onto.getOntologyID().getDefaultDocumentIRI().get();
		OWLDataFactory df = OWLManager.getOWLDataFactory();

		OWLObjectProperty hasFunctionProperty = df.getOWLObjectProperty(IRI.create(ontoIRI + "#hasFunction"));
		PrefixManager pm = new DefaultPrefixManager("http://yeast#");
//		pm.setPrefix("GO", "http://purl.obolibrary.org/obo/GO_");

		BufferedReader csvReader = new BufferedReader(
				new FileReader("/home/ole/workspace/MatcherWithWordEmbeddings/py/bio_data_processing/yeast_out.csv"));
		String line = "";
		while ((line = csvReader.readLine()) != null) {
			String protein;
			String go;

			if (line.charAt(0) == '"') { // compound name
				String regex = "\"(.*)\",(.*)";
				Pattern pattern = Pattern.compile(regex);
				Matcher matcher = pattern.matcher(line);
				matcher.matches();
				protein = matcher.group(1);
				go = matcher.group(2);
			} else {
				protein = line.split(",")[0];
				go = line.split(",")[1];
			}

			if (protein.contains("|")) {
				boolean found = false;
				String[] parts = protein.split("\\|");
				String regex = "(Q.{4})|(Y.{6}(.{2})?)";
				Pattern pattern = Pattern.compile(regex);

				for (String part : parts) {
					Matcher matcher = pattern.matcher(part);
					if (matcher.matches()) {
						protein = part;
						found = true;
						break;
					}
				}

				if (!found) {
					System.out.println("not found: " + protein);
					System.out.println("in parts: ");
					for (String part : parts) {
						System.out.println(part);
					}
					continue;
				}
			}
			OWLNamedIndividual proteinIndividual = df.getOWLNamedIndividual(protein, pm);
			OWLClass goClass = df.getOWLClass(go, pm);
			OWLAxiom ax = df.getOWLClassAssertionAxiom(goClass, proteinIndividual);
//			System.out.println(ax);
			onto.getOWLOntologyManager().addAxiom(onto, ax);
		}
		OntologyReader.writeOntology(onto, "file:" + ontoPath, "rdf");
		System.out.println("Written ontology: " + ontoPath);
	}

}