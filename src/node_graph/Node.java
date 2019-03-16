package node_graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import mappings.utils.StringUtils;

public class Node {
	public String label;
	public Set<Edge> edges;
	public List<String> synonyms;

	public Node(String label) {
		this.label = label;
		edges = new HashSet<>();
	}

	public List<String> findSynonyms() {
		return new ArrayList<String>();
	}

	/** assuming we have synonyms, return any of the names **/
	public String getSomeName(boolean includeURI) {
		int numSynonyms = synonyms.size();
		Random randomNumberGenerator = new Random();
		int randomIndex = randomNumberGenerator.nextInt(numSynonyms);
		String synonym = synonyms.get(randomIndex);
		if (!includeURI) {
			if (StringUtils.isUri(synonym)) { // not to include the URI
				return getSomeName(includeURI);
			}
		}
		return synonym;
	}

	/**
	 * returning a string representation of a Node
	 *
	 */
	public String toString() {
//		todo: IMPLEMENT multiple ways to represent the node uri/text-part multiple words etc
		return label;
	}

	public String getUriPart() {
		return StringUtils.normalizeFullIRINoSpace(label);
	}

	public String getUriWords() {
		return StringUtils.normalizeFullIRI(label);
	}

	public String getOneSynonym() {
		if (!synonyms.isEmpty()) {
			Random random = new Random();
			int pos = random.nextInt(synonyms.size());
			return synonyms.get(pos);
		} else {
			return label;
		}
	}

	public String getAllSynonyms() {
		if (!synonyms.isEmpty()) {
			StringBuilder strs = new StringBuilder();
			for (String s : synonyms) {
				strs.append(s + " ");
			}
			return strs.toString();
		} else {
			return label;
		}
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
