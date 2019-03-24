package io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class RelatedConceptsReader {
	String conceptsFilePath;
	Document document;
	List<List<String>> subClassConcepts;
	List<List<String>> propertyConcepts;
	List<List<String>> unrelatedConcepts;
	
	public RelatedConceptsReader(String conceptsFilePath) {
		this.conceptsFilePath = conceptsFilePath;
	}
	
	public void readConcepts() {
		openDocument();
		readSubClassConcepts();
		readPropertyConcepts();
		readUnrelatedConcepts();
	}
	
	public void openDocument() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			System.out.println("Opening: " + conceptsFilePath);
			document = builder.parse(new File(conceptsFilePath));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readSubClassConcepts() {
		subClassConcepts = new ArrayList<List<String>>();
		NodeList nList = document.getElementsByTagName("RelatedSubClassConcepts");
		Node nNode = null;
		for (int i = 0; i < nList.getLength(); i++) { // Each RelatedSubClassConcepts
			nNode = nList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) { 
				subClassConcepts.add(new ArrayList<String>());
				Element eElement = (Element) nNode;
				NodeList concepts = eElement.getElementsByTagName("concept");
				for (int j = 0; j < concepts.getLength(); j++) { // get each URI
					Node nConcept = concepts.item(j);
					String currentUri = nConcept.getTextContent();
					subClassConcepts.get(i).add(currentUri);
				}
			}
		}
	}
	
	public void readPropertyConcepts() {
		propertyConcepts = new ArrayList<List<String>>();
		NodeList nList = document.getElementsByTagName("RelatedPropertyConcepts");
		Node nNode = null;
		for (int i = 0; i < nList.getLength(); i++) { // Each RelatedSubClassConcepts
			nNode = nList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) { 
				propertyConcepts.add(new ArrayList<String>());
				Element eElement = (Element) nNode;
				NodeList concepts = eElement.getElementsByTagName("concept");
				for (int j = 0; j < concepts.getLength(); j++) { // get each URI
					Node nConcept = concepts.item(j);
					String currentUri = nConcept.getTextContent();
					propertyConcepts.get(i).add(currentUri);
				}
			}
		}
	}
	
	public void readUnrelatedConcepts() {
		unrelatedConcepts = new ArrayList<List<String>>();
		NodeList nList = document.getElementsByTagName("UnrelatedConcepts");
		Node nNode = null;
		for (int i = 0; i < nList.getLength(); i++) { // Each RelatedSubClassConcepts
			nNode = nList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) { 
				unrelatedConcepts.add(new ArrayList<String>());
				Element eElement = (Element) nNode;
				NodeList concepts = eElement.getElementsByTagName("concept");
				for (int j = 0; j < concepts.getLength(); j++) { // get each URI
					Node nConcept = concepts.item(j);
					String currentUri = nConcept.getTextContent();
					unrelatedConcepts.get(i).add(currentUri);
				}
			}
		}
	}
	
	public List<List<String>> getSubClassConcepts() {
		return subClassConcepts;
	}
	
	public List<List<String>> getPropertyConcepts() {
		return propertyConcepts;
	}
	
	public List<List<String>> getUnrelatedConcepts() {
		return unrelatedConcepts;
	}
	
	public static void main(String[] args) throws Exception {
		RelatedConceptsReader reader = new RelatedConceptsReader("/home/ole/src/thesis/evaluation/ekaw.xml");
		reader.readConcepts();
		
		reader.subClassConcepts.forEach(System.out::println);
		reader.propertyConcepts.forEach(System.out::println);
	}
}
