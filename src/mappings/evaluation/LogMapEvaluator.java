package mappings.evaluation;

import java.io.File;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.AlignmentsReader;
import io.OAEIAlignmentsReader;
import io.OntologyReader;
import mapping.object.MappingObjectStr;
import mappings.candidate_finder.AnchorsCandidateFinder;
import mappings.candidate_finder.BestAnchorsCandidateFinder;
import mappings.trainer.OntologyProjector;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.TestRunUtils;
import mappings.walks_generator.SecondOrderWalksGenerator;
import uk.ac.ox.krr.logmap2.LogMap2_Matcher;

public class LogMapEvaluator {

	public static void main(String[] args) throws Exception {
		long startTime = System.nanoTime();

		String firstOntologyFile = TestRunUtils.firstOntologyFile;
		String secondOntologyFile = TestRunUtils.secondOntologyFile;
		String referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
		String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
		double equalityThreshold = TestRunUtils.equalityThreshold;
		double fractionOfMappings = TestRunUtils.fractionOfMappings;
		String walksType = TestRunUtils.walksType;

		Logger log = LoggerFactory.getLogger(WordEmbeddingsTrainer.class);
		String currentDir = new File(ClassLoader.getSystemClassLoader().getResource("").getPath()).toString();
		BasicConfigurator.configure();

		OntologyReader reader = new OntologyReader();
		reader.setFname(firstOntologyFile);
		reader.readOntology();
		OWLOntology onto1 = reader.getOntology();

		reader.setFname(secondOntologyFile);
		reader.readOntology();
		OWLOntology onto2 = reader.getOntology();

		// For training of ontology start:
		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });
		AnchorsCandidateFinder finder = new BestAnchorsCandidateFinder(onto1, onto2, mergedOnto,
				currentDir + "/temp/out.txt", equalityThreshold);

		/* Adding anchors, by using word embeddings or by manually adding them */
//		finder.findAnchors(); /* this will use word embeddings to find anchors */

		/* Adding anchors by reading an alignments file */
		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, onto1, onto2);
//		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(
//				"/home/ole/master/logmap_standalone/output/logmap2_mappings.rdf", onto1, onto2);

		LogMap2_Matcher logMapMatcher = new LogMap2_Matcher("file:/home/ole/master/test_onto/cmt.owl",
				"file:/home/ole/master/test_onto/ekaw.owl", "/home/ole/master/test_onto/out.txt", true);
		String iri1 = logMapMatcher.getIRIOntology1();
		System.out.println(iri1);

		List<MappingObjectStr> mappings = alignmentsReader.getMappings();
		for (int i = 0; i < (mappings.size() * fractionOfMappings); i++) {
			MappingObjectStr mapping = mappings.get(i);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}

		System.out.println("starting projection");

		OntologyProjector projector = new OntologyProjector("file:/home/ole/master/test_onto/merged.owl");
		projector.projectOntology();
		projector.saveModel(TestRunUtils.modelPath);
		System.out.println("starting walksgenerator");

//		SecondOrderWalksGenerator(String inputFile, String outputFile, int numberOfThreads, int walkDepth,
//		int limit, int numberOfWalks, int offset, int p, int q)

		SecondOrderWalksGenerator walks = new SecondOrderWalksGenerator(TestRunUtils.modelPath,
				"/home/ole/master/test_onto/walks_out.txt", 12, 40, 100000, 50, 0, 0.2, 5, "uripart", false);
		walks.generateWalks();

		long endTime = System.nanoTime();
		long duration = (endTime - startTime) / 1000000;
		System.out.println("duration: " + duration);
	}
}
