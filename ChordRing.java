import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import edu.rit.numeric.ExponentialPrng;
import edu.rit.numeric.ListSeries;
import edu.rit.sim.Event;
import edu.rit.sim.Simulation;
import edu.rit.util.Random;

/**
 * Class ChordRing represents the structure of the ring topology of the nodes
 * that is used in the Chord Distributed Hash Table algorithm. This class is
 * responsible for creation of a number initial nodes, firing query lookups on
 * random nodes and getting the finger table information of nodes.
 * 
 * @author Chinmay Dani
 * 
 */
public class ChordRing {
	/**
	 * Toggle verbose mode.
	 */
	public static boolean verbose;

	/**
	 * The base of the Chord DHT.
	 */
	private int base;

	/**
	 * The hash key length in bits generated by the hash function.
	 */
	private int hashKeyLength;

	/**
	 * The maximum size of the Chord ring given be the base and the hash key
	 * length.
	 */
	private int maxSize;

	/**
	 * A map with the pairs of hash key and corresponding node in the ring.
	 */
	private Map<Integer, Node> nodes;

	/**
	 * A list of hash key of the nodes in the ring (sorted).
	 */
	private List<Integer> nodeKeys;

	/**
	 * A Simulation object.
	 */
	private Simulation sim;

	/**
	 * A random number generator for generating random query processing/
	 * forwarding times.
	 */
	private ExponentialPrng queryProcTimes;

	/**
	 * A queue containing the queries to be looked up.
	 */
	private Queue<Integer> queue;

	/**
	 * A series to accumulate the number of successful and failed lookups.
	 */
	private ListSeries lookups;

	/**
	 * Construct a new Chord ring with the given number of initial nodes.
	 * 
	 * @param base
	 *            the base of the Chord
	 * @param hashKeyLength
	 *            the number of bits in the hash key
	 * @param initialNodes
	 *            the number of initial nodes
	 * @param sim
	 *            the Simulation object
	 * @param rand
	 *            the Random object
	 * @param meanProcTime
	 *            the mean query processing/forwarding time
	 */
	public ChordRing(int base, int hashKeyLength, int initialNodes,
			Simulation sim, Random rand, double meanProcTime) {
		this.queryProcTimes = new ExponentialPrng(rand, 1.0 / meanProcTime);
		this.sim = sim;
		this.base = base;
		this.hashKeyLength = hashKeyLength;
		this.maxSize = (int) Math.pow(base, this.hashKeyLength);
		this.nodeKeys = new ArrayList<Integer>();
		this.nodes = new HashMap<Integer, Node>();
		this.lookups = new ListSeries();
		this.queue = new LinkedList<Integer>();
		for (int i = 0; i < initialNodes; i++) {
			int x = rand.nextInt(maxSize);
			while (nodes.containsKey(x)) {
				x = rand.nextInt(maxSize);
			}
			nodes.put(x, new Node(x, this, sim, queryProcTimes, lookups));
			nodeKeys.add(x);
		}
		// Update the finger table entries and successor information of
		// all the nodes in the ring.
		for (int i = 0; i < initialNodes; i++) {
			nodes.get(nodeKeys.get(i)).join();
		}
		// Sort the node hash keys for placing them on the Chord ring.
		Collections.sort(nodeKeys);
	}

	/**
	 * Adds the given data item on appropriate node in the Chord ring. Finds a
	 * node with the same hash key as the given data. If not found, finds the
	 * next immediate node in the Chord ring and stores the data on it.
	 * 
	 * @param data
	 *            Data object
	 * @return true if data added successfully, false otherwise
	 */
	public boolean addDataToNode(Data data) {
		int entry = data.hashCode();
		if (nodeKeys.contains(entry)) {
			data.setNode(nodes.get(entry));
			return nodes.get(entry).addData(data);
		}
		for (int j = entry; j < maxSize; j++) {
			if (nodeKeys.contains(j)) {
				data.setNode(nodes.get(j));
				return nodes.get(j).addData(data);
			}
		}
		for (int j = 0; j < entry; j++) {
			if (nodeKeys.contains(j)) {
				data.setNode(nodes.get(j));
				return nodes.get(j).addData(data);
			}
		}
		return false;
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
			if (nodeKeys.contains(j) && nodes.get(j).isAlive())
				return nodes.get(j);
		}
		for (int j = 0; j < entry; j++) {
			if (nodeKeys.contains(j) && nodes.get(j).isAlive())
				return nodes.get(j);
		}
		return null;
	}

	/**
	 * Adds a query Data object's hash key in the query queue for lookup.
	 * 
	 * @param dataKey
	 *            the hash key of the data to be retrieved
	 */
	public void addQuery(int dataKey) {
		queue.add(dataKey);
	}

	/**
	 * Initiates a query lookup at a random start node in the ring until the
	 * query queue becomes empty.
	 */
	public void lookup() {
		if (!queue.isEmpty()) {
			int dataKey = queue.remove();
			int startNodeKey = nodeKeys.get(new java.util.Random()
					.nextInt(nodeKeys.size()));
			if (verbose) {
				System.out.println("Remaining queries: " + queue.size());
				System.out.println();
				System.out.printf("%.3f %s", sim.time(), ": ");
				System.out.println("Query " + dataKey + " initiated at Node "
						+ startNodeKey);
			}
			nodes.get(startNodeKey).query(dataKey);
			sim.doAfter(queryProcTimes.next(), new Event() {

				@Override
				public void perform() {
					lookup();
				}
			});
		}
	}

	/**
	 * Returns the maximum size of the Chord ring.
	 * 
	 * @return the maximum size of the Chord ring
	 */
	public int ringMaxSize() {
		return this.maxSize;
	}

	/**
	 * Returns the base of the Chord ring.
	 * 
	 * @return the base of the Chord ring.
	 */
	public int ringBase() {
		return this.base;
	}

	/**
	 * Returns the map of nodes in the Chord Ring.
	 * 
	 * @return the map of nodes in the Chord Ring.
	 */
	public Map<Integer, Node> getNodes() {
		return this.nodes;
	}

	/**
	 * Returns the list of node hash keys in the Chord Ring.
	 * 
	 * @return the list of node hash keys in the Chord Ring.
	 */
	public List<Integer> getNodeKeys() {
		return this.nodeKeys;
	}

	/**
	 * Returns the number of bits in the hash key.
	 * 
	 * @return the number of bits in the hash key.
	 */
	public int getHashKeyLength() {
		return this.hashKeyLength;
	}

	/**
	 * Returns the list series containing information of successful/failed
	 * lookups.
	 * 
	 * @return the list series containing information of successful/failed
	 *         lookups.
	 */
	public ListSeries getSeries() {
		return this.lookups;
	}
	
	/**
	 * Checks if the query queue is empty.
	 * 
	 * @return true if queue is empty, false otherwise
	 */
	public boolean isQueueEmpty() {
		return queue.isEmpty();
	}
	/**
	 * For unit testing purpose.
	 */
	public static void main(String[] a) {
		ChordRing ring = new ChordRing(2, 10, 512, new Simulation(),
				Random.getInstance(31413), 1);
		System.out.println(ring.nodeKeys);
	}
}
