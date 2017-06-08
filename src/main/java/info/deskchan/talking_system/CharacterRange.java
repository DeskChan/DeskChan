package info.deskchan.talking_system;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

class Range {
	public Range(int st, int en) {
		if (st < -10 || st > 10 || en < -10 || en > 10) {
			throw new IllegalArgumentException("wrong range for quote");
		}
		if (st > en) {
			int t = st;
			st = en;
			en = t;
		}
		start = st;
		end = en;
	}
	
	public int start;
	public int end;
	
	public boolean Match(int value) {
		return (value >= start && value <= end);
	}
	
	public Node toXMLNode(Document doc, String name) {
		Node node = doc.createElement(name);
		node.setTextContent(start + " | " + end);
		return node;
	}
}

public class CharacterRange extends CharacterSystem {
	public Range[] range;
	
	CharacterRange(int[][] ranges) {
		range = new Range[featureCount];
		for (int i = 0; i < featureCount; i++) {
			try {
				range[i] = new Range(ranges[i][0], ranges[i][1]);
			} catch (Exception e) {
				range[i] = new Range(-10, 10);
			}
		}
	}
	
	CharacterRange() {
		range = new Range[featureCount];
		for (int i = 0; i < featureCount; i++) {
			range[i] = new Range(-10, 10);
		}
	}
	
	public Node toXMLNode(Document doc) {
		Node node = doc.createElement("range");
		boolean found = false;
		for (int i = 0; i < featureCount; i++) {
			if (range[i].start != -10 || range[i].end != 10) {
				node.appendChild(range[i].toXMLNode(doc, getFeatureName(i)));
				found = true;
			}
		}
		if (found) {
			return node;
		}
		return null;
	}
	
	public String toString() {
		String s = "";
		boolean f = false;
		for (int i = 0; i < featureCount; i++) {
			if (range[i].start != -10 || range[i].end != 10) {
				if (f) {
					s += ", ";
				} else {
					f = true;
				}
				s += getFeatureName(i) + ": {" + range[i].start + ";" + range[i].end + "}";
			}
		}
		return s;
	}
}