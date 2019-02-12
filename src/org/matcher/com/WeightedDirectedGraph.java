package org.matcher.com;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class WeightedDirectedGraph {
	List<Node> nodes;
	List<Edge> edges;
	Node head;
	int walkDepth;
	int RANDOM_JUMP_WEIGHT = 1;
	String[] SYNONYM_EDGES = { "http://www.w3.org/2000/01/rdf-schema#label",
			"http://www.w3.org/2000/01/rdf-schema#comment" };

	public WeightedDirectedGraph() {
		nodes = new ArrayList<>();
		edges = new ArrayList<>();
	}

	public WeightedDirectedGraph(Node node) {
		nodes = new ArrayList<>();
		edges = new ArrayList<>();
		this.head = node;
	}

	/** print the graph to console, depth first **/
	public void printGraph() {
		if (head != null) {
			printGraph(head, 0, "");
		}
	}

	/** Recursive method, print the paths of the graph to console **/
	public void printGraph(Node node, int level, String path) {
		path += replaceNamespaces(node.label + " - > ");
		if (node.edges != null && level < walkDepth) {
			for (Edge e : node.edges) {
				path += replaceNamespaces(e.label + "(" + e.weight + ") -> ");
				for (Node n : e.outNodes) {
					printGraph(n, level + 1, path);
				}
			}
		} else {
			System.out.println(path);
		}
	}

	public String replaceNamespaces(String uri) {
		uri = uri.replace("http://www.w3.org/2002/07/owl#", "owl:");
		uri = uri.replace("http://www.w3.org/2000/01/rdf-schema#", "rdfs:");
		uri = uri.replace("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:");
		return uri;
	}

	/** choosing a random edge of a list of edges using the weights */
	public Edge chooseRandomEdge(List<Edge> edgeList) {
		int totWeight = 0;
		Edge chosenEdge = null;
		if (edgeList != null) {
			for (Edge e : edgeList) {
				totWeight += e.weight;
			}
		}

		totWeight += RANDOM_JUMP_WEIGHT;
		Random ran = new Random();
		int randomNumber = ran.nextInt(totWeight);

		int accWeight = 0;
		if (edgeList != null) {
			for (Edge e : edgeList) {
				accWeight += e.weight;
				if (randomNumber <= accWeight) {
					chosenEdge = e;
					break;
				}
			}
		}
		if (chosenEdge == null) {
//			System.out.println("RANDOM EDGE: ");
			chosenEdge = chooseRandomEdgeWithoutWeights(edges);
//			System.out.println(chosenEdge.label);
		}
		return chosenEdge;
	}

	public Edge chooseRandomEdgeWithoutSynonyms(List<Edge> edgeList) {
		int totWeight = 0;
		Edge chosenEdge = null;
		if (edgeList != null) {
			for (Edge e : edgeList) {
				boolean isSynonymEdge = Arrays.stream(SYNONYM_EDGES).anyMatch(e.label::equals);
				if (!isSynonymEdge) {
					totWeight += e.weight;
				}
			}
		}

		totWeight += RANDOM_JUMP_WEIGHT;
		Random ran = new Random();
		int randomNumber = ran.nextInt(totWeight);

		int accWeight = 0;
		if (edgeList != null) {
			for (Edge e : edgeList) {
				boolean isSynonymEdge = Arrays.stream(SYNONYM_EDGES).anyMatch(e.label::equals);
				if (!isSynonymEdge) {
					accWeight += e.weight;
					if (randomNumber <= accWeight) {
						chosenEdge = e;
						break;
					}
				}
			}
		}
		if (chosenEdge == null) {
//			System.out.println("RANDOM EDGE: ");
			chosenEdge = chooseRandomEdgeWithoutWeightsAndSynonyms(edges);
//			System.out.println(chosenEdge.label);
		}
		return chosenEdge;
	}

	public Edge chooseRandomEdgeWithoutWeightsAndSynonyms(List<Edge> edgeList) {
		int numEdges = 0;

		for (Edge e : edgeList) {
			boolean isSynonymEdge = Arrays.stream(SYNONYM_EDGES).anyMatch(e.label::equals);
			if (!isSynonymEdge) {
				numEdges++;
			}
		}

		Edge chosenEdge = null;
		Random ran = new Random();
		int randomNumber = ran.nextInt(numEdges);
		int i = 0;
		for (Edge e : edgeList) {
			boolean isSynonymEdge = Arrays.stream(SYNONYM_EDGES).anyMatch(e.label::equals);
			if (!isSynonymEdge) {
				if (i == randomNumber) {
					chosenEdge = e;
					break;
				}
				i++;
			}
		}
		return chosenEdge;
	}

	public Edge chooseRandomEdgeWithoutWeights(List<Edge> edgeList) {
		int numEdges = edgeList.size();
		Edge chosenEdge = null;
		Random ran = new Random();
		int randomNumber = ran.nextInt(numEdges);
		int i = 0;
		for (Edge e : edgeList) {
			if (i == randomNumber) {
				chosenEdge = e;
				break;
			}
			i++;
		}
		return chosenEdge;
	}

	public Node chooseRandomNode(List<Node> nodeList) {
		int numNodes = nodeList.size();
		if (numNodes == 0) {
			return chooseRandomNode(nodes); // if empty return any node from the graph
		}
		Node chosenNode = null;
		Random ran = new Random();
		int randomNumber = ran.nextInt(numNodes);
		int i = 0;
		for (Node n : nodeList) {
			if (i == randomNumber) {
				chosenNode = n;
				break;
			}
			i++;
		}
		return chosenNode;
	}

	public String generateRandomWalk() {
		if (head != null) {
			return generateRandomWalkWithSynonyms(head, 1);
		} else {
			return null;
		}
	}

	public String generateRandomWalk(Node node, int level) {
		String tmpWalk = "";
		tmpWalk += replaceNamespaces(node.label) + " ";
		if (level < walkDepth) {
			Edge nextEdge = chooseRandomEdge(node.edges);
			Node nextNode = chooseRandomNode(nextEdge.outNodes);
			tmpWalk += replaceNamespaces(nextEdge.label) + " ";
			tmpWalk += generateRandomWalk(nextNode, level + 1);
		}
		return tmpWalk;
	}

	/** substitutes the uris for labels randomly, does not include label/comment in the walks **/
	public String generateRandomWalkWithSynonyms(Node node, int level) {
		String tmpWalk = "";
		tmpWalk += replaceNamespaces(node.getSomeName()) + " ";
		if (level < walkDepth) {
			Edge nextEdge = chooseRandomEdgeWithoutSynonyms(node.edges);
			Node nextNode = chooseRandomNode(nextEdge.outNodes);
			tmpWalk += replaceNamespaces(nextEdge.label) + " ";
			tmpWalk += generateRandomWalkWithSynonyms(nextNode, level + 1);
		}
		return tmpWalk;
	}

	public boolean isEmpty() {
		return head == null;
	}

	public boolean contains(String nodeName) {
		if (isEmpty()) {
			return false;
		}

		boolean contains = false;

		for (Node n : nodes) {
			if (n.label.equals(nodeName)) {
				contains = true;
				break;
			}
		}
		return contains;
	}

	/**
	 * finds and returns a Node object from the graph, if it does not exist it
	 * returns null
	 **/
	public Node find(String nodeName) {
		if (isEmpty()) {
			return null;
		}

		Node theNode = null;

		for (Node n : nodes) {
			if (n.label.equals(nodeName)) {
				theNode = n;
				break;
			}
		}
		return theNode;
	}

	public void printNodes() {
		System.out.println("The graph for: " + head.label + " has " + nodes.size() + " nodes: ");
		for (Node n : nodes) {
			System.out.println(n.label);
		}
	}

	public List<String> findSynonyms() {
		Node node = head;
		ArrayList<String> synonyms = new ArrayList<>();

		for (Edge e : node.edges) {
			boolean isSynonymEdge = Arrays.stream(SYNONYM_EDGES).anyMatch(e.label::equals);
			if (isSynonymEdge) {
				for (Node n : e.outNodes) {
					synonyms.add(n.label);
				}
			}
		}

		return synonyms;
	}

	public void printSynonyms() {
		List<String> synonyms = findSynonyms();
		System.out.println("Synonyms for " + head.label + " " + synonyms.size() + " stk");
		synonyms.forEach(System.out::println);
	}
}
