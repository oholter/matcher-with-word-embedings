package io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import mapping.object.MappingObjectStr;
import mappings.utils.AlignmentUtilities;

public class OAEIAlignmentsReader extends AlignmentsReader {
	Document document;
	OWLOntology firstOntology;
	OWLOntology secondOntology;

	public OAEIAlignmentsReader(String fname) {
		super(fname);
	}

	public OAEIAlignmentsReader(String fname, OWLOntology firstOntology, OWLOntology secondOntology) {
		super();
		this.firstOntology = firstOntology;
		this.secondOntology = secondOntology;
		this.fileName = fname;
		mappings = new ArrayList<>();
		openMappingsFile();
		readMappings();
		System.out.println("Read " + numberOfMappings + " mappings from: " + fileName);
	}

	@Override
	public void openMappingsFile() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			System.out.println("Trying to open: " + getFileName());
			document = builder.parse(new File(getFileName()));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void readMappings() {
		NodeList nList = document.getElementsByTagName("Cell");
		Node nNode = null;
		for (int i = 0; i < nList.getLength(); i++) {
			nNode = nList.item(i);
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				Element eElement = (Element) nNode;
				String currentEntity1 = eElement.getElementsByTagName("entity1").item(0).getAttributes().item(0)
						.getTextContent();
				String currentEntity2 = eElement.getElementsByTagName("entity2").item(0).getAttributes().item(0)
						.getTextContent();
				double currentConfidence = Double
						.parseDouble(eElement.getElementsByTagName("measure").item(0).getTextContent());
//				System.out.println(currentEntity1 + " = " + currentEntity2 + ": " + currentConfidence);
				MappingObjectStr currentMappingObject = new MappingObjectStr(currentEntity1, currentEntity2,
						currentConfidence);
				
				if (firstOntology != null && secondOntology != null) {
					IRI iri = IRI.create(currentEntity1);
					boolean isClass = firstOntology.containsClassInSignature(iri) || secondOntology.containsClassInSignature(iri);
					boolean isObjectProperty = firstOntology.containsObjectPropertyInSignature(iri) || secondOntology.containsObjectPropertyInSignature(iri);
					boolean isDataProperty = firstOntology.containsDataPropertyInSignature(iri) || secondOntology.containsDataPropertyInSignature(iri);
					if (isClass) {
						currentMappingObject.setTypeOfMapping(AlignmentUtilities.CLASSES);
					} else if (isObjectProperty) {
						currentMappingObject.setTypeOfMapping(AlignmentUtilities.OBJECTPROPERTIES);
					} else if (isDataProperty) {
						currentMappingObject.setTypeOfMapping(AlignmentUtilities.DATAPROPERTIES);
					}
				}
				
				mappings.add(currentMappingObject);
				numberOfMappings++;
			}
		}
	}

	public static void main(String[] args) {
		OAEIAlignmentsReader reader = new OAEIAlignmentsReader(
				"/home/ole/master/test_onto/reference_alignments/cmt-ekaw.rdf");
		List<MappingObjectStr> mappings = reader.getMappings();
		mappings.forEach(System.out::println);
	}

}
