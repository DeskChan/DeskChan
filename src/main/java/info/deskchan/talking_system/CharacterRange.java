package info.deskchan.talking_system;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

class Range {

	public final int start;
	public final int end;

	public Range(String text) {
		String[] sp = text.split("\\s*[\\|\\\\\\/]\\s*");

		int st, en;
		try {
			st = Math.max(Integer.valueOf(sp[0]), -CharacterFeatures.BORDER);
		} catch (Exception e) {
			st = -CharacterFeatures.BORDER;
		}
		try {
			en = Math.min(Integer.valueOf(sp[1]), CharacterFeatures.BORDER);
		} catch (Exception e) {
			en = CharacterFeatures.BORDER;
		}
		start = st;  end = en;
	}

	public Range(int st, int en) {
		if (st < -CharacterFeatures.BORDER ||
			st >  CharacterFeatures.BORDER ||
			en < -CharacterFeatures.BORDER ||
			en >  CharacterFeatures.BORDER) {
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

	public boolean match(int value) {
		return (value >= start && value <= end);
	}
	
	public Node toXMLNode(Document doc, String name) {
		Node node = doc.createElement(name);
		node.setTextContent(start + " | " + end);
		return node;
	}

}

public class CharacterRange extends CharacterFeatures {
	public Range[] range;
	
	CharacterRange(int[][] ranges) {
		range = new Range[getFeatureCount()];
		for (int i = 0; i < getFeatureCount(); i++) {
			try {
				range[i] = new Range(ranges[i][0], ranges[i][1]);
			} catch (Exception e) {
				range[i] = new Range(-BORDER, BORDER);
			}
		}
	}
	
	CharacterRange() {
		range = new Range[getFeatureCount()];
		for (int i = 0; i < getFeatureCount(); i++) {
			range[i] = new Range(-BORDER, BORDER);
		}
	}
	
	public Node toXMLNode(Document doc) {
		Node node = doc.createElement("range");
		boolean found = false;
		for (int i = 0; i < getFeatureCount(); i++) {
			if (range[i].start != -BORDER || range[i].end != BORDER) {
				node.appendChild(range[i].toXMLNode(doc, getFeatureName(i)));
				found = true;
			}
		}

		return found ? node : null;
	}
	
	public String toString() {
		StringBuilder s = new StringBuilder();
		boolean f = false;
		for (int i = 0; i < getFeatureCount(); i++) {
			if (range[i].start != -BORDER || range[i].end != BORDER) {
				if (f) s.append(", ");
				else f = true;

				s.append(getFeatureName(i) + ": {" + range[i].start + ";" + range[i].end + "}");
			}
		}
		return s.toString();
	}
}