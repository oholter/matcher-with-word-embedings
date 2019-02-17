package directed_graph;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import mappings.utils.StringUtils;

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
		synonyms.add(StringUtils.normalizeFullIRI(label)); // also adding the plane text representation of the URI
		synonyms.add(StringUtils.normalizeLiteral(StringUtils.normalizeFullIRI(label))); // plane text using _ in stead of blank
//		String[] words = StringUtils.normalizeLiteral(StringUtils.normalizeFullIRI(label)).split("");
//		if (words.length > 1) {
//			for (String s : words) {
//				synonyms.add(s);
//			}
//		} gives worse performance
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
//	public String getSomeName() {
//		if (synonyms == null) { // lazy
//			synonyms = findSynonyms();
//		}
//		int numSynonyms = synonyms.size();
//		Random randomNumberGenerator = new Random();
//		int randomIndex = randomNumberGenerator.nextInt(numSynonyms);
//		return synonyms.get(randomIndex);
//	}
	
	/** adding all synonyms every time **/
	public String getSomeName() {
		if (synonyms == null) { // lazy
			synonyms = findSynonyms();
		}
		String returnString = "";
		for (String synonym : synonyms) {
			returnString += synonym + " ";
		}
		return returnString;
	}
}
