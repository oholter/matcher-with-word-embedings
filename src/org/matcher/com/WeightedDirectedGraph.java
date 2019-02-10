package org.matcher.com;

import java.util.ArrayList;
import java.util.List;

public class WeightedDirectedGraph {
	List<Node> nodes;
	List<Edge> edges;
	Node head;
	int depth;

	public WeightedDirectedGraph(Node head) {
		this.head = head;
		nodes = new ArrayList<>();
		edges = new ArrayList<>();
	}

	public void printGraph() {
		if (head != null) {
			printGraph(head, 0);
		}
	}

	public void printGraph(Node node, int level) {
		System.out.println("level: " + level);
		System.out.println("Node: " + replaceNamespaces(node.label) + " -> ");
		System.out.println("has: " + node.edges.size() + " edges");
		if (node.edges != null && level < depth) {
			for (Edge e : node.edges) {
				System.out.println("Edge: " + replaceNamespaces(e.label) + " weight: " + e.weight + " -> ");
				for (Node n : e.outNodes) {
					printGraph(n, level + 1);
				}
			}
		}
	}

	public String replaceNamespaces(String uri) {
		uri = uri.replace("http://www.w3.org/2002/07/owl#", "owl:");
		uri = uri.replace("http://www.w3.org/2000/01/rdf-schema#", "rdfs:");
		uri = uri.replace("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:");
		return uri;
	}
}
