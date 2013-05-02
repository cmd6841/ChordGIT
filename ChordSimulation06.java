import java.awt.Color;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import edu.rit.numeric.AggregateXYSeries;
import edu.rit.numeric.ListSeries;
import edu.rit.numeric.plot.Plot;
import edu.rit.sim.Simulation;
import edu.rit.util.Random;

public class ChordSimulation06 {

	private static double meanQueryProcTime = 1.0;
	// private static double meanStabilizeTime = 0.01;
	private static int seed = 214141;
	private static Random rand;
	private static Simulation sim;
	private static ChordRing ring;
	private static int base = 2;
	private static int initialNodes = 32;
	private static int hashKeyLength = 10;

	public static void main(String[] args) {
		// base = Integer.parseInt(args[0]);
		// initialNodes = Integer.parseInt(args[1]);
		// meanQueryProcTime = Double.parseDouble(args[2]);
		// seed = Integer.parseInt(args[3]);

		ChordRing.verbose = false;
		Node.verbose = false;

		rand = Random.getInstance(seed);

		Data.maxSize = (int) Math.pow(base, hashKeyLength);

		initialNodes =  1000;//(int) (Math.pow(base, hashKeyLength) * 0.5);

		ListSeries stimes = new ListSeries();
		ListSeries ratiosWoS = new ListSeries();
		ListSeries ratiosWS = new ListSeries();

		System.out.println("Total number of queries\t: 50\n");
		System.out.println("\tStabilize OFF\t\tStabilize ON");
		System.out.println("churn\tLookup Success\t\tLookup Success");

		List<Data> dataList = new ArrayList<Data>();
		for (int i = 0; i < initialNodes * 1.5; i++) {
			Data data = new Data();
			dataList.add(data);
		}
		List<Data> queryList = new ArrayList<Data>();
		for (int i = 0; i < 100; i++) {
			Data query = dataList.get(new java.util.Random().nextInt(dataList
					.size()));
			queryList.add(query);
		}

		for (double churnrate = 0.1; churnrate <= 5.0; churnrate += 0.1) {
			stimes.add(churnrate);
			System.out.printf("%.2f", churnrate);
			sim = new Simulation();

			ChordRing.lookups = new ListSeries();

			ring = new ChordRing(base, hashKeyLength, initialNodes, sim, rand,
					meanQueryProcTime);
			for (Data data : dataList) {
				ring.addDataToNode(data);
			}
			for (Data data : queryList) {
				ring.addQuery(data.hashCode());
			}

			new Churner(sim, rand, churnrate, ring, 1, false);

			ring.lookup();
			sim.run();
			ratiosWoS.add(ChordRing.lookups.stats().mean);
			System.out.printf("\t%.3f\t\t\t", ChordRing.lookups.stats().mean);

			sim = new Simulation();

			ChordRing.lookups = new ListSeries();

			ring = new ChordRing(base, hashKeyLength, initialNodes, sim, rand,
					meanQueryProcTime);
			for (Data data : dataList) {
				ring.addDataToNode(data);
			}
			for (Data data : queryList) {
				ring.addQuery(data.hashCode());
			}

			new Churner(sim, rand, churnrate, ring, 0.005, true);

			ring.lookup();
			sim.run();
			ratiosWS.add(ChordRing.lookups.stats().mean);
			System.out.printf("%.3f\n", ChordRing.lookups.stats().mean);
		}

		System.out.println("Mean lookup success ratio : "
				+ ratiosWoS.stats().mean);
		System.out.println("Mean lookup success ratio : "
				+ ratiosWS.stats().mean);

		Plot plot = new Plot();

		plot = new Plot().rightMargin(36).xAxisTitle("Chord Base")
				.xAxisTickFormat(new DecimalFormat("0.0"))
				.yAxisTitle("Lookup Success Ratio")
				.yAxisTickFormat(new DecimalFormat("0.0")).seriesDots(null)
				.seriesColor(Color.BLUE)
				.xySeries(new AggregateXYSeries(stimes, ratiosWoS))
				.seriesColor(Color.RED)
				.xySeries(new AggregateXYSeries(stimes, ratiosWS));
		;

		plot.getFrame().setVisible(true);

	}
}
