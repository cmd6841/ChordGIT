import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.rit.numeric.ExponentialPrng;
import edu.rit.numeric.ListSeries;
import edu.rit.sim.Event;
import edu.rit.sim.Simulation;

/**
 * Class Node represents a Node in the Chord Distributed Hash Table that stores
 * data having hash keys related to its hash key. The hash key of a node is
 * given by its id.
 * 
 * @author Chinmay Dani
 * 
 */
public class Node implements Comparable<Node> {
	/**
	 * Toggle verbose mode.
	 */
	public static boolean verbose;

	/**
	 * The hash key of a Node in the Chord Ring.
	 */
	private int id;

	/**
	 * The Chord ring to which the node belongs.
	 */
	private ChordRing ring;

	/**
	 * A collection of the data items the node stores.
	 */
	private Map<Integer, Data> dataMap;

	/**
	 * The Simulation object.
	 */
	private Simulation sim;

	/**
	 * A random number generator for generating random query processing/
	 * forwarding times.
	 */
	private ExponentialPrng queryProcTimes;

	/**
	 * The finger table of the node containing entries of the nodes
	 * corresponding to certain nodes in the Chord ring.
	 */
	private FingerTable fingerTable;

	/**
	 * The predecessor node of this node in the Chord ring.
	 */
	private Node predecessor;

	/**
	 * The successor node of this node in the Chord ring.
	 */
	private Node successor;

	/**
	 * A flag indicating whether the node is a active.
	 */
	private boolean isAlive;

	/**
	 * A series to accumulate the number of successful and failed lookups.
	 */
	private ListSeries lookupSeries;

	/**
	 * A list containing the hash keys of queries looked up by this Node.
	 */
	private List<Integer> seenQueries = new ArrayList<Integer>();

	/**
	 * Construct a new Node object with the supplied information.
	 * 
	 * @param id
	 *            the hash key of the node
	 * @param ring
	 *            the Chord ring this node belongs to
	 * @param sim
	 *            the Simulation object
	 * @param queryProcTimes
	 *            the random query processing time generator
	 * @param lookupSeries
	 *            the accumulator series for lookup information
	 */
	public Node(int id, ChordRing ring, Simulation sim,
			ExponentialPrng queryProcTimes, ListSeries lookupSeries) {
		this.id = id;
		this.ring = ring;
		this.sim = sim;
		this.queryProcTimes = queryProcTimes;
		this.dataMap = new HashMap<Integer, Data>();
		this.fingerTable = new FingerTable(ring);
		this.predecessor = null;
		this.successor = this;
		this.isAlive = true;
		this.lookupSeries = lookupSeries;
	}

	/**
	 * Adds data to the Node's dataMap
	 * 
	 * @param data
	 *            The Data object to be added.
	 * @return true if object added successfully, false otherwise
	 */
	public boolean addData(Data data) {
		return (this.dataMap.put(data.hashCode(), data) == null);
	}

	/**
	 * Joins the Chord ring this Node belongs to, updates its finger table
	 * entries and successor node and notifies its successor about its arrival.
	 */
	public void join() {
		this.isAlive = true;
		this.fingerTable.update(id);
		this.successor = this.fingerTable.getIthEntry(0);
		successor.notify(this);
	}

	/**
	 * Gets notified by a node to update the predecessor. Called when a new node
	 * joins between the current node and its predecessor. Also, the data
	 * belonging to the newly join node is transferred to the node.
	 * 
	 * @param node
	 */
	public void notify(Node node) {
		if (predecessor == null || (predecessor.id < node.id && node.id < id)) {
			predecessor = node;
			HashMap<Integer, Data> tempMap = new HashMap<Integer, Data>();
			tempMap.putAll(dataMap);
			for (int key : tempMap.keySet()) {
				if (key <= node.id) {
					dataMap.remove(key);
				}
			}
		}
	}

	/**
	 * Starts the stabilization procedure for the current node.
	 */
	public void start() {
		stabilize();
		fixFingers();
		checkPredecessor();
	}

	/**
	 * Checks for any node that has joined and could be the successor to this
	 * Node. Also, notifies the newly joined node to update its predecessor to
	 * this node.
	 */
	public void stabilize() {
		Node x = successor.predecessor;
		if (x != null && id < x.id && x.id < successor.id) {
			successor = x;
			successor.notify(this);
		}
	}

	/**
	 * Fixes the finger table entries as a part of maintaining updated
	 * information about the Chord ring.
	 */
	public void fixFingers() {
		this.fingerTable.update(id);
	}

	/**
	 * Checks if the predecessor node is alive or not.
	 */
	public void checkPredecessor() {
		if (predecessor != null && !predecessor.isAlive)
			predecessor = null;
	}

