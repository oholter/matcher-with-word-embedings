package mappings.candidate_finder;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.AlignmentsReader;
import io.OAEIAlignmentsReader;
import io.OntologyReader;
import mappings.trainer.OntologyProjector;
import mappings.trainer.WordEmbeddingsTrainer;
import mappings.utils.TestRunUtils;
import mappings.utils.VectorUtils;
import mappings.walks_generator.Walks;
import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public class ThreeMethodsCandidateFinder extends AnchorsCandidateFinder {

	public WordEmbeddingsTrainer owl2vecTrainer;
	public WordEmbeddingsTrainer labelTrainer;
	public WordEmbeddingsTrainer pretrainedTrainer;
	PrintWriter resultWriter;
	PrintWriter referenceWriter;
	String resultFilePath;
	String referenceFilePath;

	public ThreeMethodsCandidateFinder(OWLOntology onto1, OWLOntology onto2, OWLOntology mergedOnto, String modelPath,
			double distLimit, String resultFilePath, String referenceFilePath) throws Exception {
//		public AnchorsCandidateFinder(OWLOntology o1, OWLOntology o2, OWLOntology mergedOnto, String modelPath,
//				double distLimit) throws Exception {
		super(onto1, onto2, mergedOnto, modelPath, distLimit);
		this.resultFilePath = resultFilePath;
		this.referenceFilePath = referenceFilePath;
	}

	public void createMappings() {
		resultWriter = openWriter();
		referenceWriter = openWriter();

		generateClassCandidates();

		closeWriter(resultWriter);
		closeWriter(referenceWriter);
	}

	public PrintWriter openWriter() {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new File(resultFilePath));
		} catch (Exception e) {
			e.printStackTrace();
		}

		writer.println("a,b,owl2vec,label,pretrained");
		return writer;
	}

	public void closeWriter(PrintWriter writer) {
		writer.close();
	}

	public void generateClassCandidates() {
		int numCandidates = 0;

		for (OWLClass classFromFirstOntology : onto1.getClassesInSignature()) {
			String iriFromFirstOntology = classFromFirstOntology.getIRI().toString();
			String iriFragmentFromFirstOntology = normalizeIRI(classFromFirstOntology.getIRI().getFragment());
			String labelFromFirstOntology = normalizeString(findAnnotation(classFromFirstOntology, onto1, "label"));
			String commentFromFirstOntology = normalizeString(findAnnotation(classFromFirstOntology, onto1, "comment"));

			for (OWLClass classFromSecondOntology : onto2.getClassesInSignature()) {
				String iriFromSecondOntology = classFromSecondOntology.getIRI().toString();

				double owl2vecCosine = 0;
				double pretrainedCosine = 0;
				double labelCosine = 0;

				owl2vecCosine = VectorUtils.cosineSimilarity(owl2vecTrainer.getWordVector(iriFromFirstOntology),
						owl2vecTrainer.getWordVector(iriFromSecondOntology));
				if (Double.isNaN(owl2vecCosine)) {
					owl2vecCosine = 0;
				}

				String iriFragmentFromSecondOntology = normalizeIRI(classFromSecondOntology.getIRI().getFragment());
				String labelFromSecondOntology = normalizeString(
						findAnnotation(classFromSecondOntology, onto2, "label"));
				String commentFromSecondOntology = normalizeString(
						findAnnotation(classFromSecondOntology, onto2, "comment"));

				double iriSimilarity_label = 0;
				double labelSimilarity_label = 0;
				double commentSimilarity_label = 0;

				iriSimilarity_label = labelTrainer.getAvgVectorCosine(iriFragmentFromFirstOntology.split(" "),
						iriFragmentFromSecondOntology.split(" "));

				if (labelFromFirstOntology != null && labelFromSecondOntology != null) {
					labelSimilarity_label = labelTrainer.getAvgVectorCosine(labelFromFirstOntology.split(" "),
							labelFromSecondOntology.split(" "));
				}
				if (commentFromFirstOntology != null && commentFromSecondOntology != null) {
					commentSimilarity_label = labelTrainer.getAvgVectorCosine(commentFromFirstOntology.split(" "),
							commentFromSecondOntology.split(" "));
				}

				// using the best
				labelCosine = Math.max(iriSimilarity_label, Math.max(labelSimilarity_label, commentSimilarity_label));

				double iriSimilarity_pretrained = 0;
				double labelSimilarity_pretrained = 0;
				double commentSimilarity_pretrained = 0;

				iriSimilarity_pretrained = pretrainedTrainer.getAvgVectorCosine(iriFragmentFromFirstOntology.split(" "),
						iriFragmentFromSecondOntology.split(" "));

				if (labelFromFirstOntology != null && labelFromSecondOntology != null) {
					labelSimilarity_pretrained = pretrainedTrainer.getAvgVectorCosine(labelFromFirstOntology.split(" "),
							labelFromSecondOntology.split(" "));
				}
				if (commentFromFirstOntology != null && commentFromSecondOntology != null) {
					commentSimilarity_pretrained = pretrainedTrainer.getAvgVectorCosine(
							commentFromFirstOntology.split(" "), commentFromSecondOntology.split(" "));
				}

				pretrainedCosine = Math.max(iriSimilarity_pretrained,
						Math.max(labelSimilarity_pretrained, commentSimilarity_pretrained));

				if (Double.isNaN(pretrainedCosine)) {
					pretrainedCosine = 0;
				}

				if (owl2vecCosine > distLimit || labelCosine > distLimit || pretrainedCosine > distLimit) {
					resultWriter.println(iriFromFirstOntology + "," + iriFromSecondOntology + "," + owl2vecCosine + ","
							+ labelCosine + "," + pretrainedCosine);
					System.out.println(iriFromFirstOntology + "," + iriFromSecondOntology + "," + owl2vecCosine + ","
							+ labelCosine + "," + pretrainedCosine);
					numCandidates++;
				}
			} // end classFromSecondOntology

		} // finished classFromFirstOntology

		System.out.println("Found " + numCandidates + " class candidates:");
	}

	public void generateDataProperties() {

	}

	public void generateObjectProperties() {

	}

	public void setOwl2VecTrainer(WordEmbeddingsTrainer trainer) {
		this.owl2vecTrainer = trainer;
	}

	public void setLabelTrainer(WordEmbeddingsTrainer trainer) {
		this.labelTrainer = trainer;
	}

	public void setPretrainedTrainer(WordEmbeddingsTrainer trainer) {
		this.pretrainedTrainer = trainer;
	}

	public static void main(String[] args) throws Exception {
		String firstOntologyFile = TestRunUtils.firstOntologyFile;
		String secondOntologyFile = TestRunUtils.secondOntologyFile;
		String referenceAlignmentsFile = TestRunUtils.referenceAlignmentsFile;
		String logMapAlignmentsFile = TestRunUtils.logMapAlignmentsFile;
		double equalityThreshold = TestRunUtils.equalityThreshold;
		double fractionOfMappings = TestRunUtils.fractionOfMappings;
		String walksType = TestRunUtils.walksType;
		String resultFilePath = TestRunUtils.resultFilePath;
		String refFilePath = TestRunUtils.referenceFilePath;
		String modelPath = TestRunUtils.modelPath;
		String word2vecModelPath = TestRunUtils.word2vecModelPath;
		String mergedOwlPath = TestRunUtils.mergedOwlPath;
		String pretrainedModelOutputPath = TestRunUtils.pretrainedModelOutputPath;

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

		OWLOntology mergedOnto = OntologyReader.mergeOntologies("merged", new OWLOntology[] { onto1, onto2 });

//		public ThreeMethodsCandidateFinder(OWLOntology onto1, OWLOntology onto2, String modelPath, double distLimit,
//		String resultFilePath, String referenceFilePath)
		ThreeMethodsCandidateFinder finder = new ThreeMethodsCandidateFinder(onto1, onto2, mergedOnto, modelPath,
				equalityThreshold, resultFilePath, refFilePath);

		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(referenceAlignmentsFile, onto1, onto2);
//		AlignmentsReader alignmentsReader = new OAEIAlignmentsReader(
//				"/home/ole/master/logmap_standalone/output/logmap2_mappings.rdf", onto1, onto2);

		List<MappingObjectStr> mappings = alignmentsReader.getMappings();
		for (int i = 0; i < (mappings.size() * fractionOfMappings); i++) {
			MappingObjectStr mapping = mappings.get(i);
			finder.addAnchor(mapping.getIRIStrEnt1(), mapping.getIRIStrEnt2());
		}

		finder.addAnchorsToOntology(mergedOnto);
		OntologyReader.writeOntology(mergedOnto, mergedOwlPath, "owl");

		OntologyProjector projector = new OntologyProjector(mergedOwlPath);
		projector.projectOntology();
		projector.saveModel(modelPath);

		Walks walks = new Walks(modelPath, walksType);
		walks.generateWalks();
		String owl2vecWalksFile = walks.getOutputFile();
		String labelWalksFile = walks.getLabelOutputFile();

		WordEmbeddingsTrainer owl2vecTrainer = new WordEmbeddingsTrainer(owl2vecWalksFile,
				currentDir + "/temp/out.txt");
		owl2vecTrainer.train();
		finder.setOwl2VecTrainer(owl2vecTrainer);

		WordEmbeddingsTrainer labelTrainer = new WordEmbeddingsTrainer(labelWalksFile,
				currentDir + "/temp/label_out.txt");
		labelTrainer.train();
		finder.setLabelTrainer(labelTrainer);

		WordEmbeddingsTrainer pretrainedTrainer = new WordEmbeddingsTrainer(word2vecModelPath, word2vecModelPath);
		pretrainedTrainer.loadModel();
		finder.setPretrainedTrainer(pretrainedTrainer);

		finder.createMappings();
	}

}
