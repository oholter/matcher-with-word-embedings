package org.matcher.com;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Node {
	public String label;
	public List<Edge> edges;
	public WeightedDirectedGraph graph;
	public List<String> synonyms;

	public Node(String label, WeightedDirectedGraph graph) {
		this.label = label;
		this.graph = graph;
	}

	public List<String> findSynonyms() {
		ArrayList<String> synonyms = new ArrayList<>();
		synonyms.add(label);
		if (edges != null) {
			for (Edge e : edges) {
				if (e.label.equals("http://www.w3.org/2000/01/rdf-schema#label")
						|| e.label.equals("http://www.w3.org/2000/01/rdf-schema#comment")) {
					for (Node n : e.outNodes) {
						synonyms.add(n.label);
					}
				}
			}
		}
		return synonyms;
	}

	/** assuming we have synonyms, return any of the names **/
	public String getSomeName() {
		if (synonyms == null) { // lazy
			synonyms = findSynonyms();
		}
		int numSynonyms = synonyms.size();
		Random randomNumberGenerator = new Random();
		int randomIndex = randomNumberGenerator.nextInt(numSynonyms);
		return synonyms.get(randomIndex);
	}
}
