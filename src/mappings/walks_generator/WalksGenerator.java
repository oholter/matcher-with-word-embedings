package mappings.walks_generator;

public abstract class WalksGenerator {
	protected String inputFile;
	protected String outputFilePath;
	protected int walkDepth;
	protected int limit;
	protected int offset;
	protected int numberOfWalks;
	protected int numberOfThreads;

	public WalksGenerator(String inputFile, String outputFile, int numberOfThreads, int walkDepth,
			int limit, int numberOfWalks, int offset) {
		this.inputFile = inputFile;
		this.outputFilePath = outputFile;
		this.numberOfThreads = numberOfThreads;
		this.walkDepth = walkDepth;
		this.limit = limit;
		this.offset = offset;
		this.numberOfWalks = numberOfWalks;
		this.numberOfThreads = numberOfThreads;
	}
	
	abstract public void generateWalks();
}
