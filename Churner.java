import java.util.List;
import java.util.Map;

import edu.rit.numeric.ExponentialPrng;
import edu.rit.sim.Event;
import edu.rit.sim.Simulation;
import edu.rit.util.Random;

/**
 * Class Churner is responsible for the simulation of nodes voluntarily leaving
 * or joining a Chord ring in the Chord Distributed Hash Table. It is also
 * responsible for simulating the stabilization procedure on nodes in the Chord
 * ring.
 * 
 * @author Chinmay Dani
 * 
 */
public class Churner {
	/**
	 * The Simulation object.
	 */
	private Simulation sim;

	/**
	 * The random churn time generator.
	 */
	private ExponentialPrng churnRate;

	/**
	 * The random stabilization time generator.
	 */
	private ExponentialPrng stabilizer;

	/**
	 * The ChordRing object.
	 */
	private ChordRing ring;

	/**
	 * The collection of nodes in the Chord ring.
	 */
	private Map<Integer, Node> nodes;

	/**
	 * The sorted list of the hash keys of all the nodes in the Chord ring.
	 */
	private List<Integer> nodeKeys;

	private boolean stabilize;

	/**
	 * Construct a new Churner object and start the simulation of churing of
	 * node in the Chord ring.
	 * 
	 * @param sim
	 *            the Simulation object
	 * @param rand
	 *            the Random object
	 * @param meanChurnRate
	 *            the mean churn interval
	 * @param ring
	 *            the ChordRing object
	 * @param meanStabilizeTime
	 *            the mean stabilization interval
	 */
	public Churner(Simulation sim, Random rand, double meanChurnRate,
			ChordRing ring, double meanStabilizeTime, boolean stabilize) {
		this.sim = sim;
		this.ring = ring;
		this.churnRate = new ExponentialPrng(rand, 1.0 / meanChurnRate);
		this.stabilizer = new ExponentialPrng(rand, 1.0 / meanStabilizeTime);
		this.nodeKeys = ring.getNodeKeys();
		this.nodes = ring.getNodes();
		this.stabilize = stabilize;
		churn();
	}

	/**
	 * Simulates the voluntary arrival and departure of nodes in the Chord ring
	 * at regular intervals exponentially distributed over a mean interval. Also
	 * simulates the stabilization procedure on relevant nodes that are affected
	 * by the arrival/departure of a node from the ring.
	 */
	public void churn() {
		if (!ring.isQueueEmpty()) {
			int nodeKey = nodeKeys.get(new java.util.Random().nextInt(nodeKeys
					.size()));
			final Node node = nodes.get(nodeKey);
			node.changeState();
			if (stabilize) {
				sim.doAfter(stabilizer.next(), new Event() {

					@Override
					public void perform() {
						// Calls the stabilize procedure of the affected nodes
						// in the Chord ring.
						for (Node n : nodes.values()) {
							if (n.isAlive()) {
								FingerTable table = n.getFingerTable();
								for (int i = 0; i < table.size(); i++) {
									if (table.getIthEntry(i).equals(node)) {
										n.start();
										break;
									}
								}
							}
						}
					}
				});
			}
			sim.doAfter(churnRate.next(), new Event() {

				@Override
				public void perform() {
					churn();
				}
			});
		}
	}
}
