package info.deskchan.talking_system;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Date;
import java.util.HashMap;


public class Quote {
	public CharacterRange character;
	public String quote;
	public String purposeType;
	public TextBooleanSet possibleHour;
	public TextBooleanSet possibleMonth;
	public TextBooleanSet possibleWeekDay;
	public int timeout;
	public String spriteType;
	
	public Quote(String quote) {
		this.quote = quote;
		purposeType = "CHAT";
		possibleHour = new TextBooleanSet(24);
		possibleMonth = new TextBooleanSet(12);
		possibleWeekDay = new TextBooleanSet(7);
		possibleMonth.offset = 1;
		possibleWeekDay.offset = 1;
		character = new CharacterRange();
		timeout = 0;
		spriteType = "AUTO";
	}
	
	private static Node findInNode(NodeList list, String name) {
		for (int i = 0; i < list.getLength(); i++) {
			if (list.item(i).getNodeName().equals(name)) {
				return list.item(i);
			}
		}
		return null;
	}
	
	public String toString() {
		String s = "{" + quote + "} / Purpose: " + purposeType + " / Range: { " + character.toString() + " }";
		if (!spriteType.equals("AUTO")) {
			s += " / SpriteType: " + spriteType;
		}
		if (timeout > 0) {
			s += " / timeout: " + timeout;
		}
		if (!possibleHour.full()) {
			s += " / possibleHour: " + possibleHour.toString();
		}
		if (!possibleMonth.full()) {
			s += " / possibleMonth: " + possibleMonth.toString();
		}
		if (!possibleWeekDay.full()) {
			s += " / possibleWeekDay: " + possibleWeekDay.toString();
		}
		return s;
	}
	
	public HashMap<String, Object> toMap() {
		HashMap<String, Object> map = new HashMap<>();
		map.put("text", quote);
		if (timeout != 0) {
			map.put("timeout", timeout);
		}
		map.put("characterImage", spriteType);
		return map;
	}
	
	public static Quote create(Node node) {
		NodeList list = node.getChildNodes();
		String p;
		Node n;
		try {
			p = findInNode(list, "text").getTextContent();
		} catch (Exception e) {
			return null;
		}
		if (p.length() < 2) {
			return null;
		}
		
		Quote q = new Quote(p);
		
		try {
			q.purposeType = findInNode(list, "purpose").getTextContent();
		} catch (Exception e) {
			q.purposeType = "CHAT";
		}
		
		try {
			q.spriteType = findInNode(list, "sprite").getTextContent();
		} catch (Exception e) {
			q.spriteType = "AUTO";
		}
		n = findInNode(list, "possibleHour");
		if (n != null) {
			q.possibleHour.fillFromString(n.getTextContent());
		}
		
		n = findInNode(list, "possibleMonth");
		if (n != null) {
			q.possibleMonth.fillFromString(n.getTextContent());
		}
		
		n = findInNode(list, "possibleWeekDay");
		if (n != null) {
			q.possibleWeekDay.fillFromString(n.getTextContent());
		}
		
		try {
			q.timeout = Integer.valueOf(findInNode(list, "timeout").getTextContent());
		} catch (Exception e) {
			q.timeout = 0;
		}
		
		try {
			Node range = findInNode(list, "range");
			for (int i = 0; i < CharacterSystem.getFeatureCount(); i++) {
				try {
					p = findInNode(range.getChildNodes(), CharacterSystem.getFeatureName(i)).getTextContent();
					String[] sp = p.split(" \\| ");
					int a1, a2;
					try {
						a1 = Integer.valueOf(sp[0]);
					} catch (Exception e) {
						a1 = -10;
					}
					try {
						a2 = Integer.valueOf(sp[1]);
					} catch (Exception e) {
						a2 = 10;
					}
					q.character.range[i] = new Range(a1, a2);
				} catch (Exception e) {
				}
			}
		} catch (Exception e) {
		}
		return q;
	}
	
	private void AppendTo(Document doc, Node target, String name, String text) {
		Node n = doc.createElement(name);
		n.setTextContent(text);
		target.appendChild(n);
	}
	
	public Node toXMLNode(Document doc) {
		Node mainNode = doc.createElement("quote");
		try {
			AppendTo(doc, mainNode, "text", quote);
			if (timeout > 0) {
				AppendTo(doc, mainNode, "timeout", String.valueOf(timeout));
			}
			AppendTo(doc, mainNode, "purpose", purposeType);
			if (!spriteType.equals("AUTO")) {
				AppendTo(doc, mainNode, "sprite", spriteType);
			}
			if (!possibleHour.full()) {
				AppendTo(doc, mainNode, "possibleHour", possibleHour.toString());
			}
			if (!possibleMonth.full()) {
				AppendTo(doc, mainNode, "possibleMonth", possibleMonth.toString());
			}
			if (!possibleWeekDay.full()) {
				AppendTo(doc, mainNode, "possibleWeekDay", possibleWeekDay.toString());
			}
			Node c = character.toXMLNode(doc);
			if (c != null) {
				mainNode.appendChild(c);
			}
		} catch (Exception e) {
			//Main.log(e);
		}
		return mainNode;
	}
	
	public boolean matchToCharacter(CharacterDefinite target) {
		for (int i = 0, l = CharacterSystem.getFeatureCount(); i < l; i++) {
			if (!character.range[i].Match(target.getValue(i))) {
				return false;
			}
		}
		return true;
	}
	
	private Date last_usage;
	
	public void UpdateLastUsage() {
		last_usage = new Date();
	}
	
	public boolean noTimeout() {
		if (last_usage == null) {
			return true;
		}
		return (new Date().getTime() - last_usage.getTime() > timeout * 1000);
	}
}
