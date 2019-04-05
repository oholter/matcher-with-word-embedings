package node_graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import mappings.utils.StringUtils;

public class NodeGraph {
	private double p; // likely to revisit node
	private double q; // difference between inward/outward nodes
	private List<Node> nodeList;
	private HashMap<String, Node> uri2Node;
	private boolean includeEdges;
	private HashMap<Node, HashMap<Node, EdgeCollection>> edgeCollections;
	private boolean cacheEdgeWeights;

	public NodeGraph(List<Node> nodeList, double p, double q, boolean includeEdges, boolean cacheEdgeWeights) {
		this.q = q;
		this.p = p;
		this.nodeList = nodeList;
		this.uri2Node = createUri2Node();
		this.includeEdges = includeEdges;
		this.edgeCollections = new HashMap<>();
		this.cacheEdgeWeights = cacheEdgeWeights;
	}

	public int size() {
		return nodeList.size();
	}

	public HashMap<String, Node> createUri2Node() {
		HashMap<String, Node> map = new HashMap<>();
		for (Node n : nodeList) {
			map.put(n.label, n);
		}
		return map;
	}

	public Node getNodeFromUri(String uri) {
		return uri2Node.get(uri);
	}

	public boolean containsUri(String uri) {
		return uri2Node.containsKey(uri);
	}

	public boolean containsNode(Node n) {
		return uri2Node.containsValue(n);
	}

	public List<Node> getNodeList() {
		return nodeList;
	}

	public Node getNode(int index) {
		return nodeList.get(index);
	}

	public void shuffleNodeList() {
		Collections.shuffle(nodeList);
	}

	/**
	 * Using treeMap to randomly choose between weighted edges Also taking adjusting
	 * for p and q parameters
	 * 
	 * @param src
	 * @param dst
	 * @return
	 */
	public Edge findNextEdge(Node src, Node dst) {
		if (dst.edges.size() > 0 && src.edges.size() > 0) {
			if (dst.edges.size() == 1) { // only one possible edge, return this
				return dst.edges.stream().findFirst().get();
			}
			
			HashMap<Node, EdgeCollection> lookup = null;
			EdgeCollection col = null;

			if (cacheEdgeWeights) {
				lookup = edgeCollections.get(src);
				// if collection is cached, return next
				if (lookup != null) {
					col = lookup.get(dst);
				}
				if (col != null) {
					return col.next();
				}
			}

			// if not cached -> find, cache and return next edge

			col = new EdgeCollection();
//			System.out.println("src: " + src.toString() + " dst: " + dst.toString());

			for (Edge e : dst.edges) {
				double updatedWeight = e.weight;
				if (e.outNode == src) { // || e.outNode == dst) {
					updatedWeight /= p; // penalizing returning edges
				}

				if (e.outNode.edges != null && e.outNode.edges.stream().anyMatch(x -> x.outNode == src)) {
					// do nothing
				} else { // penalizing when NO next node with the possibility of returning
//					System.out.println("Updated weights: " + src + " " + dst);
					updatedWeight /= q;
				}

				col.add(updatedWeight, e);
			}
			
			if (cacheEdgeWeights && lookup == null) {
				HashMap<Node, EdgeCollection> newMap = new HashMap<>();
				edgeCollections.put(src, newMap);
				newMap.put(dst, col);
			}
			
			return col.next();
		} else if (dst.edges.size() == 0) {
			Edge e = findNextEdge(src);
			if (e != null && !e.outNode.equals(dst)) {
				return e;
			} else {
				return null;
			}
		} else {
			Node notNullEdgesNode = (dst.edges != null && dst.edges.size() > 0) ? dst : src;
			return findNextEdge(notNullEdgesNode);
		}
	}

