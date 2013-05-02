import java.util.ArrayList;
import java.util.List;

public class FingerTable {
	private Node[] table;
	private ChordRing ring;

	public FingerTable(ChordRing ring) {
		this.table = new Node[ring.hashKeyLength];
		this.ring = ring;
	}

	public void update(int nodeKey) {
		for (int i = 0; i < table.length; i++) {
			table[i] = ring.getFingerTableEntry(nodeKey, i);
		}
	}

	public boolean updateEntry(int nodeKey, int i) {
		if (table[i] != ring.getFingerTableEntry(nodeKey, i)) {
			table[i] = ring.getFingerTableEntry(nodeKey, i);
			return true;
		}
		return false;
	}

	public int size() {
		return this.table.length;
	}

	public Node getIthEntry(int i) {
		return this.table[i];
	}

	public String string() {
		List<Node> temp = new ArrayList<Node>();
		for (Node node : table)
			temp.add(node);
		return temp.toString();
	}
}
