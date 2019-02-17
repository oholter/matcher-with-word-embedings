package mappings.utils;

import java.net.URI;

public class StringUtils {
	public static String normalizeFullIRI(String s) {
		String httpPattern = "^(http|https)://.*#";
		s = s.replaceAll(httpPattern, "");
		return normalizeIRI(s); //replace(" ", "_");
	}
	
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
			s = s.replaceAll("[.,]", ""); // remove punctuation
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
		URI uri;
		try {
			uri = URI.create(string);
			return true;
		} catch (Exception e) {
			return false;
//			return string.contains("http://");
		}
	}
}
