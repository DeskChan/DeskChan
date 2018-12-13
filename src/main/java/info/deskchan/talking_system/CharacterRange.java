package info.deskchan.talking_system;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public float getCenter(){
		return (end + start) / 2.0F;
	}
	public float getRadius(){
		return (end - start) / 2.0F;
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

	CharacterRange(Map<String, List<Integer>> data) {
		range = new Range[getFeatureCount()];
		for (int i = 0; i < getFeatureCount(); i++) {
			List<Integer> line = data.get(getFeatureName(i));
			if (line == null) continue;
			range[i] = new Range(line.get(0), line.get(1));
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
		StringBuilder s = new StringBuilder("[");
		boolean f = false;
		for (int i = 0; i < getFeatureCount(); i++) {
			if (range[i].start != -BORDER || range[i].end != BORDER) {
				if (f) s.append(", ");
				else f = true;

				s.append(getFeatureName(i) + ": {" + range[i].start + ";" + range[i].end + "}");
			}
		}
		s.append("]");
		return s.toString();
	}

	public Map toMap(){
		Map<String, List<Integer>> map = new HashMap<>();
		for (int i = 0; i < getFeatureCount(); i++) {
			List<Integer> d = new ArrayList<>(2);
			d.add(range[i].start); d.add(range[i].end);
			map.put(getFeatureName(i), d);
		}
		return map;
	}

	public float[] toCentersArray(){
		float[] ar = new float[range.length];
		for (int i = 0; i < range.length; i++){
			ar[i] = range[i].getCenter();
		}
		return ar;
	}
}