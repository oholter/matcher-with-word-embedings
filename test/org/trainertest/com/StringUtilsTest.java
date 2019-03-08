package org.trainertest.com;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mappings.utils.StringUtils;


public class StringUtilsTest {
	@BeforeEach
	public void setup() throws Exception {
	}
	
	@Test
	public void normalizedIRIshouldOnlyContainPartAfterHash() {
		String iri = "http://www.google.com#test";
		String norm = StringUtils.normalizeFullIRI(iri);
		
		String iri2 = "http://ekaw#ShouldBeMultipleWords";
		String iri3 = "http://ekaw#Should_be_multiple_words";
		String expected = "should be multiple words";
		String norm2 = StringUtils.normalizeFullIRI(iri2);
		String norm3 = StringUtils.normalizeFullIRI(iri3);
		
		assertEquals("test", norm);
		assertEquals(expected, norm2);
		assertEquals(expected, norm3);
		assertEquals(expected, norm3);
	}
	
	@Test
	public void shouldRemoveStopWords() {
		String[] bag = {"should", "be", "multiple", "words" };
		String[] expected = {"multiple", "words" };
		String[] res = StringUtils.removeStopWords(bag);
		
		for (int i = 0; i < expected.length; i++) {
			assertEquals(res[i], expected[i]);
		}
	}
}
