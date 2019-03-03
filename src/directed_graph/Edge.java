package directed_graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mappings.utils.StringUtils;

public class Edge {
	int SUBCLASS_WEIGHT = 10; // 5
	int SUPERCLASS_WEIGHT = 0;
	int DISJOINT_CLASS_WEIGHT = 0;
	int A_SUB_R_SOME_B_WEIGHT = 0;
	int A_SUB_R_ONLY_B_WEIGHT = 0;
	int INVERSE_OF_WEIGHT = 0;
	int RANGE_DOMAIN_WEIGHT = 0;
	int TYPE_WEIGHT = 0; // 5?
	int NORMAL_PROPERTY_WEIGHT = 0;
	int RANDOM_JUMP_WEIGHT = 0; // 1
	
	public String label;
	public List<Node> outNodes;
	public int weight;
	List<String> synonyms;
	
	public Edge(String label) { // todo: add subRsome/only
		this.label = label;
		if (label.equals("http://www.w3.org/2000/01/rdf-schema#subClassOf")) {
			weight = SUBCLASS_WEIGHT;
		} else if (label.equals("http://www.w3.org/2002/07/owl#disjointClass")) {
			weight = DISJOINT_CLASS_WEIGHT;
		} else if (label.equals("http://www.w3.org/2000/01/rdf-schema#inverseOf")) {
			weight = INVERSE_OF_WEIGHT;
		} else if (label.equals("http://www.w3.org/2000/01/rdf-schema#domain") || label.equals("rdfs:range")) {
			weight = RANGE_DOMAIN_WEIGHT;
		} else if (label.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
			weight = TYPE_WEIGHT;
		} else if (label.equals("http://www.w3.org/2000/01/rdf-schema#superClassOf")) {
			weight = SUPERCLASS_WEIGHT;
		} else { // other  
			weight = NORMAL_PROPERTY_WEIGHT;
		}
	}
	
	public List<String> findSynonyms() {
		ArrayList<String> synonyms = new ArrayList<>();
		synonyms.add(label);
		synonyms.add(StringUtils.normalizeFullIRI(label)); // also adding the plane text representation of the URI
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
