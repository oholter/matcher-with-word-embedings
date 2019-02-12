package org.matcher.com;

import java.util.List;

public class Edge {
	int SUBCLASS_WEIGHT = 6;
	int EQUIVALENT_CLASS_WEIGHT = 10;
	int DISJOINT_CLASS_WEIGHT = 0;
	int A_SUB_R_SOME_B_WEIGHT = 6;
	int A_SUB_R_ONLY_B_WEIGHT = 5;
	int INVERSE_OF_WEIGHT = 0;
	int RANGE_DOMAIN_WEIGHT = 3;
	int TYPE_WEIGHT = 2;
	int NORMAL_PROPERTY_WEIGHT = 4;
	int RANDOM_JUMP_WEIGHT = 1;
	
	String label;
	List<Node> outNodes;
	int weight;
	
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
		} else { // other  
			weight = NORMAL_PROPERTY_WEIGHT;
		}
	}
}
