package org.matcher.com;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.trainer.com.WordEmbeddingsTrainer;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.reasoner.Reasoner;
import com.hp.hpl.jena.reasoner.ReasonerRegistry;

// 1) Read the .ttl - file
// 2) Create data model Jena out of convenience (could later upgrade to RDFox)
// 3) Use SPARQL-queries to generate random walks
public class GenerateRandomWalks {
	private String inputFile;
	private String outputFilePath;
	private File outputFile;
	private int walkDepth;
	private int limit;
	private int offset = 0;
	private int numberOfWalks;
	private Dataset dataset;
//	private Model model;
	private OntModel ontModel;
	private Model infModel;
	private String fileType = "TTL";
	private Reasoner reasoner;
	private BufferedWriter writer;
	private String adjacentPropertiesQuery = "SELECT DISTINCT ?p WHERE {$CLASS$ ?p ?o .}";
	private String adjacentClassesQuery = "SELECT DISTINCT ?o WHERE { $CLASS$ $PROPERTY$ ?o . }";

	public GenerateRandomWalks(String in, String outputFilePath, int walkDepth, int limit, int nmWalks) {
		this.inputFile = in;
		this.outputFilePath = outputFilePath;
		this.walkDepth = walkDepth;
		this.limit = limit;
		this.numberOfWalks = nmWalks;
	}

	public void generateWalks() {
		initializeEmptyModel();
		readInputFileToModel();
		prepareReasoner();
		performReasoningOnModel();
		prepareDocumentWriter();
		walkTheGraph();
		closeDocumentWriter();
		System.out.println(infModel.size());
		System.out.println("generated walks");
	}

	public void initializeEmptyModel() {
//		model = ModelFactory.createOntologyModel();
		ontModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
//		ontModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC);
	}

	public void readInputFileToModel() {
		ontModel.read(inputFile, fileType);
	}

	public void prepareReasoner() {
		reasoner = ReasonerRegistry.getOWLReasoner();
//		reasoner = PelletReasonerFactory.theInstance().create();
	}

	public void performReasoningOnModel() {
//		reasoner = reasoner.bindSchema(ontModel);
		infModel = ModelFactory.createInfModel(reasoner, ontModel);
	}

	public void prepareDocumentWriter() {
		try {
			outputFile = new File(outputFilePath);
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath, false), "utf-8"),
					32 * 1024);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			System.out.println("Not found file: " + outputFilePath);
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void closeDocumentWriter() {
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeToFile(List<String> lines) {
		for (String str : lines)
			try {
				writer.write(str + "\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	public void walkTheGraph() {
		ArrayList<String> strs = new ArrayList<>();
		List<String> allClasses = selectAllClasses();
//		executeQuery(generateQuery(), strs);
//		processClass(allClasses.get(0), generateQuery(), strs);
		WeightedDirectedGraph graph = createWeightedDirectedGraph(allClasses.get(1));
		graph.printGraph();
		writeToFile(strs);
	}

	public List<String> selectAllClasses() {
		List<String> allClasses = new ArrayList<String>();

		String queryStrign = "SELECT ?s WHERE { {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> } "
				+ " UNION {?s <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2000/01/rdf-schema#Class> } "
				+ " } " + "OFFSET " + offset + " LIMIT " + limit;

		Query query = QueryFactory.create(queryStrign);

		// Execute the query and obtain results
		QueryExecution qe = QueryExecutionFactory.create(query, infModel);
		ResultSet results = qe.execSelect();

		while (results.hasNext()) {
			QuerySolution result = results.next();
			System.out.println(result.get("s").toString());
			allClasses.add(result.get("s").toString());
		}
		qe.close();
		System.out.println("TOTAL CLASSES: " + allClasses.size());
		return allClasses;
	}

	public void executeQuery(String queryString, List<String> walkList) {
//		String qString = "SELECT ?x ?y ?z WHERE {?x ?y ?z}";
		System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, infModel);
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

	public List<Edge> findAdjacentProperties(String className) {
		ArrayList<Edge> propertyList = new ArrayList<>();
		String queryString = adjacentPropertiesQuery.replace("$CLASS$", "<" + className + ">");
		System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, infModel);
		ResultSet res = queryExecution.execSelect();
		int numRes = 0;

		while (res.hasNext()) {
			QuerySolution sol = res.next();
			String currentProperty = sol.get("?p").toString();
			propertyList.add(new Edge(currentProperty));
		}

		return propertyList;
	}

	public List<Node> findAdjacentClasses(String className, String propertyName) {
		ArrayList<Node> classList = new ArrayList<>();
		String queryString = adjacentClassesQuery.replace("$CLASS$", "<" + className + ">").replace("$PROPERTY$",
				"<" + propertyName + ">");
		System.out.println(queryString);
		Query query = QueryFactory.create(queryString);
		QueryExecution queryExecution = QueryExecutionFactory.create(query, infModel);
		ResultSet res = queryExecution.execSelect();
		int numRes = 0;

		while (res.hasNext()) {
			QuerySolution sol = res.next();
			String currentClass = sol.get("?o").toString();
			classList.add(new Node(currentClass));
		}

		return classList;
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

	private void processClass(String className, String walkQuery, List<String> finalList) {

		// get all the walks
		List<String> tmpList = new ArrayList<String>();
		String queryStr = walkQuery.replace("$CLASS$", "<" + className + ">");
		executeQuery(queryStr, finalList);

		// get all the direct properties
//		queryStr = directPropertiesQuery.replace("$CLASS$", "<" + className + ">");
		executeQuery(queryStr, finalList);
	}
	


	/** Generates an instance of the graph using an initial class **/
	public WeightedDirectedGraph createWeightedDirectedGraph(String firstClassName) {
		Node headNode = new Node(firstClassName);
		WeightedDirectedGraph graph = new WeightedDirectedGraph(headNode);
		graph.nodes.add(headNode);
		graph.depth = walkDepth;
		populateGraph(graph, graph.head, 0);
		return graph;
	}

	/** Recursively populates the graph **/
	public void populateGraph(WeightedDirectedGraph graph, Node currentNode, int level) {
		currentNode.edges = findAdjacentProperties(currentNode.label);
		for (Edge e : currentNode.edges) {
			graph.edges.add(e);
			e.outNodes = findAdjacentClasses(currentNode.label, e.label);
			if (level < walkDepth && e.outNodes != null) {
				for (Node n : e.outNodes) {
					graph.nodes.add(n);
					populateGraph(graph, n, level + 1);
				}
			}
		}
	}

	public static void main(String[] args) {
		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		BasicConfigurator.configure();

		GenerateRandomWalks walks = new GenerateRandomWalks("/home/ole/master/test_onto/merged.ttl",
				"/home/ole/master/test_onto/walks_out.txt", 2, 1000, 100);
		walks.generateWalks();
	}
}
