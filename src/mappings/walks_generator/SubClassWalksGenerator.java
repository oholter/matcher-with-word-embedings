/**
 * This WalksGenerator generates all subClass-relations for all entities
 */
package mappings.walks_generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

import directed_graph.Edge;
import directed_graph.Node;
import directed_graph.WeightedDirectedGraph;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.StringUtils;

public class SubClassWalksGenerator extends WalksGenerator {

	protected Dataset dataset;
	protected OntModel ontModel;
	protected Model model;
	protected String fileType = "TTL";
	protected Reasoner reasoner;
	protected BufferedWriter outputWriter;
	protected String adjacentPropertiesQuery;
	protected String adjacentClassesQuery;
	protected String adjacentSubClassOfQuery;
	protected String superClassQuery;
	protected int numberOfProcessedClasses = 0;
	protected List<String> allClasses;
	protected String[] UNDESIRED_CLASSES = { "http://www.w3.org/2002/07/owl#Thing",
			"http://www.w3.org/2002/07/owl#Class" };
	protected String[] UNDESIRED_PROPERTIES = { "http://www.w3.org/2002/07/owl#inverseOf" };
//			"http://www.w3.org/1999/02/22-rdf-syntax-ns#type" };

	public SubClassWalksGenerator(String in, String outputFilePath, int numThreads, int walkDepth, int limit,
			int nmWalks, int offset) {
		super(in, outputFilePath, numThreads, walkDepth, limit, nmWalks, offset);
		this.adjacentSubClassOfQuery = "SELECT DISTINCT ?s WHERE { ?s <http://www.w3.org/2000/01/rdf-schema#subClassOf> $CLASS$ . } LIMIT "
				+ limit;
		this.superClassQuery = "SELECT DISTINCT ?o WHERE { $CLASS$ <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?o . } LIMIT "
				+ limit;
		this.adjacentPropertiesQuery = "SELECT DISTINCT ?p WHERE {$CLASS$ ?p ?o .} LIMIT " + limit;
		this.adjacentClassesQuery = "SELECT DISTINCT ?o WHERE { $CLASS$ $PROPERTY$ ?o . } LIMIT " + limit;

//		cleanDataSet(in);
	}

	public void generateWalks() {
		initializeEmptyModel();
		readInputFileToModel();
		outputWriter = prepareDocumentWriter(outputFilePath);
		walkTheGraph();
		closeDocumentWriter(outputWriter);
		System.out.println("Model size: " + model.size());
		System.out.println("Finished generating walks");
	}

	public void initializeEmptyModel() {
		model = ModelFactory.createDefaultModel();
	}

	public void readInputFileToModel() {
		model.read(inputFile, fileType);
	}

