package directed_graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import mappings.utils.StringUtils;

public class WeightedDirectedGraph {
	public List<Node> nodes;
	public List<Edge> edges;
	public Node head;
	public int walkDepth;
	int RANDOM_JUMP_WEIGHT = 0;
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
		return StringUtils.replaceNamespaces(uri);
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
		if (totWeight > 0) {
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
		}
		if (chosenEdge == null) { // if edge is not found:
//			System.out.println("RANDOM EDGE: ");
//			chosenEdge = chooseRandomEdgeWithoutWeights(edges);
			return null;
//			System.out.println(chosenEdge.label);
		}
		return chosenEdge;
	}

	/* choosing a random edge, but avoiding comment, label etc */
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
		if (totWeight > 0) {
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
		}
		if (chosenEdge == null) { // if no edge was found
//			System.out.println("RANDOM EDGE: ");
//			chosenEdge = chooseRandomEdgeWithoutWeightsAndSynonyms(edges);
			return null;
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
		if (numEdges == 0) {
			return null;
		}
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
			return generateRandomWalk(head, 1);
		} else {
			return null;
		}
	}

	public String generateRandomWalkWithSynonyms() {
		if (head != null) {
			return generateRandomWalkWithSynonyms(head, 1);
		} else {
			return null;
		}
	}

	public String generateLabelRandomWalk() {
		if (head != null) {
			return generateLabelRandomWalk(head, 1);
		} else {
			return null;
		}
	}

	public List<String> generateAllSubClassWalks() {
		if (head != null) {
			ArrayList<String> allSubClassWalks = new ArrayList<>();
			String tmp = replaceNamespaces(head.label) + " ";
//			String tmp = StringUtils.normalizeFullIRINoSpace(head.label) + " ";
			for (Edge e : head.edges) {
				int numSubClasses = 0;
				if (e.label.equals("http://www.w3.org/2000/01/rdf-schema#subClassOf")) {
					for (Node nextNode : e.outNodes) {
//						System.out.println(head.label + "->" + nextNode.label);
//						if (!tmp.endsWith(StringUtils.normalizeFullIRINoSpace("http://www.w3.org/2000/01/rdf-schema#subClassOf"))) {
						if (!tmp.endsWith(replaceNamespaces("http://www.w3.org/2000/01/rdf-schema#subClassOf") + " ")) {
							tmp += replaceNamespaces(e.label) + " ";
//							tmp += StringUtils.normalizeFullIRINoSpace(e.label) + " ";
						}
						numSubClasses++;
						generateSubClassWalk(nextNode, 2, allSubClassWalks, tmp);
					}
//					System.out.println("node: " + head.label + " has " + numSubClasses + " subclasses");
				}
			}
			if (allSubClassWalks.size() == 0) {
//				allSubClassWalks.add(tmp + "owl:Thing"); // using owl:Thing for mark all as top-classes
				allSubClassWalks.add(tmp + "thing");
			}
			return allSubClassWalks;
		} else {
			return null;
		}
	}

	public void generateSubClassWalk(Node node, int level, ArrayList<String> walks, String tmp) {
		String nodeName = replaceNamespaces(node.label);
		if (!tmp.contains(nodeName) || !tmp.contains(StringUtils.normalizeFullIRI(node.label))) { // avoid looping over
																									// the same nodes
			tmp += nodeName + " ";
//			tmp += StringUtils.normalizeFullIRINoSpace(node.label)+ " "; 
			if (level < walkDepth && node.edges != null && node.edges.size() > 0) {
				int numSubClassEdges = 0;
				for (Edge e : node.edges) {
					if (e.label.equals("http://www.w3.org/2000/01/rdf-schema#subClassOf")) {
						for (Node nextNode : e.outNodes) {
							if (!tmp.endsWith(
									replaceNamespaces("http://www.w3.org/2000/01/rdf-schema#subClassOf") + " ")) {
								tmp += replaceNamespaces("http://www.w3.org/2000/01/rdf-schema#subClassOf") + " ";
//								tmp += StringUtils.normalizeFullIRINoSpace("http://www.w3.org/2000/01/rdf-schema#subClassOf") + " ";
							}
							numSubClassEdges++;
							generateSubClassWalk(nextNode, level + 1, walks, tmp);
						}
					}
				}
				if (numSubClassEdges == 0) {
					walks.add(tmp); // has edges, but no subClassEdges --> end
				}
			} else { // either walk depth or no more edges --> end
				walks.add(tmp);
			}
		} else {
//			System.out.println("tmp contains " + nodeName + " tmp: ");
			tmp = tmp.substring(0, tmp.length() - "rdfs:subClassOf ".length());
			walks.add(tmp);
		}
	}

	public String generateRandomWalk(Node node, int level) {
		String tmpWalk = "";
		tmpWalk += replaceNamespaces(node.label) + " ";
		if (level < walkDepth) {
			Edge nextEdge = chooseRandomEdge(node.edges);
			if (nextEdge != null) {
				Node nextNode = chooseRandomNode(nextEdge.outNodes);
//				tmpWalk += replaceNamespaces(nextEdge.label) + " ";
				tmpWalk += generateRandomWalk(nextNode, level + 1);
			} else {
//				System.out.println(level);
//				System.out.println(node.edges);
//				System.out.println(node.label + " does not have edges");
			}
		}
		return tmpWalk;
	}

	/**
	 * substitutes the uris for labels randomly, does not include label/comment in
	 * the walks
	 **/
	public String generateRandomWalkWithSynonyms(Node node, int level) {
		String tmpWalk = "";
		tmpWalk += replaceNamespaces(node.getSomeName(true)) + " ";
		if (level < walkDepth) {
			Edge nextEdge = chooseRandomEdgeWithoutSynonyms(node.edges);
			if (nextEdge == null) {
				return tmpWalk;
			}
			Node nextNode = chooseRandomNode(nextEdge.outNodes);
			tmpWalk += generateRandomWalkWithSynonyms(nextNode, level + 1);
		}
		return tmpWalk;
	}

	/**
	 * generates random walk using only textual representation of the classes, not
	 * including the URIs
	 **/
	public String generateLabelRandomWalk(Node node, int level) {
		String tmpWalk = "";
		tmpWalk += node.getSomeName(false) + " ";
		if (level < walkDepth) {
			Edge nextEdge = chooseRandomEdgeWithoutSynonyms(node.edges);
			if (nextEdge == null) {
				return tmpWalk;
			}
			Node nextNode = chooseRandomNode(nextEdge.outNodes);
			tmpWalk += generateLabelRandomWalk(nextNode, level + 1);
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
		String uriLabel = head.label;
		synonyms.add(uriLabel);
		synonyms.add(StringUtils.normalizeFullIRI(uriLabel));
//		for (String token : StringUtils.removeStopWords(StringUtils.normalizeFullIRI(uriLabel).split(" "))) {
		for (String token : StringUtils.uri2Set(uriLabel)) {
			synonyms.add(token);
		}

		for (Edge e : node.edges) {
			boolean isSynonymEdge = Arrays.stream(SYNONYM_EDGES).anyMatch(e.label::equals);
			if (isSynonymEdge) {
				for (Node n : e.outNodes) {
					String synonymLabel = n.label;
					synonyms.add(synonymLabel);
//					synonyms.add(StringUtils.normalizeFullIRI(synonymLabel));
//					for (String token : StringUtils
//							.removeStopWords(StringUtils.normalizeString(synonymLabel).split(" "))) {
					for (String token : StringUtils.uri2Set(synonymLabel)) {
						synonyms.add(token);
					}
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
