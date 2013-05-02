import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.rit.numeric.ExponentialPrng;
import edu.rit.sim.Event;
import edu.rit.sim.Simulation;
import edu.rit.util.Random;

public class Churner {
	private Simulation sim;
	private ExponentialPrng churnRate;
	private ExponentialPrng stabilizer;
	private ChordRing ring;
	private Map<Integer, Node> nodes;
	private List<Integer> nodeKeys;
	private boolean stabilize;

	public Churner(Simulation sim, Random rand, double meanChurnRate,
			ChordRing ring, double meanStabilizeTime, boolean stabilize) {
		this.sim = sim;
		this.ring = ring;
		this.churnRate = new ExponentialPrng(rand, 1.0 / meanChurnRate);
		this.stabilizer = new ExponentialPrng(rand, 1.0 / meanStabilizeTime);
		this.nodeKeys = ring.nodeKeys;
		this.nodes = ring.nodes;
		this.stabilize = stabilize;
		churn();
	}

	public List<Node> churnedNodes = new ArrayList<Node>();

	public void churn() {
		if (!ring.queue.isEmpty()) {
			int nodeKey = nodeKeys.get(new java.util.Random().nextInt(nodeKeys
					.size()));
			final Node node = nodes.get(nodeKey);
			churnedNodes.add(node);
			node.changeState();
			if (stabilize) {
				sim.doAfter(stabilizer.next(), new Event() {

					@Override
					public void perform() {
						for (Node n : nodes.values()) {
							// n.start();
							if (n.alive) {
								FingerTable table = n.fingerTable;
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
