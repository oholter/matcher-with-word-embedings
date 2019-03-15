package node_graph;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.Random;
import java.util.TreeMap;

public class EdgeCollection {
	private final NavigableMap<Double, Edge> map = new TreeMap<Double, Edge>();
	private final Random random;
	private double total = 0;

	public EdgeCollection() {
		this(new Random());
	}

	public EdgeCollection(Random random) {
		this.random = random;
	}

	public EdgeCollection add(double weight, Edge edge) {
		if (weight <= 0)
			return this;
		total += weight;
		map.put(total, edge);
		return this;
	}

	public Edge next() {
		if (map.size() != 0) {
			double value = random.nextDouble() * total;
			return map.higherEntry(value).getValue();
		} else {
			return null;
		}
	}

	public static void main(String[] args) {
		List<Edge> edgeList = new ArrayList<>();
		Edge e1 = new Edge("http://www.w3.org/2000/01/rdf-schema#subClassOf");
		e1.inNode = new Node("intest1");
		e1.outNode = new Node("outtest1");
		Edge e2 = new Edge("http://ekaw#AuthorOf");
		e2.inNode = new Node("intest2");
		e2.outNode = new Node("outtest2");
		edgeList.add(e1);
		edgeList.add(e2);

		EdgeCollection col = new EdgeCollection();
		col.add(e1.weight, e1);
		col.add(e2.weight, e2);

		System.out.println(col.next().inNode);
		System.out.println(col.next().inNode);
		System.out.println(col.next().inNode);
		System.out.println(col.next().inNode);

	}
}
