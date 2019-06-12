package mappings.utils;

import java.io.File;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.AlignmentsReader;
import io.OAEIAlignmentsReader;
import io.OntologyReader;
import mappings.candidate_finder.BestAnchorsCandidateFinder;
import mappings.evaluation.ClassMappingsEvaluator;
import mappings.evaluation.MappingsEvaluator;
import mappings.trainer.OntologyProjector;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.walks_generator.Walks;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public class BigDocumentCreator {
	public static Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);

	public static String[] ontologyFiles = { "/home/ole/master/test_onto/ekaw.owl",
			"/home/ole/master/test_onto/cmt.owl", "/home/ole/master/test_onto/edas.owl",
			"/home/ole/master/test_onto/iasted.owl", };

	public static String[] anchorFiles = { // "/home/ole/master/test_onto/reference_alignments/cmt-ekaw.rdf",
			"/home/ole/master/test_onto/reference_alignments/cmt-edas.rdf",
			"/home/ole/master/test_onto/reference_alignments/cmt-iasted.rdf",
			"/home/ole/master/test_onto/reference_alignments/edas-ekaw.rdf",
			"/home/ole/master/test_onto/reference_alignments/edas-iasted.rdf" };

	public static void main(String[] args) throws Exception {

		String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();

		OntologyReader reader = new OntologyReader();
		OWLOntology[] ontologies = new OWLOntology[ontologyFiles.length];

		// merging all ontologies
		for (int i = 0; i < ontologyFiles.length; i++) {
			String file = ontologyFiles[i];
			reader.setFname(file);
			reader.readOntology();
			ontologies[i] = reader.getOntology();
		}

		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", ontologies);
		BestAnchorsCandidateFinder finder = new BestAnchorsCandidateFinder(ontologies[1], ontologies[0], mergedOnto,
				TestRunUtils.modelPath, TestRunUtils.equalityThreshold);

		// adding all the anchors
		String referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/cmt-edas.rdf";
		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, ontologies[1],
				ontologies[2]);
		List<MappingObjectStr> mappings = alignmentsReader.getMappings();
		for (int j = 0; j < mappings.size(); j++) {
			MappingObjectStr mapping = mappings.get(j);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}
		finder.addAnchorsToOntology(mergedOnto);

		referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/cmt-iasted.rdf";
		alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, ontologies[1], ontologies[3]);
		mappings = alignmentsReader.getMappings();
		for (int j = 0; j < mappings.size(); j++) {
			MappingObjectStr mapping = mappings.get(j);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}
		finder.addAnchorsToOntology(mergedOnto);

		referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/edas-ekaw.rdf";
		alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, ontologies[2], ontologies[0]);
		mappings = alignmentsReader.getMappings();
		for (int j = 0; j < mappings.size(); j++) {
			MappingObjectStr mapping = mappings.get(j);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}
		finder.addAnchorsToOntology(mergedOnto);

		referenceAlignmentsFile = "/home/ole/master/test_onto/reference_alignments/edas-iasted.rdf";
		alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, ontologies[2], ontologies[3]);
		mappings = alignmentsReader.getMappings();
		for (int j = 0; j < mappings.size(); j++) {
			MappingObjectStr mapping = mappings.get(j);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}
		finder.addAnchorsToOntology(mergedOnto);

		// normal best anchors function
		String firstOntologyFile = TestRunUtils.firstOntologyFile;
		String secondOntologyFile = TestRunUtils.secondOntologyFile;
		referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
		String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
		double equalityThreshold = TestRunUtils.equalityThreshold;
		double fractionOfMappings = TestRunUtils.fractionOfMappings;
		String walksType = TestRunUtils.walksType;

		alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, ontologies[1], ontologies[0]);
		mappings = alignmentsReader.getMappings();
		for (int i = 0; i < (mappings.size() * fractionOfMappings); i++) {
			MappingObjectStr mapping = mappings.get(i);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}

		finder.addAnchorsToOntology(mergedOnto);
		OntologyReader.writeOntology(mergedOnto, "file:/home/ole/master/test_onto/merged.owl", "owl");

		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/merged.owl");
		projector.projectOntology();
		projector.saveModel("/home/ole/master/test_onto/merged.ttl");

		Walks walks = new Walks(TestRunUtils.modelPath, walksType, TestRunUtils.walksFile, TestRunUtils.numWalks,
				TestRunUtils.walkDepth, TestRunUtils.numThreads, TestRunUtils.offset, TestRunUtils.classLimit);
//		Walks walks = new Walks("/home/ole/master/test_onto/merged.ttl", walksType);
//		Walks_rdf2vec walks = new Walks_rdf2vec();
//		walks.loadFromRdfFile("/home/ole/master/test_onto/merged.ttl");
		walks.generateWalks();
		String walksFile = walks.getOutputFile();

		WordEmbeddingsTrainer trainer = new WordEmbeddingsTrainer(walksFile, currentDir + "/temp/out.txt");
//		trainer.train();
		
		/**
		 * calling a python script! -> model.bin
		 */
		TestRunUtils.trainEmbeddings(TestRunUtils.embeddingsSystem);

		trainer.loadGensimModel("/home/ole/master/test_onto/model.bin");
		
		
		finder.setTrainer(trainer);

		finder.createMappings(); // this runs the program

		// evaluating the mappings
		System.out.println("--------------------------------------------");
		System.out.println("The alignments file used to provide anchors: ");
		MappingsEvaluator evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile, logMapAlignmentsFile,
				finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");

		System.out.println("This system:");
		evaluator = new ClassMappingsEvaluator(referenceAlignmentsFile,
				finder.getOutputAlignment().returnAlignmentFile().getFile(), finder.getOnto1(), finder.getOnto2());
		evaluator.printEvaluation();
		System.out.println("--------------------------------------------");

	}

}
