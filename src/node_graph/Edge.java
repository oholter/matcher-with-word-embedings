package node_graph;

import java.util.List;

import mappings.utils.StringUtils;

public class Edge extends Element {
	double SUBCLASS_WEIGHT = 1.0; //
	double SUPERCLASS_WEIGHT = 0.7;
	double TYPE_WEIGHT = 0.0; //
	double NORMAL_PROPERTY_WEIGHT = 0.3;
	double RANDOM_JUMP_WEIGHT = 0; //
	double MEMBER_WEIGHT = 0;

	public Node inNode;
	public Node outNode;
	public double weight;

	public Edge(String label) { // todo: add subRsome/only
		this.label = label;
		if (label.equals("http://www.w3.org/2000/01/rdf-schema#subClassOf")) {
			weight = SUBCLASS_WEIGHT;
		} else if (label.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
			weight = TYPE_WEIGHT;
		} else if (label.equals("http://www.w3.org/2000/01/rdf-schema#superClassOf")) {
			weight = SUPERCLASS_WEIGHT;
		} else if (label.equals("hasMember")) {
			weight = MEMBER_WEIGHT;
		} else { // other
			weight = NORMAL_PROPERTY_WEIGHT;
		}
	}

	public String toString() {
		return StringUtils.replaceNamespaces(label);
	}

	/*
	 * Two edges are equal if they have the same label, startnode and endnode
	 */
	public boolean equals(Edge e) {
		if (e.inNode.equals(this.inNode) && e.outNode.equals(this.outNode) && e.label.equals(this.label)) {
			return true;
		} else {
			return false;
		}
		
	}
}