	/*
	 * Uses only one node, usage: first node in the walk or the last node has no way
	 * to keep walking
	 */
	public Edge findNextEdge(Node node) {
		if (node.edges != null && node.edges.size() > 0) {
			
			if (node.edges.size() == 1) { // only one possible edge, return this
				return node.edges.stream().findFirst().get();
			}
			
			HashMap<Node, EdgeCollection> lookup = null;
			EdgeCollection col = null;
			
			if (cacheEdgeWeights) {
				lookup = edgeCollections.get(node);
				// if collection is cached, return next
				if (lookup != null) {
					col = lookup.get(null);
					
				}
				if (col != null) {
					return col.next();
				}
			}

			col = new EdgeCollection();
			for (Edge e : node.edges) {
				double updatedWeight = e.weight;
				if (e.outNode == node) {
					updatedWeight /= p;
				}
				col.add(updatedWeight, e);
			}
			
			if (cacheEdgeWeights && lookup == null) {
				HashMap<Node, EdgeCollection> newMap = new HashMap<>();
				edgeCollections.put(node, newMap);
				newMap.put(null, col);
			}
			
			return col.next();
			
		} else { // no where to go
//			System.out.println("Ended here");
			return null;
		}
	}

	/**
	 * returns the a string representation of a walks this is useful for writing the
	 * walks
	 * 
	 * @param lst
	 * @return
	 */
	public static String walk2String(List<Element> lst, String outputFormat) {
		String str = null;
		if (outputFormat.toLowerCase().equals("fulluri")) {
			str = lst.stream().map(n -> n.toString()).collect(Collectors.joining(" "));
		} else if (outputFormat.toLowerCase().equals("uripart")) {
			str = lst.stream().map(n -> n.getUriPart()).collect(Collectors.joining(" "));
			str = StringUtils.getUriPart(str);
		} else if (outputFormat.toLowerCase().equals("words")) {
			str = lst.stream().map(n -> n.getUriWords()).collect(Collectors.joining(" "));
		} else if (outputFormat.toLowerCase().equals("onesynonym")) {
			str = lst.stream().map(n -> n.getOneSynonym()).collect(Collectors.joining(" "));
		} else if (outputFormat.toLowerCase().equals("allsynonyms")) {
			str = lst.stream().map(n -> n.getAllSynonyms()).collect(Collectors.joining(" "));
		} else if (outputFormat.toLowerCase().equals("gouripart")) {
			str = lst.stream().map(n -> n.getGoUriPart()).collect(Collectors.joining(" "));
		} else if (outputFormat.toLowerCase().equals("allsynonymsanduri")) {
			str = lst.stream().map(n -> n.getAllSynonymsAndUri()).collect(Collectors.joining(" "));
		} else if (outputFormat.toLowerCase().equals("twodocuments")) {
			str = lst.stream().map(n -> n.getTwoDocumentsFormat()).collect(Collectors.joining("\n"));
		} else if (outputFormat.toLowerCase().equals("uripartnonormalized")) {
			str = lst.stream().map(n -> n.getUriPartNoNormalized()).collect(Collectors.joining(" "));
		} else {
			str = lst.stream().map(n -> n.toString()).collect(Collectors.joining(" "));
		}
		return str;
	}

	public List<Element> createWalks(Node startNode, int walkDepth) {
		ArrayList<Element> lst = new ArrayList<>();
		lst.add(startNode);
		int numNodes = 0;
		while (numNodes < walkDepth) {
			Node current = (Node) lst.get(lst.size() - 1);
			if (lst.size() == 1) {
				Edge nextEdge = findNextEdge(current);
				if (nextEdge != null) {
					Node nextNode = nextEdge.outNode;
					if (includeEdges) {
						lst.add(nextEdge);
					}
					lst.add(nextNode);
					numNodes++;
				} else {
					break; // node has no edges
				}
			} else {
//				System.out.println(nodeListToString(lst));
				Node previous;
				if (includeEdges) {
					previous = (Node) lst.get(lst.size() - 3);
				} else {
					previous = (Node) lst.get(lst.size() - 2);
				}
				Edge nextEdge = findNextEdge(previous, current);
				if (nextEdge != null) {
					Node nextNode = nextEdge.outNode;
					if (includeEdges) {
						lst.add(nextEdge);
					}
					lst.add(nextNode);
					numNodes++;
				} else {
					break;
				}
			}
		}
		return lst;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
