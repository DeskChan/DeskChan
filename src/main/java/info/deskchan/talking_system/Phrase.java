package info.deskchan.talking_system;


import info.deskchan.core_utils.TextOperations;
import org.json.JSONArray;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.*;


public class Phrase {

	protected CharacterRange character;

	protected String phraseText;

	protected int timeout;

	protected TextOperations.TagsMap tags;

	// Array of { block_name of String, start of Int, end of Int, params.. of String/Integer/Float }
	protected Object[][] blocks;

	// null means default intent
	protected List<String> intentType;

	protected String spriteType;

	protected static final String DEFAULT_INTENT = "CHAT";
	protected static final String DEFAULT_SPRITE = "AUTO";

	public Phrase(String text) {
		phraseText = text.replace("\n", "");
		intentType = null;
		character = new CharacterRange();
		spriteType = DEFAULT_SPRITE;
		timeout = 0;
		tags = null;
		setBlocks();
	}

	public void setIntentType(String text){
		if (text == null || text.length() == 0) return;

		List<String> intents = new ArrayList<>();
		for(String intent : text.replace("\n", "").split(",")) {
			intent = intent.trim().toUpperCase();
			if (intent.length() > 0) intents.add(intent);
		}
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
			phrase.setIntentType(findInNode(list, "purpose").getTextContent());
		} catch (Exception e) { }

		try {
			phrase.setIntentType(findInNode(list, "intent").getTextContent());
		} catch (Exception e) { }
		
		try {
			phrase.setSpriteType(findInNode(list, "sprite").getTextContent());
		} catch (Exception e) {
			phrase.spriteType = DEFAULT_SPRITE;
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

	public TextOperations.TagsMap getTags(){
		return tags;
	}


	static class Block {
		int start, end;
		String text;
		Block(String t, int s, int e){ text = t; start = s; end = e; }
		List<Object> toList(){
			List<Object> list = new ArrayList<>();
			String[] parts = text.split(",");
			list.add(parts[0]); list.add(new Integer(start)); list.add(new Integer(end));
			for (int i=1; i<parts.length; i++) list.add(parts[i]);
			return list;
		}
	}
	protected void setBlocks(){
		String input = phraseText;

		ArrayList<Block> stringBlocks = new ArrayList<> ();

		for (int i=0, s=0, state=1; i<input.length(); i++) {
			if (state == 1 && input.charAt(i) == '{') {
				state = 2;
				s = i;
			} else if (state == 2 && input.charAt(i) == '}') {
				state = 1;
				stringBlocks.add(new Block(input.substring(s+1, i), s, i+1));
			}
		}

		ArrayList<List<Object>> tempBlocks = new ArrayList<> ();
		for (int i=0; i<stringBlocks.size(); i++){
			String block = stringBlocks.get(i).text;
			if(block.trim().length() == 0) continue;

			if (!block.contains("(")) {
				tempBlocks.add(stringBlocks.get(i).toList());
				continue;
			}

			for (int j=0; j<block.length(); j++) {
				if (block.charAt(j) == '(') {
					block = block.substring(0, j) + "," + block.substring(j+1);
				} else if (block.charAt(j) == ')' || block.charAt(j) == '"') {
					block = block.substring(0, j) + block.substring(j+1);
					j--;
				}
			}

			stringBlocks.get(i).text = block;
			tempBlocks.add(stringBlocks.get(i).toList());
		}

		blocks = new Object[tempBlocks.size()][];
		for (int i=0; i<tempBlocks.size(); i++){
			List<Object> block = new ArrayList<>();
			for (Object _item : tempBlocks.get(i)){
				String item = _item.toString().trim();
				try {
					block.add(Integer.parseInt(item));
				} catch (Exception e) {
					//System.out.println(item + " " + e.getClass().getSimpleName() + " " + e.getMessage());
					try {
						block.add(Float.parseFloat(item));
					} catch (Exception er) {
						//System.out.println(item + " " + er.getClass().getSimpleName() + " " + er.getMessage());
						block.add(item);
					}
				}
			}

			blocks[i] = block.toArray(new Object[block.size()]);
		}
	}


	protected void appendTo(Document doc, Node target, String name, String text) {
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
						phrase.setIntentType(array.getString(k));
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

	public boolean hasIntent(String match){
		if (intentType == null)  return match.equals(DEFAULT_INTENT);
		return intentType.contains(match);
	}

	protected static Node findInNode(NodeList list, String name) {
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
		map.put("intent", getIntents());
		map.put("hash", this.hashCode());
		map.put("blocks", blocks);

		if(tags != null)
			for(Map.Entry<String, Set<String>> tag : tags.entrySet()) {
				map.put(tag.getKey(), tag.getValue());
			}
		return map;
	}

	public String toString() {
		StringBuilder s = new StringBuilder("{" + phraseText + "} / intent: " + getIntentsAsString() + " / Range: { " + character.toString() + " }");
		if (!spriteType.equals(DEFAULT_SPRITE)) {
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

	public List<String> getIntents(){
		return intentType != null ? intentType : Arrays.asList(DEFAULT_INTENT);
	}

	public String getIntentsAsString(){
		if (intentType == null || intentType.size() == 0)
			return DEFAULT_INTENT;

		StringBuilder sb = new StringBuilder();
		for(String intent : intentType)
			sb.append(intent + ", ");
		sb.setLength(sb.length() - 2);
		return sb.toString();
	}

	public String getPhraseText(){
		return phraseText;
	}
}
