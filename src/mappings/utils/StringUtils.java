package mappings.utils;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import opennlp.tools.stemmer.PorterStemmer;

public class StringUtils {
	
	// http://ekaw#ConferenceParticipant -> conference participant
	public static String normalizeFullIRI(String s) {
		String httpPattern = "^(http|https)://.*#";
		s = s.replaceAll(httpPattern, "");
		return normalizeIRI(s); // replace(" ", "_");
	}
	
	public static String getGoUriPart(String s) {
		String httpPattern = "^(http|https)://.*#";
		String goPattern = "http://purl.obolibrary.org/obo/";
		return s.replace(httpPattern, "").replace(goPattern, "");
	}
	
	public static String normalizeFullIRINoSpace(String s) {
		return normalizeFullIRI(s).replaceAll(" ", "_");
	}

	/* from the python nltk.corpus.stopwords */
	public static String[] stopWords = {
			"ourselves", "hers", "between", "yourself", "but", "again", "there",
			"about", "once", "during", "out", "very", "having", "with", 
			"they", "own", "an", "be", "some", "for", "do", "its", "yours", 
			"such", "into", "of", "most", "itself", "other", "off", "is", "s", 
			"am", "or", "who", "as", "from", "him", "each", "the", "themselves", 
			"until", "below", "are", "we", "these", "your", "his", 
			"through", "don", "nor", "me", "were", "her", "more", "himself", 
			"this", "down", "should", "our", "their", "while", "above", "both", 
			"up", "to", "ours", "had", "she", "all", "no", "when", "at", "any", 
			"before", "them", "same", "and", "been", "have", "in", "will", "on", 
			"does", "yourselves", "then", "that", "because", "what", "over", "why", 
			"so", "can", "did", "not", "now", "under", "he", "you", "herself", "has", 
			"just", "where", "too", "only", "myself", "which", "those", "i", 
			"after", "few", "whom", "t", "being", "if", "theirs", "my", "against", 
			"a", "by", "doing", "it", "how", "further", "was", "here", "than"
	};

	public static Set<String> uri2Bag(String uri) {
		Set<String> bag = new HashSet<>();
		String normUri = normalizeFullIRI(uri);
		String[] normWords = normUri.split(" ");
		normWords = removeStopWords(normWords);
		normWords = stemming(normWords);
		for (String word : normWords) {
			bag.add(word);
		}
		return bag;
	}
	
	public static Set<String> string2Set(String string) {
		Set<String> set = new HashSet<>();
		String normString = normalizeString(string);
		String[] strings = normString.split(" ");
		strings = removeStopWords(strings);
		strings = stemming(strings);
		for (String word : strings) {
			set.add(word);
		}
		return set;
	}
	
	public static String[] stemming(String[] strs) {
		ArrayList<String> newList = new ArrayList<>();
		for (String s : strs) {
			newList.add(stemming(s));
		}
		return newList.toArray(new String[newList.size()]);
	}
	
	public static String stemming(String string) {
		PorterStemmer stemmer = new PorterStemmer();
		String stemmedString = stemmer.stem(string);
		return stemmedString;
	}
	
	public static List<String> removeStopWords(List<String> bag) {
		for (String stopWord : stopWords) {
			bag.remove(stopWord);
		}
		return bag;
	}
	
	public static String[] removeStopWords(String[] bag) {
		ArrayList<String> newList = new ArrayList<>();
			for (String b : bag) {
				boolean isStopWord = false;
				for (String stopWord : stopWords) {
					if (b.equals(stopWord)) {
						isStopWord = true;
					}
			}
				if (!isStopWord) {
					newList.add(b);
				}
		}
		return newList.toArray(new String[newList.size()]);
	}

	// ConferenceParticipant -> conference participant
	// conference_participant -> conference participant
	public static String normalizeIRI(String s) {
		if (s != null) {
			s = s.replaceAll(String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])",
					"(?<=[A-Za-z])(?=[^A-Za-z])"), " "); // mixedCase -> multiple words
			s = s.toLowerCase(); // case normalization
			s = s.replaceAll("[_-]", " "); // link normalization
			s = s.replaceAll("[1-9.,]", ""); // remove numbers and punctuation
			s = s.replaceAll("\\s", " "); // blank normalization
			s = s.replaceAll("\\s+", " "); // only one blank
			s = s.trim();
		}
		return s;
	}

	public static String normalizeString(String s) {
		if (s != null) {
			s = s.toLowerCase(); // case normalization
			s = s.replaceAll("[_-]", " "); // link normalization
			s = s.replaceAll("[.,!?]", ""); // remove punctuation and exclamation/interrogation
			s = s.replaceAll("\\s", " "); // blank normalization
			s = s.replaceAll("\\s+", " "); // only one blank
			s = s.trim();
		}
		return s;
	}

	public static String replaceNamespaces(String uri) {
		uri = uri.replace("http://www.w3.org/2002/07/owl#", "owl:");
		uri = uri.replace("http://www.w3.org/2000/01/rdf-schema#", "rdfs:");
		uri = uri.replace("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:");
		return uri;
	}

	public static String normalizeLiteral(String literal) {
		literal = literal.replaceAll("[-]", "_"); // link normalization
		literal = literal.replaceAll("[.,']", ""); // remove punctuation
		literal = literal.replaceAll("\\s", "_"); // remove whitespace
		literal = literal.replaceAll("[_]+", "_");
		literal = literal.toLowerCase(); // case normalization
		literal = literal.trim();
		return literal;
	}

	public static boolean isUri(String string) {
		String httpPattern = "^(http|https)://.*";
		URI uri;

		if (!string.matches(httpPattern)) {
//			System.out.println("does not match");
			return false;
		}
		try {
			uri = URI.create(string);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
