package info.deskchan.talking_system;


import info.deskchan.core_utils.TextOperations;
import org.json.JSONArray;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;


public class Phrase {

	private CharacterRange character;

	private String phraseText;

	private int timeout;

	private TextOperations.TagsMap<String, Set<String>> tags;

	// null = "CHAT"
	private List<String> purposeType;

	private String spriteType;

	private static final String defaultPurpose = "CHAT";
	private static final String defaultSprite = "AUTO";

	public Phrase(String text) {
		phraseText = text.replace("\n", "");
		purposeType = null;
		character = new CharacterRange();
		spriteType = defaultSprite;
		timeout = 0;
		tags = null;
	}

	public void setPurposeType(String text){
		if (text == null || text.length() == 0) return;

		List<String> purposes = new ArrayList<>();
		for(String purpose : text.replace("\n", "").split(",")) {
			purpose = purpose.trim().toUpperCase();
			if (purpose.length() > 0) purposes.add(purpose);
		}
		if (purposes.size() != 0 && !(purposes.size() == 1 && purposes.get(0).equals(defaultPurpose)))
			purposeType = purposes;
		else
			purposeType = null;
	}

	public void setSpriteType(String text){
		if (text == null || text.length() == 0) return;
		spriteType = text.trim().replace("\n", "").toUpperCase();
	}

	private static final String[] notTags = {"text", "purpose", "sprite", "timeout", "range"};

	public static Phrase create(Node node) {
		NodeList list = node.getChildNodes();
		String p;
		try {
			p = findInNode(list, "text").getTextContent();
		} catch (Exception e) {
			return null;
		}
		if (p.length() < 2) return null;
		
		Phrase phrase = new Phrase(p);
		
		try {
			phrase.setPurposeType(findInNode(list, "purpose").getTextContent());
		} catch (Exception e) { }
		
		try {
			phrase.setSpriteType(findInNode(list, "sprite").getTextContent());
		} catch (Exception e) {
			phrase.spriteType = defaultSprite;
		}
		try {
			phrase.timeout = Integer.valueOf(findInNode(list, "timeout").getTextContent());
		} catch (Exception e) {
			phrase.timeout = 0;
		}

		try {
			Node range = findInNode(list, "range");
			for (int i = 0; i < CharacterFeatures.getFeatureCount(); i++) {
				try {
					phrase.character.range[i] = new Range(findInNode(range.getChildNodes(), CharacterFeatures.getFeatureName(i)).getTextContent());
				} catch (Exception e) { }
			}
		} catch (Exception e) { }

		phrase.tags = null;

		for (int i = 0, k; i < list.getLength(); i++) {
			if(list.item(i).getNodeName().charAt(0)=='#') continue;
			for (k = 0; k < notTags.length; k++)
				if (list.item(i).getNodeName().equals(notTags[k]))
					break;
			if(k < notTags.length) continue;
			phrase.setTag(list.item(i).getNodeName(),list.item(i).getTextContent());
		}
		return phrase;
	}

	public void setTag(String tag,String text){
		if(tags == null) tags = new TextOperations.TagsMap();
		tags.put(tag, text);
	}

	public void setTags(String text){
		if(tags == null) tags = new TextOperations.TagsMap();
		tags.putFromText(text);
	}

	public Set<String> getTag(String name){
		return tags != null ? tags.get(name) : null;
	}

	private void appendTo(Document doc, Node target, String name, String text) {
		Node n = doc.createElement(name);
		n.setTextContent(text);
		target.appendChild(n);
	}

	public static Phrase fromJSONArray(JSONArray array){
		try {
			Phrase phrase = null;
			int[][] range_values = new int[CharacterFeatures.getFeatureCount()][2];
			for (int k = 0; k < CharacterFeatures.getFeatureCount(); k++)
				range_values[k] = new int[]{ -CharacterFeatures.BORDER, CharacterFeatures.BORDER };

			for (int k = 0; k < array.length() && k < 12; k++) {
				switch (k) {
					case 0:
						phrase = new Phrase(array.getString(k));
					break;
					case 1:
						phrase.setSpriteType(array.getString(k));
					break;
					case 2:
						phrase.setPurposeType(array.getString(k));
					break;
					case 3: case 4: case 5: case 6: case 7: case 8: case 9: case 10: {
						Range range = new Range(array.getString(k));
						range_values[k - 3] = new int[]{range.start, range.end};
					}
					break;
					case 11:
						phrase.setTags(array.getString(11));
					break;
				}
			}
			phrase.character = new CharacterRange(range_values);
			return phrase;
		} catch (Exception u) {
			Main.log(u);
		}
		return null;
	}
	
	public Node toXMLNode(Document doc) {
		Node mainNode = doc.createElement("phrase");
		try {
			appendTo(doc, mainNode, "text", phraseText);
			if (timeout > 0)
				appendTo(doc, mainNode, "timeout", String.valueOf(timeout));

			appendTo(doc, mainNode, "purpose", getPurposesAsString());
			if (!spriteType.equals(defaultSprite))
				appendTo(doc, mainNode, "sprite", spriteType);

			if (tags != null) {
				for(String tag : tags.keySet()) {
					appendTo(doc, mainNode, tag, tags.getAsString(tag));
				}
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
	
	public boolean matchToCharacter(CharacterController target) {
		for (int i = 0, l = CharacterFeatures.getFeatureCount(); i < l; i++)
			if (!character.range[i].match(target.getValue(i)))
				return false;

		return true;
	}
	
	private Date last_usage;
	
	public void updateLastUsage() {
		last_usage = new Date();
	}
	
	public boolean noTimeout() {
		if (last_usage == null || timeout<=0) {
			return true;
		}
		return (new Date().getTime() - last_usage.getTime() > timeout * 1000);
	}

	public boolean purposeEquals(String match){
		if (purposeType == null)  return match.equals(defaultPurpose);
		return purposeType.contains(match);
	}

	private static Node findInNode(NodeList list, String name) {
		for (int i = 0; i < list.getLength(); i++)
			if (list.item(i).getNodeName().equals(name))
				return list.item(i);

		return null;
	}

	public HashMap<String, Object> toMap() {
		HashMap<String, Object> map = new HashMap<>();
		map.put("text", phraseText);
		if (timeout != 0)
			map.put("timeout", timeout);

		map.put("characterImage", spriteType);
		map.put("purpose", purposeType != null ? purposeType : Arrays.asList(defaultPurpose));
		map.put("hash", this.hashCode());

		if(tags != null)
			for(Map.Entry<String, Set<String>> tag : tags.entrySet()) {
				map.put(tag.getKey(), tag.getValue());
			}
		return map;
	}

	public String toString() {
		StringBuilder s = new StringBuilder("{" + phraseText + "} / Purpose: " + getPurposesAsString() + " / Range: { " + character.toString() + " }");
		if (!spriteType.equals(defaultSprite)) {
			s.append(" / SpriteType: " + spriteType);
		}
		if (timeout > 0) {
			s.append(" / Timeout: " + timeout);
		}
		if(tags != null)
			for(String tag : tags.keySet()) {
				s.append(" / " + tag + ": " + tags.getAsString(tag));
			}
		return s.toString();
	}

	private String getPurposesAsString(){
		if (purposeType == null || purposeType.size() == 0)
			return defaultPurpose;

		StringBuilder sb = new StringBuilder();
		for(String purpose : purposeType)
			sb.append(purpose + ", ");
		sb.setLength(sb.length() - 2);
		return sb.toString();
	}
}
