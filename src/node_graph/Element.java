package node_graph;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import mappings.utils.StringUtils;

public abstract class Element {
	public String label;
	public String cache;
	public List<String> synonyms;

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
		return label;
	}

	public String getGoUriPart() {
		if (cache == null) {
			cache = StringUtils.getGoUriPart(label);
		}
		return cache;
	}

	public String getUriPart() {
		if (cache == null) {
			cache = StringUtils.normalizeFullIRINoSpace(label);
		}
		return cache;
	}

	public String getUriPartNoNormalized() {
		if (cache == null) {
			cache = StringUtils.getUriPart(label);
		}
		return cache;
	}

	public String getUriWords() {
		if (cache == null) {
			cache = StringUtils.normalizeFullIRI(label);
		}
		return cache;
	}

	public String getTwoDocumentsFormat() {
		String uri = label;
		String synonym = getOneSynonymAsWords();
		return uri + "->" + synonym;
	}

	public String getOneSynonym() {
		if (synonyms != null && synonyms.size() > 0) {
			Random random = new Random();
			int pos = random.nextInt(synonyms.size());
			return synonyms.get(pos);
		} else {
			return label;
		}
	}

	public String getOneSynonymAsWords() {
		String synonym = getOneSynonym();
		Set<String> stringSet = StringUtils.string2Set(synonym);
		return stringSet.stream().collect(Collectors.joining(" "));
	}

	public String getAllSynonymsAsWords() {
		if (cache == null) {
			String synonyms = getAllSynonyms();
			Set<String> stringSet = StringUtils.string2Set(synonyms);
			cache = stringSet.stream().collect(Collectors.joining(" "));
		}
		return cache;
	}

	public String getAllSynonyms() {
		if (cache == null) {
			if (!synonyms.isEmpty()) {
				StringBuilder strs = new StringBuilder();
				for (String s : synonyms) {
					strs.append(s + " ");
				}
				cache = strs.toString();
			} else {
				cache = label;
			}
		}
		return cache;
	}

	public String getAllSynonymsAndUri() {
		if (cache == null) {
			StringBuilder strs = new StringBuilder();
			for (String s : synonyms) {
				strs.append(s + " ");
			}
			strs.append(StringUtils.normalizeFullIRINoSpace(label) + " ");
			strs.append(label);
			cache = strs.toString();
		}
		return cache;
	}
}
