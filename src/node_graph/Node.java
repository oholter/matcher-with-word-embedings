package node_graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import mappings.utils.StringUtils;

public class Node extends Element {
	public Set<Edge> edges;

	public Node(String label) {
		this.label = label;
		edges = new HashSet<>();
	}

	public List<String> findSynonyms() {
		return new ArrayList<String>();
	}

	/*
	 * Two nodes are equal if they have the same label, thus the same URI
	 */
	public boolean equals(Node n) {
		if (n.label.equals(this.label)) {
			return true;
		} else {
			return false;
		}
	}

}
