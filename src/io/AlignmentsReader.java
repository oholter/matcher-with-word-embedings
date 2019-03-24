package io;

import java.util.ArrayList;
import java.util.List;

import uk.ac.ox.krr.logmap2.mappings.objects.MappingObjectStr;

public abstract class AlignmentsReader {
	String fileName;
	ArrayList<MappingObjectStr> mappings;
	int numberOfMappings;
	
	public AlignmentsReader(String fname) {
		this.fileName = fname;
		mappings = new ArrayList<>();
		openMappingsFile();
		readMappings();
		System.out.println("Read " + numberOfMappings + " mappings from: " + fileName);
	}
	
	public AlignmentsReader() {}
	
	public List<MappingObjectStr> getMappings() {
		return mappings;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public abstract void openMappingsFile();
	
	public abstract void readMappings();
}
