import java.util.ArrayList;
import java.util.List;

import edu.rit.numeric.ListSeries;
import edu.rit.sim.Simulation;
import edu.rit.util.Random;

public class ChordSimulation04 {

	private static double meanQueryProcTime = 100.0;
	private static double meanStabilizeTime = 0.1;
	private static int seed = 31443;
	private static Random rand;
	private static Simulation sim;
	private static ChordRing ring;
	private static int base = 2;
	private static int initialNodes = 12;
	private static int hashKeyLength = 10;

	public static void main(String[] args) {
		// base = Integer.parseInt(args[0]);
		// initialNodes = Integer.parseInt(args[1]);
		// meanQueryProcTime = Double.parseDouble(args[2]);
		// seed = Integer.parseInt(args[3]);

		ChordRing.verbose = true;
		Node.verbose = true;

		rand = Random.getInstance(seed);

		Data.maxSize = (int) Math.pow(base, hashKeyLength);

		initialNodes = (int) (Math.pow(base, hashKeyLength) * 0.5);

		List<Data> dataList = new ArrayList<Data>();
		for (int i = 0; i < initialNodes * 1.5; i++) {
			Data data = new Data();
			dataList.add(data);
		}

		List<Data> queryList = new ArrayList<Data>();
		for (int i = 0; i < 1000; i++) {
			Data query = dataList.get(new java.util.Random().nextInt(dataList
					.size()));
			queryList.add(query);
		}

		sim = new Simulation();

		ChordRing.lookups = new ListSeries();

		ring = new ChordRing(base, hashKeyLength, initialNodes, sim, rand,
				meanQueryProcTime);

		for (Data data : dataList) {
			ring.addDataToNode(data);
		}
		System.out.println(queryList);
		for (Data data : queryList) {
			ring.addQuery(data.hashCode());
		}

		new Churner(sim, rand, 600, ring, 1, true);
//		 new Churner(sim, rand, 5, ring, 1, false);

		ring.lookup();
		sim.run();

		System.out.println();
		System.out.println("Total number of queries\t: "
				+ ChordRing.lookups.length());
		System.out.println("Lookup Success Ratio\t: "
				+ ChordRing.lookups.stats().mean);

	}
}
