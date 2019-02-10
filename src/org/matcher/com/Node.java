package org.matcher.com;

import java.util.List;

public class Node {
	public String label;
	public List<Edge> edges;
	
	public Node(String label) {
		this.label = label;
	}
}
