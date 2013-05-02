import java.util.Random;
import java.util.UUID;

public class Data {
	private int hashcode;
	public static int maxSize = (int)Math.pow(2, 10);
	private String data;
	public Node node;

	public Data() {
		this.hashcode = new Random().nextInt(maxSize);
		this.data = UUID.randomUUID().toString();
	}
 
	public void setNode(Node node) {
		this.node = node;
	}
	
	public int hashCode() {
		return hashcode;
	}

	public String getContents() {
		return data;
	}

	public String toString() {
		return Integer.toString(hashcode) + "stored on: " + node;
	}
}