	public BufferedWriter prepareDocumentWriter(String outputFilePath) {
		BufferedWriter writer = null;
		try {
			File outputFile = new File(outputFilePath);
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

	public synchronized void writeToFile(List<String> lines, BufferedWriter writer) {
		numberOfProcessedClasses++;
		if (numberOfProcessedClasses % 10 == 0) {
			System.out.println("Processed: " + numberOfProcessedClasses + " classes");
		}
		for (String str : lines)
			try {
				writer.write(str + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	private class WalkThread implements Runnable {
		ArrayList<String> lines;
		int index;
		int start;
		int end;

		public WalkThread(int index) {
			this.index = index;
			int classesPerThread = allClasses.size() / numberOfThreads;
			int rest = allClasses.size() % numberOfThreads;
			this.start = index * classesPerThread + Math.min(rest, index);
			this.end = (index + 1) * classesPerThread + Math.min(rest, (index + 1));
		}

		public void run() {
			for (int classNum = start; classNum < end; classNum++) {
				lines = new ArrayList<>();
				String className = allClasses.get(classNum);
//				WeightedDirectedGraph graph = createWeightedDirectedGraph(className);
				WeightedDirectedGraph graph = createSubClassGraph(className);
				List<String> subClassWalks = graph.generateAllSubClassWalks();
				for (String subClassWalk : subClassWalks) {
					lines.add(subClassWalk);
				}
				writeToFile(lines, outputWriter);
//				System.out.println("Thread: " + index + " written: " + subClassWalks.size() + " lines");
			}
		}
	}

	public void walkTheGraph() {
		allClasses = selectAllClasses();
		System.out.println("Random walks of depth: " + walkDepth);
		WeightedDirectedGraph g = createWeightedDirectedGraph("http://ekaw#Student");
		g.printGraph();
		g.printNodes();
		g.printSynonyms();

		Thread[] threads = new Thread[numberOfThreads];

		for (int i = 0; i < numberOfThreads; i++) {
			threads[i] = new Thread(new WalkThread(i));
			threads[i].start();
		}

		try {
			for (int i = 0; i < numberOfThreads; i++) {
				threads[i].join();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<String> selectAllClasses() {
		List<String> allClasses = new ArrayList<String>();
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
			if (!result.get("s").isLiteral()) {
				String currentResult = result.get("s").toString();
				if (!currentResult.contains("http://no.sirius.ontology")) {
//					System.out.println(result.get("s").toString());
					allClasses.add(result.get("s").toString());
				}
			}
		}
		qe.close();
		System.out.println("TOTAL CLASSES: " + allClasses.size());
		return allClasses;
	}

	public void executeQuery(String queryString, List<String> walkList) {
//		String qString = "SELECT ?x ?y ?z WHERE {?x ?y ?z}";
//		System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		ResultSet res = queryExecution.execSelect();
		int numRes = 0;
		while (res.hasNext()) {
			String singleWalk = "";
			QuerySolution sol = res.next();
			for (String var : res.getResultVars()) {
				if (sol.get(var) != null && sol.get(var).isLiteral()) {
					String currentValue = sol.getLiteral(var).toString();
					currentValue = currentValue.replace("\n", " ").replace("\t", " ");
					singleWalk += currentValue + " ";
				} else if (sol.get(var) != null) {
					String currentValue = sol.get(var).toString()
							.replace("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:")
							.replace("http://www.w3.org/2000/01/rdf-schema#", "rdfs:");
					currentValue = currentValue.replace("\n", " ").replace("\t", " ");
					singleWalk += currentValue + "->";
				}
			}
			numRes++;
			if (numRes % 1000 == 0) {
				System.out.println("Generated: " + numRes);
			}
			walkList.add(singleWalk);
		}
	}

	public static void cleanDataSet(String rdfFile) {
		Model m = ModelFactory.createDefaultModel();
		m.read(rdfFile);
		String qString = "DELETE {?s ?p ?o} WHERE {?s ?p ?o "
				+ "FILTER ( strstarts(str(?s), \"http://no.sirius.ontology/\") ||"
				+ "strstarts(str(?p), \"http://no.sirius.ontology/\") || "
				+ "strstarts(str(?q), \"http://no.sirius.ontology/\") )" + "}";

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

	public List<Node> findSuperClassesOf(String className, WeightedDirectedGraph graph) {
		ArrayList<Edge> propertyList = new ArrayList<>();
		if (!StringUtils.isUri(className)) { // could be a literal
			return null;
		}
		String queryString = superClassQuery.replace("$CLASS$", "<" + className + ">");
		ArrayList<Node> classList = new ArrayList<>();
//		System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		ResultSet res = queryExecution.execSelect();
		int numRes = 0;

		while (res.hasNext()) {
			QuerySolution sol = res.next();
			RDFNode currentNode = sol.get("?o");
			String currentClass = sol.get("?o").toString();
//			if (!StringUtils.isUri(currentClass)) {
//				currentClass = normalizeLiteral(currentClass);
////				System.out.println("Not uri: " + currentClass);
//			}
			boolean undesiredClass = Arrays.stream(UNDESIRED_CLASSES).anyMatch(currentClass::equals);
			if (!undesiredClass) {
				Node nodeAlreadyInGraph = graph.find(currentClass);
				if (nodeAlreadyInGraph == null) { // a new node
					classList.add(new Node(currentClass, graph));
				} else {
					classList.add(nodeAlreadyInGraph);
				}
			} else {
//				System.out.println("Found undesired class: " + currentClass);
			}
		}
		return classList;
	}

	public List<Edge> findAdjacentProperties(String className) {
		ArrayList<Edge> propertyList = new ArrayList<>();
		if (!StringUtils.isUri(className)) { // could be a literal
			return null;
		}
		String queryString = adjacentPropertiesQuery.replace("$CLASS$", "<" + className + ">");
//		System.out.println(queryString);
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
					propertyList.add(new Edge(currentProperty));
				}
			}

			return propertyList;
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("The query that failed was: " + queryString);
			return null;
		}
	}

	public String normalizeLiteral(String literal) {
		return StringUtils.normalizeLiteral(literal);
	}

	public List<Node> findAdjacentClasses(String className, String propertyName, WeightedDirectedGraph graph) {
		ArrayList<Node> classList = new ArrayList<>();
		String queryString = adjacentClassesQuery.replace("$CLASS$", "<" + className + ">").replace("$PROPERTY$",
				"<" + propertyName + ">");
//		System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		ResultSet res = queryExecution.execSelect();
		int numRes = 0;

		while (res.hasNext()) {
			QuerySolution sol = res.next();
			RDFNode currentNode = sol.get("?o");
			String currentClass = sol.get("?o").toString();
			if (!StringUtils.isUri(currentClass)) {
				currentClass = normalizeLiteral(currentClass);
//				System.out.println("Not uri: " + currentClass);
			}
			boolean undesiredClass = Arrays.stream(UNDESIRED_CLASSES).anyMatch(currentClass::equals);
			if (!undesiredClass) {
				Node nodeAlreadyInGraph = graph.find(currentClass);
				if (nodeAlreadyInGraph == null) { // a new node
					classList.add(new Node(currentClass, graph));
				} else {
					classList.add(nodeAlreadyInGraph);
				}
			} else {
//				System.out.println("Found undesired class: " + currentClass);
			}
		}

		return classList;
	}

	public List<Node> findSubClassesOf(String className, WeightedDirectedGraph graph) {
		ArrayList<Node> nodeList = new ArrayList<>();
		String queryString = adjacentSubClassOfQuery.replace("$CLASS$", "<" + className + ">");
//		System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, model);
		ResultSet res = queryExecution.execSelect();
		int numRes = 0;

		while (res.hasNext()) {
			QuerySolution sol = res.next();
			RDFNode currentNode = sol.get("?s");
			String currentClass = sol.get("?s").toString();
			boolean undesiredClass = Arrays.stream(UNDESIRED_CLASSES).anyMatch(currentClass::equals);
			if (!undesiredClass) {
				Node nodeAlreadyInGraph = graph.find(currentClass);
				if (nodeAlreadyInGraph == null) { // a new node
					nodeList.add(new Node(currentClass, graph));
				} else {
					nodeList.add(nodeAlreadyInGraph);
				}
			} else {
//				System.out.println("Found undesired class: " + currentClass);
			}
		}
//		for (Node node : nodeList) {
//			System.out.println(node.label);
//		}
		return nodeList;
	}

	/**
	 * Generates query with desired depth
	 **/
	public String generateQuery() {
		String selectPart = "SELECT ?p ?o1";
		String mainPart = "{ $CLASS$ ?p ?o1  ";
		String query = "";
		for (int i = 1; i < walkDepth; i++) {
			mainPart += ". ?o" + i + " ?p" + i + "?o" + (i + 1);
			selectPart += " ?p" + i + "?o" + (i + 1);
		}
		query = selectPart + " WHERE " + mainPart + "} LIMIT 1000";

		return query;
	}

	/** This will create a graph containing only the subClassOf relationships */
	public WeightedDirectedGraph createSubClassGraph(String firstClassName) {
		WeightedDirectedGraph graph = new WeightedDirectedGraph();
		Node headNode = new Node(firstClassName, graph);
		graph.head = headNode;
		graph.nodes.add(headNode);
		graph.walkDepth = walkDepth;

		populateSubClassGraph(graph, headNode, 0);

		return graph;
	}

	/** Generates an instance of the graph using an initial class **/
	public WeightedDirectedGraph createWeightedDirectedGraph(String firstClassName) {
		WeightedDirectedGraph graph = new WeightedDirectedGraph();
		Node headNode = new Node(firstClassName, graph);
		graph.head = headNode;
		graph.nodes.add(headNode);
		graph.walkDepth = walkDepth;

		populateGraph(graph, headNode, 0);

		// find and add subClasses of head node
		if (graph.head.edges != null) {
			List<Node> subClassNodes = findSubClassesOf(firstClassName, graph);
			for (Node subClassNode : subClassNodes) {
				graph.nodes.add(subClassNode); // in the graphs global node list
				Edge superClassOfEdge = new Edge("http://www.w3.org/2000/01/rdf-schema#superClassOf");
				graph.edges.add(superClassOfEdge); // in the graphs global edge list
				List<Node> outNodes = new ArrayList<>();
				outNodes.add(subClassNode);
				superClassOfEdge.outNodes = outNodes; // subClassNode at the end of the edge
				graph.head.edges.add(superClassOfEdge); // added the edge from the head node to the subClassNode
			}
		}

		return graph;
	}

	public void populateSubClassGraph(WeightedDirectedGraph graph, Node currentNode, int level) {
		List<Node> superClassNodes = findSuperClassesOf(currentNode.label, graph);

		if (superClassNodes == null) {
			return; // no more superclasses terminate
		}
		currentNode.edges = new ArrayList<Edge>();
		Edge subClassEdge = new Edge("http://www.w3.org/2000/01/rdf-schema#subClassOf");
		currentNode.edges.add(subClassEdge);

		subClassEdge.outNodes = superClassNodes;
		if (level < walkDepth) {
			for (Node n : subClassEdge.outNodes) {
				boolean undesiredClass = Arrays.stream(UNDESIRED_CLASSES).anyMatch(n.label::equals);
				if (!undesiredClass) {
					if (!graph.contains(n.label)) {
						graph.nodes.add(n); // adding to the graphs node list
					}
					populateSubClassGraph(graph, n, level + 1);
				} else {
//					System.out.println("found undesired class: " + n.label);
				}
			}
		}

	}

	/** Recursively populates the graph including superClass relations **/
	public void populateGraph(WeightedDirectedGraph graph, Node currentNode, int level) {
		currentNode.edges = findAdjacentProperties(currentNode.label);

		List<Node> subClassNodes = findSubClassesOf(currentNode.label, graph);
		for (Node subClassNode : subClassNodes) {
			graph.nodes.add(subClassNode); // in the graphs global node list
			Edge superClassOfEdge = new Edge("http://www.w3.org/2000/01/rdf-schema#superClassOf");
			graph.edges.add(superClassOfEdge); // in the graphs global edge list
			List<Node> outNodes = new ArrayList<>();
			outNodes.add(subClassNode);
			superClassOfEdge.outNodes = outNodes; // subClassNode at the end of the edge
			currentNode.edges.add(superClassOfEdge); // added the edge from the current node to the subClassNode
		}

		if (currentNode.edges != null) {
			for (Edge e : currentNode.edges) {
				graph.edges.add(e);
				e.outNodes = findAdjacentClasses(currentNode.label, e.label, graph);
				if (level < walkDepth && e.outNodes != null) {
					for (Node n : e.outNodes) {
						boolean undesiredClass = Arrays.stream(UNDESIRED_CLASSES).anyMatch(n.label::equals);
						if (!undesiredClass) {
							Node nodeAlreadyInGraph = graph.find(n.label);
							if (nodeAlreadyInGraph == null) { // this is a new node
								graph.nodes.add(n);
								populateGraph(graph, n, level + 1);
							} else {
								// nothing
							}
						} else {
//							System.out.println("found undesired class: " + n.label);
						}
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		BasicConfigurator.configure();

//		RandomWalksGenerator(String inputFile, String outputFile, int numberOfThreads, int walkDepth,
//		int limit, int numberOfWalks, int offset, int childLimit)
		SubClassWalksGenerator walks = new SubClassWalksGenerator("/home/ole/master/test_onto/merged.ttl",
				"/home/ole/master/test_onto/walks_out.txt", 8, 3, 100, 100, 0);
		walks.generateWalks();
	}
}
