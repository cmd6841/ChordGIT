import java.util.Random;

/**
 * Class Data represents a data item in the Chord Distributed Hash Table. The
 * hash key of each Data object is randomly generated with a modulo hash
 * function given by the maximum size of the Chord ring.
 * 
 * @author Chinmay Dani
 * 
 */
public class Data {

	/**
	 * The hash key of the Data object as defined by the hash function.
	 */
	private int hashKey;

	/**
	 * The node in the Chord ring on which the data is stored.
	 */
	public Node node;

	/**
	 * Construct a new data object and generate its hash key.
	 * 
	 * @param base
	 *            the base of the Chord ring
	 * @param hashKeyLength
	 *            the number of bits in the hash key.
	 */
	public Data(int base, int hashKeyLength) {
		int maxSize = (int) Math.pow(base, hashKeyLength);
		this.hashKey = new Random().nextInt(maxSize);
	}

	/**
	 * Sets the node on which this object is stored.
	 * 
	 * @param node
	 *            the Node on which this object is stored.
	 */
	public void setNode(Node node) {
		this.node = node;
	}

	/**
	 * Returns the hash key of the Data object.
	 * 
	 * @return the hash key of the Data object.
	 */
	public int hashCode() {
		return hashKey;
	}

}
