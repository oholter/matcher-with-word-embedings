package io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Set;

import org.semanticweb.owlapi.io.OWLRenderer;
import org.semanticweb.owlapi.manchestersyntax.renderer.ManchesterOWLSyntaxRenderer;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.ox.cs.JRDFox.Prefixes;
import uk.ac.ox.cs.JRDFox.model.Query;
import uk.ac.ox.cs.JRDFox.store.DataStore;
import uk.ac.ox.cs.JRDFox.store.Resource;
import uk.ac.ox.cs.JRDFox.store.TupleIterator;

public class OutputStarSpaceFormat {
	private static String outputFile = "/home/ole/master/test_onto/render.txt";
	private static String ontoName = "/home/ole/master/test_onto/ekaw.owl";
	private static String projectionName = "/home/ole/master/test_onto/projection.ttl";

	public void outputManchesterSyntax() throws Exception {
		OutputStream out = new FileOutputStream(new File(outputFile));
		OntologyReader reader = new OntologyReader();
		reader.setFname(ontoName);
		reader.readOntology();
		OWLOntology onto = reader.getOntology();
		OWLRenderer renderer = new ManchesterOWLSyntaxRenderer();

		Set<OWLAxiom> axioms = onto.getAxioms();
		renderer.render(onto, out);
	}

	public static void output() throws Exception {
		DataStore store = new DataStore(DataStore.StoreType.ParallelSimpleNN);
		store.setNumberOfThreads(12);
		store.importFiles(new File[] { new File(projectionName) });

		OntologyReader reader = new OntologyReader();
		reader.setFname(ontoName);
		reader.readOntology();
		OWLOntology onto = reader.getOntology();

		System.out.println("Number of tuples after import: " + store.getTriplesCount());
		Prefixes prefixes = Prefixes.DEFAULT_IMMUTABLE_INSTANCE;
		System.out.println("Retrieving all properties before materialisation.");
		String queryString = "SELECT DISTINCT ?x ?y ?z "
				+ "WHERE{ ?x ?y ?z . ?x <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/2002/07/owl#Class> . } ";
		TupleIterator tupleIterator = store.compileQuery(queryString, prefixes);
		String q = "SELECT ?z WHERE { ?x ?y ?z } LIMIT 10";
		System.out.println(queryString);
		TupleIterator it = store.compileQuery(q);

		outputQuery(it);
	}

	public static void outputQuery(TupleIterator iterator) throws Exception {
		PrintWriter out = new PrintWriter( new File(outputFile));
		for (long multiplicity = iterator.open(); multiplicity > 0; multiplicity = iterator.advance()) {
			Resource xResource = iterator.getResource(0);
//			Resource yResource = iterator.getResource(1);
//			Resource zResource = iterator.getResource(2);
			Resource label = iterator.getResource(0);
			if (label != null) {
//				out.println(xResource.toString() + " " + "__label__" + zResource);
				System.out.println(label.toString());
				out.println(label);
			}
		}
		out.close();
	}

	
	public static void main(String[] args) throws Exception {

		output();
	}

}
