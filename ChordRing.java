import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import edu.rit.numeric.ExponentialPrng;
import edu.rit.numeric.ListSeries;
import edu.rit.sim.Simulation;
import edu.rit.util.Random;

public class ChordRing {
	public static boolean verbose = true;
	public int base;
	public int hashKeyLength;
	public int maxSize;
	private int initialNodes;
	public Map<Integer, Node> nodes;
	public List<Integer> nodeKeys;
	private Simulation sim;
	private ExponentialPrng queryProcTimes;
	public Queue<Integer> queue;
	public static ListSeries lookups = new ListSeries();
	public List<Data> dataAdded = new ArrayList<Data>();

	public ChordRing(int base, int hashKeyLength, int initialNodes,
			Simulation sim, Random rand, double meanProcTime) {
		this.queryProcTimes = new ExponentialPrng(rand, 1.0 / meanProcTime);
		this.sim = sim;
		this.base = base;
		this.hashKeyLength = hashKeyLength;
		this.maxSize = (int) Math.pow(base, this.hashKeyLength);
		this.initialNodes = initialNodes;
		this.nodeKeys = new ArrayList<Integer>();
		this.nodes = new HashMap<Integer, Node>();
		for (int i = 0; i < initialNodes; i++) {
			int x = rand.nextInt(maxSize);
			while (nodes.containsKey(x)) {
				x = rand.nextInt(maxSize);
			}

			nodes.put(x, new Node(x, this, sim, queryProcTimes));
			nodeKeys.add(x);
		}
		for (int i = 0; i < initialNodes; i++) {
			nodes.get(nodeKeys.get(i)).join();
		}
		// System.out.println("Total nodes added in the ring: " + initialNodes);
		Collections.sort(nodeKeys);
		this.queue = new LinkedList<Integer>();
		//addRandomData();
	}

	/**
	 * Adds random data items on appropriate nodes in the Chord ring.
	 */
	private void addRandomData() {
		for (int i = 0; i < initialNodes * 1.5; i++) {
			Data data = new Data();
			dataAdded.add(data);
			addDataToNode(data);
		}
		// System.out.println("Total data items added: " + (initialNodes *
		// 1.5));
	}

	/**
	 * Adds the given data item on appropriate node in the Chord ring.
	 * 
	 * @param data
	 *            Data object
	 * @return true if data added successfully, false otherwise
	 */
	public boolean addDataToNode(Data data) {
		int entry = data.hashCode();
		// System.out.println(entry);
		if (nodeKeys.contains(entry)) {
			data.setNode(nodes.get(entry));
			// System.out.println(data);
			return nodes.get(entry).addData(data);
		}
		for (int j = entry; j < maxSize; j++) {
			if (nodeKeys.contains(j)) {
				data.setNode(nodes.get(j));
				// System.out.println(data);
				return nodes.get(j).addData(data);
			}
		}
		for (int j = 0; j < entry; j++) {
			if (nodeKeys.contains(j)) {
				data.setNode(nodes.get(j));
				// System.out.println(data);
				return nodes.get(j).addData(data);
			}
		}
		// System.out.println("failed to add data");
		return false;
	}

	public int ringSize() {
		return this.nodes.size();
	}

	/**
	 * Gets the ith node in the finger table of the given node.
	 * 
	 * @param nodeKey
	 *            the key of the node
	 * @param i
	 *            the entry index
	 * @return node at the ith index
	 */
	public Node getFingerTableEntry(int nodeKey, int i) {
		int entry = (nodeKey + (int) Math.pow(base, i)) % maxSize;
		for (int j = entry; j < maxSize; j++) {
			if (nodeKeys.contains(j) && nodes.get(j).alive)
				return nodes.get(j);
		}
		for (int j = 0; j < entry; j++) {
			if (nodeKeys.contains(j) && nodes.get(j).alive)
				return nodes.get(j);
		}
		return null;
	}

	public void addQuery(int dataKey) {
		queue.add(dataKey);
	}

	/**
	 * Initiates a query lookup at a random start node in the ring.
	 */
	public void lookup() {
		if (!queue.isEmpty()) {
			int dataKey = queue.remove();
			int startNodeKey = nodeKeys.get(new java.util.Random()
					.nextInt(nodeKeys.size()));
			if (verbose) {
				System.out.println();
				System.out.printf("%.3f %s", sim.time(), ": ");
				System.out.println("Query " + dataKey + " initiated at Node "
						+ startNodeKey);
			}
			nodes.get(startNodeKey).query(dataKey);
		}
	}

	public static void main(String[] a) {
		ChordRing ring = new ChordRing(2, 10, 512, new Simulation(),
				Random.getInstance(31413), 1);
		System.out.println(ring.nodeKeys);
	}
}
