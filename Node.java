import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.rit.numeric.ExponentialPrng;
import edu.rit.sim.Event;
import edu.rit.sim.Simulation;

public class Node implements Comparable<Node> {
	public static boolean verbose = true;
	private int id;
	private List<Data> data;
	private ChordRing ring;
	private Map<Integer, Data> dataMap;
	private Simulation sim;
	private ExponentialPrng queryProcTimes;
	public FingerTable fingerTable;
	public Node predecessor;
	public Node successor;
	public boolean alive;

	public Node(int id, ChordRing ring, Simulation sim,
			ExponentialPrng queryProcTimes) {
		this.id = id;
		this.ring = ring;
		this.sim = sim;
		this.queryProcTimes = queryProcTimes;
		this.data = new ArrayList<Data>();
		this.dataMap = new HashMap<Integer, Data>();
		this.fingerTable = new FingerTable(ring);
		this.predecessor = null;
		this.successor = this;
		this.alive = true;
	}

	public boolean addData(Data data) {
		this.dataMap.put(data.hashCode(), data);
		return this.data.add(data);
	}

	public String toString() {
		return Integer.toString(id);// + data;
	}

	@Override
	public int compareTo(Node o) {
		return this.id - o.id;
	}

	public int getId() {
		return id;
	}

	public void notify(Node n) {
		if (predecessor == null || (predecessor.id < n.id && n.id < id)) {
			predecessor = n;
			HashMap<Integer, Data> tempMap = new HashMap<Integer, Data>();
			tempMap.putAll(dataMap);
			for (int key : tempMap.keySet()) {
				if (key <= n.id) {
					dataMap.remove(key);
				}
			}
		}
	}

	// private static PrintStream ps;
	// static {
	// try {
	// ps = new PrintStream("ft.txt");
	// } catch (FileNotFoundException e) {
	// e.printStackTrace();
	// }
	// }

	public void join() {
		this.alive = true;
		this.fingerTable.update(id);

		// ps.append(this + ": " + fingerTable.string() + "\n");
		this.successor = this.fingerTable.getIthEntry(0);
		successor.notify(this);
	}

	public void start() {
		stabilize();
		fixFingers();
		checkPredecessor();
	}

	public void stabilize() {
		// System.out.printf("%.3f %s", sim.time(), ": ");
		// System.out.println(this + " Stabilizing now");
		Node x = successor.predecessor;
		if (x != null && id < x.id && x.id < successor.id) {
			successor = x;
			successor.notify(this);
		}
	}

	private int next = -1;

	public void fixFingers() {
		this.fingerTable.update(id);
		// next += 1;
		// if (next >= fingerTable.size())
		// next = 0;
		// fingerTable.updateEntry(id, next);

	}

	public void checkPredecessor() {
		if (predecessor != null && !predecessor.alive)
			predecessor = null;
	}

	public void copyData(Map<Integer, Data> data) {
		this.dataMap.putAll(data);
	}

	public void changeState() {
		if (alive) {
			alive = false;
			// System.out.println(this + " copied data to " + successor);
			if (!dataMap.isEmpty()) {
				successor.copyData(dataMap);
//				if (verbose) {
//					System.err.printf("%.3f %s", sim.time(), ": ");
//					System.err.println(this + " copied " + dataMap + " to "
//							+ successor);
//				}
			}
		} else {
			alive = true;
			join();
		}
	}

	private List<Integer> seen = new ArrayList<Integer>();

	/**
	 * Checks for the current node if it contains the lookup key else forwards
	 * it to the appropriate node in its finger table.
	 * 
	 * @param dataKey
	 */
	public void query(final int dataKey) {
		if (!alive) {
			if (verbose) {
				System.out.println("I am dead!");
				System.out.println("Node " + this + ": Lookup failed!");
			}
			ChordRing.lookups.add(0.0);
			sim.doAfter(queryProcTimes.next(), new Event() {

				@Override
				public void perform() {
					ring.lookup();
				}
			});
			return;
		}
		if (verbose)
			System.out.printf("%.3f %s", sim.time(), ": ");
		if (id == dataKey && !dataMap.containsKey(dataKey)) {
			if (verbose) {
				System.out.println("I don't have it!");
				System.out.println("Node " + this + ": Lookup failed!");
			}
			ChordRing.lookups.add(0.0);
			sim.doAfter(queryProcTimes.next(), new Event() {

				@Override
				public void perform() {
					ring.lookup();
				}
			});
			return;
		}
		if (!seen.isEmpty() && seen.get(seen.size() - 1) == dataKey) {
			if (verbose) {
				System.out.println("I looked it twice!");
				System.out.println("Node " + this + ": Lookup failed!");
			}
			ChordRing.lookups.add(0.0);
			sim.doAfter(queryProcTimes.next(), new Event() {

				@Override
				public void perform() {
					ring.lookup();
				}
			});
			return;
		}
		seen.add(dataKey);
		if (verbose)
			System.out.println("Node " + this + ": Received Query " + dataKey);
		if (dataMap.containsKey(dataKey)) {
			if (verbose)
				System.out.println("Node " + this + ": Lookup success!");
			ChordRing.lookups.add(1.0);
			sim.doAfter(queryProcTimes.next(), new Event() {

				@Override
				public void perform() {
					ring.lookup();
				}
			});
			return;
		} else {
			// System.out.println(fingerTable.string());
			if (dataKey > id) {
				final Node succs = fingerTable.getIthEntry(0);
				if (dataKey < succs.getId() || succs.getId() < id) {
					sim.doAfter(queryProcTimes.next(), new Event() {

						@Override
						public void perform() {
							if (verbose)
								System.out.println("***Forwarding Query "
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
				diff = ring.maxSize - 1 + dataKey - id;
			}
			// System.out.println("Diff:" + diff);
			for (int i = 1; i < fingerTable.size(); i++) {
				// System.out.println(ring.getFingerTableEntry(id, i - 1));
				if ((int) Math.pow(ring.base, i) > diff) {
					final int ii = i;
					sim.doAfter(queryProcTimes.next(), new Event() {

						@Override
						public void perform() {
							if (verbose)
								System.out.println("***Forwarding Query "
										+ dataKey + " to " + (ii - 1) + ": "
										+ fingerTable.getIthEntry(ii - 1));
							fingerTable.getIthEntry(ii - 1).query(dataKey);
						}
					});
					return;
				}
			}
			sim.doAfter(queryProcTimes.next(), new Event() {

				@Override
				public void perform() {
					// System.out.println(ring.getFingerTableEntry(id, 9));
					if (verbose)
						System.out.println("***Forwarding Query "
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
