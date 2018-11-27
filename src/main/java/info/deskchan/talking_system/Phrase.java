package info.deskchan.talking_system;

import org.json.JSONArray;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;


public class Phrase {

	protected CharacterRange character;

	protected String phraseText;

	protected TagsMap tags;

	protected PhraseBlocks blocks;

	// null means default intent
	protected IntentList intentType;

	protected String spriteType;

	protected static final String DEFAULT_INTENT = "CHAT";
	protected static final String DEFAULT_SPRITE = "AUTO";

	public Phrase(String text) {
		this(text, null);
	}

	public Phrase(String text, IntentList intents) {
		phraseText = text.replace("\n", "");
		intentType = intents;
		character = new CharacterRange();
		spriteType = DEFAULT_SPRITE;
		tags = null;
		blocks = new PhraseBlocks(phraseText);
	}

	public void setIntents(IntentList intents){
		if (intents.size() != 0 && !(intents.size() == 1 && intents.get(0).equals(DEFAULT_INTENT)))
			intentType = intents;
		else
			intentType = null;
	}

	public void setSpriteType(String text){
		if (text == null || text.length() == 0) return;
		spriteType = text.trim().replace("\n", "").toUpperCase();
	}

	protected static final String[] notTags = {"text", "intent", "purpose", "sprite", "timeout", "range"};

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
			phrase.setIntents(new IntentList(findInNode(list, "purpose").getTextContent()));
		} catch (Exception e) { }

		try {
			phrase.setIntents(new IntentList(findInNode(list, "intent").getTextContent()));
		} catch (Exception e) { }
		
		try {
			phrase.setSpriteType(findInNode(list, "sprite").getTextContent());
		} catch (Exception e) {
			phrase.spriteType = DEFAULT_SPRITE;
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
		if(tags == null) tags = new TagsMap();
		tags.put(tag, text);
	}

	public void setTags(String text){
		if(tags == null) tags = new TagsMap();
		tags.putFromText(text);
	}

	public Set<String> getTag(String name){
		return tags != null ? tags.get(name) : null;
	}

	public TagsMap getTags(){
		return tags;
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
						phrase.setIntents(new IntentList(array.getString(k)));
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

			appendTo(doc, mainNode, "intent", getIntentsAsString());
			if (!spriteType.equals(DEFAULT_SPRITE))
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

	public boolean hasIntent(String match){
		if (intentType == null)  return match.equals(DEFAULT_INTENT);
		return intentType.contains(match);
	}

	private static Node findInNode(NodeList list, String name) {
		for (int i = 0; i < list.getLength(); i++)
			if (list.item(i).getNodeName().equals(name))
				return list.item(i);

		return null;
	}

	/** Get timeout in seconds. */
	public int getTimeout(){
		try {
			Set<String> values = tags.get("timeout");
			return Integer.getInteger(values.iterator().next());
		} catch (Exception e){
			return 0;
		}
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("text", phraseText);

		map.put("characterImage", spriteType);
		map.put("intent", getIntents());

		if(tags != null)
			for(Map.Entry<String, Set<String>> tag : tags.entrySet()) {
				map.put(tag.getKey(), tag.getValue());
			}
		return map;
	}

	public Map<String, Object> toPreparedPhrase() {
		Map<String, Object> map = new HashMap<>();
		map.put("text", blocks.replace(phraseText));
		map.put("characterImage", spriteType);
		return map;
	}

	public String toString() {
		StringBuilder s = new StringBuilder("{" + phraseText + "} / intent: " + getIntentsAsString() + " / Range: { " + character.toString() + " }");
		if (!spriteType.equals(DEFAULT_SPRITE)) {
			s.append(" / SpriteType: " + spriteType);
		}
		if(tags != null)
			for(String tag : tags.keySet()) {
				s.append(" / " + tag + ": " + tags.getAsString(tag));
			}
		return s.toString();
	}

	public List<String> getIntents(){
		return intentType != null ? intentType : Arrays.asList(DEFAULT_INTENT);
	}

	public String getIntentsAsString(){
		if (intentType == null || intentType.size() == 0)
			return DEFAULT_INTENT;

		StringBuilder sb = new StringBuilder();
		for(String intent : intentType) {
			sb.append(intent); sb.append(", ");
		}
		sb.setLength(sb.length() - 2);
		return sb.toString();
	}

	public String getPhraseText(){
		return phraseText;
	}

}