	/**
	 * Changes the current state of the node in the Chord ring. If the node
	 * turns inactive, then it copies all its data to its successor node. If the
	 * node turns active, then calls the join() method to update its finger
	 * table information.
	 */
	public void changeState() {
		if (isAlive) {
			isAlive = false;
			if (!dataMap.isEmpty()) {
				successor.copyData(dataMap);
				if (verbose) {
					System.err.printf("%.3f %s", sim.time(), ": ");
					System.err.println(this + " copied " + dataMap + " to "
							+ successor);
				}
			}
		} else {
			isAlive = true;
			join();
		}
	}

	/**
	 * Stores a collection of Data objects in the current Node's dataMap.
	 * 
	 * @param data
	 *            the collection of Data objects.
	 */
	public void copyData(Map<Integer, Data> data) {
		this.dataMap.putAll(data);
	}

	/**
	 * Checks whether the current node is in active state.
	 * 
	 * @return true if active, false otherwise
	 */
	public boolean isAlive() {
		return isAlive;
	}

	/**
	 * Returns the string representation of the Node.
	 * 
	 * @return The hash key of the node as a String
	 */
	public String toString() {
		return Integer.toString(id);
	}

	/**
	 * Compares the hash key of this node to another node.
	 * 
	 * @return 0 if the keys are equal, -1 if this node's key is lesser, +1
	 *         otherwise
	 */
	@Override
	public int compareTo(Node o) {
		return this.id - o.id;
	}

	/**
	 * Returns the FingerTable of this Node.
	 * 
	 * @return the FingerTable object
	 */
	public FingerTable getFingerTable() {
		return this.fingerTable;
	}

	public int getId() {
		return id;
	}

	/**
	 * Lookup mechanism of the Chord DHT. Checks for the current node if it
	 * contains the lookup key else forwards it to the appropriate node in its
	 * finger table.
	 * 
	 * @param dataKey
	 *            the hash key of the Data object being queried.
	 */
	public void query(final int dataKey) {
		// Lookup failure if the current node is inactive.
		if (!isAlive) {
			if (verbose) {
				System.out.println("Node " + this
						+ " is inactive. Lookup failed!");
			}
			lookupSeries.add(0);
			return;
		}
		// Lookup failure if the data key is equal to the current node's hash
		// key, but the data is not present on this node.
		if (verbose)
			System.out.printf("%.3f %s", sim.time(), ": ");
		if (id == dataKey && !dataMap.containsKey(dataKey)) {
			if (verbose) {
				System.out.println("Node " + this + ": Lookup failed!");
			}
			lookupSeries.add(0);
			return;
		}
		// Lookup failure if the current node is being looked up for the same
		// query.
		if (!seenQueries.isEmpty()
				&& seenQueries.get(seenQueries.size() - 1) == dataKey) {
			if (verbose) {
				System.out.println("I looked it twice!");
				System.out.println("Node " + this + ": Lookup failed!");
			}
			lookupSeries.add(0);
			return;
		}

		seenQueries.add(dataKey);

		if (verbose)
			System.out.println("Node " + this + ": Received Query " + dataKey);

		// Lookup success.
		if (dataMap.containsKey(dataKey)) {
			if (verbose)
				System.out.println("Node " + this + ": Lookup success!");
			lookupSeries.add(1.0);
			return;
		}
		// Forward the query to the node with the largest hash key lesser than
		// the data hash key.
		else {
			if (dataKey > id) {
				final Node succs = fingerTable.getIthEntry(0);
				if (dataKey < succs.getId() || succs.getId() < id) {
					sim.doAfter(queryProcTimes.next(), new Event() {

						@Override
						public void perform() {
							if (verbose)
								System.out.println("*** Forwarding Query "
										+ dataKey + " to " + succs);
							succs.query(dataKey);
						}
					});
					return;
				}
			}
			int diff = 0;
			if (dataKey > id) {
				diff = dataKey - id;
			} else {
				diff = ring.ringMaxSize() - 1 + dataKey - id;
			}
			for (int i = 1; i < fingerTable.size(); i++) {
				if ((int) Math.pow(ring.ringBase(), i) > diff) {
					final int temp = i;
					sim.doAfter(queryProcTimes.next(), new Event() {

						@Override
						public void perform() {
							if (verbose)
								System.out.println("*** Forwarding Query "
										+ dataKey + " to " + (temp - 1) + ": "
										+ fingerTable.getIthEntry(temp - 1));
							fingerTable.getIthEntry(temp - 1).query(dataKey);
						}
					});
					return;
				}
			}
			sim.doAfter(queryProcTimes.next(), new Event() {

				@Override
				public void perform() {
					if (verbose)
						System.out.println("*** Forwarding Query "
								+ dataKey
								+ " to "
								+ (fingerTable.size() - 1)
								+ ": "
								+ fingerTable.getIthEntry(fingerTable.size() - 1));
					fingerTable.getIthEntry(fingerTable.size() - 1).query(
							dataKey);
				}
			});
			return;
		}
	}

}
