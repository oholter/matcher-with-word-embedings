package org.matcher.com;

import java.util.List;

public class Edge {
	double SUBCLASS_WEIGHT = 0.6;
	double EQUIVALENT_CLASS_WEIGHT = 1.0;
	double DISJOINT_CLASS_WEIGHT = 0.0;
	double A_SUB_R_SOME_B_WEIGHT = 0.6;
	double A_SUB_R_ONLY_B_WEIGHT = 0.5;
	double INVERSE_OF_WEIGHT = 0.0;
	double RANGE_DOMAIN_WEIGHT = 0.3;
	double TYPE_WEIGHT = 0.2;
	
	
	String label;
	List<Node> outNodes;
	double weight;
	
	public Edge(String label) { // todo: add subRsome/only
		this.label = label;
		if (label.equals("http://www.w3.org/2000/01/rdf-schema#subClassOf")) {
			weight = SUBCLASS_WEIGHT;
		} else if (label.equals("http://www.w3.org/2002/07/owl#equivalentClass")) {
			weight = EQUIVALENT_CLASS_WEIGHT;
		} else if (label.equals("http://www.w3.org/2002/07/owl#disjointClass")) {
			weight = DISJOINT_CLASS_WEIGHT;
		} else if (label.equals("http://www.w3.org/2000/01/rdf-schema#inverseOf")) {
			weight = INVERSE_OF_WEIGHT;
		} else if (label.equals("http://www.w3.org/2000/01/rdf-schema#domain") || label.equals("rdfs:range")) {
			weight = RANGE_DOMAIN_WEIGHT;
		} else if (label.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
			weight = TYPE_WEIGHT;
		} else {
			weight = 0;
		}
	}
}
