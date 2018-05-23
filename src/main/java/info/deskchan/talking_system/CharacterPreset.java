package info.deskchan.talking_system;

import info.deskchan.core_utils.TextOperations;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public class CharacterPreset {

	/** Character name. **/
	public String name;

	/** Character state controller. **/
	protected CharacterController character;
	protected static Class defaultCharacterController = StandardCharacterController.class;
	protected static CharacterController getDefaultCharacterController(){
		try {
			return (CharacterController) defaultCharacterController.newInstance();
		} catch (Exception e){
			return null;
		}
	}


	/** Current emotion state. **/
	protected EmotionsController emotionState;
	protected static Class defaultEmotionsController = StandardEmotionsController.class;
	protected EmotionsController getDefaultEmotionsController(){
		try {
			EmotionsController controller = (EmotionsController) defaultEmotionsController.newInstance();
			controller.setUpdater(new EmotionsController.UpdateHandler() {
				@Override
				public void onUpdate() {
					updatePhrases();
					inform();
				}
			});
			return controller;
		} catch (Exception e){
			return null;
		}
	}

	/** Additional character and user features like gender, species, interests. **/
	public TextOperations.TagsMap<String, Set<String>> tags;

	/** Phrases list. **/
	public PhrasesList phrases;

	/** New preset with standard info. **/
	public CharacterPreset() {
		setDefault();
		inform();
	}

	/** New preset from JSON. **/
	public CharacterPreset(JSONObject json) {
		try {
			setFromJSON(json);
		} catch (JSONException e) {
			Main.log(e);
			setDefault();
		}
		inform();
	}

	/** Set preset tags from its string representation. **/
	public void setTags(String text){
		tags = new TextOperations.TagsMap(text);
	}

	/** Set preset from JSON. **/
	public void setFromJSON(JSONObject json) {
		if (json.has("preset"))
			json = json.getJSONObject("preset");

		if (json.has("name"))
			name = json.getString("name");
		else
			name = Main.getString("default_name");

		character = getDefaultCharacterController();
		if (json.has("character"))
			character.setFromJSON(json.getJSONObject("character"));

		if (json.has("phrases")){
			phrases = new PhrasesList(character);
			List<String> quotesFiles = listFromJSON(json, "phrases");
			if (quotesFiles.size() > 0){
				phrases.set(quotesFiles);
			}
		} else {
			phrases = PhrasesList.getDefault(character);
		}
		phrases.reload();

		emotionState = getDefaultEmotionsController();
		if (json.has("emotions"))
			emotionState.setFromJSON(json.getJSONObject("emotions"));

		tags = new TextOperations.TagsMap();
		if (json.has("tags")) {
			json = json.getJSONObject("tags");
			for (HashMap.Entry<String, Object> obj : json.toMap().entrySet()) {
				tags.put(obj.getKey(), (String) obj.getValue());
			}
		}
	}

	/** Set default preset. **/
	public void setDefault() {
		character = getDefaultCharacterController();
		emotionState = getDefaultEmotionsController();
		name = Main.getString("default_name");
		phrases = PhrasesList.getDefault(character);
		tags = new TextOperations.TagsMap<>();
		tags.putFromText("gender: girl, userGender: boy, breastSize: small, species: ai, interests: anime, abuses: бака дурак извращенец");
	}

	/** Update phrases with preset info. **/
	public void updatePhrases(){
		phrases.update(emotionState.construct(character));
	}

	/** Check default tags. **/
	private static final String[] defaultTags = new String[]{ "gender" , "species" , "interests" , "breastSize" , "userGender" };
	public boolean tagsMatch(Map<String,Object> phrase){
		for (String tag : defaultTags)
			if (phrase.containsKey(tag) && !tags.match(tag, phrase.get(tag).toString())) return false;

		if (!emotionState.tagsMatch(phrase)) return false;

		return true;
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("character", character.toJSON());
		json.put("emotions", emotionState.toJSON());

		JSONObject preset_tags = new JSONObject();
		for(String key : tags.keySet())
			preset_tags.put(key, tags.getAsString(key));
		json.put("tags", preset_tags);

		json.put("phrases", phrases.toList(PhrasesPack.PackType.USER, PhrasesPack.PackType.DATABASE));
		JSONObject preset = new JSONObject();
		preset.put("preset", json);
		return preset;
	}

	public Map<String, Object> toInformationMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("name", name);

		for(int i = 0; i < CharacterFeatures.getFeatureCount(); i++)
			map.put(CharacterFeatures.getFeatureName(i), character.getValue(i));

		map.put("phrases", phrases.toList(PhrasesPack.PackType.USER, PhrasesPack.PackType.DATABASE));
		map.put("tags", tags);
		map.put("emotion", emotionState.getCurrentEmotionName());
		return map;
	}

	public void inform(){
		Main.getPluginProxy().sendMessage("talk:character-updated", toInformationMap());
	}

	private String getRandomItem(Collection<String> collection){
		int count = new Random().nextInt(collection.size());
		Iterator<String> it = collection.iterator();
		for(;count >0; count--) it.next();
		return it.next();
	}
	public String replaceTags(String phrase) {
		String ret = phrase;
		if (name != null) {
			ret = ret.replaceAll("\\{name\\}", name);
		} else {
			ret = ret.replaceAll("\\{name\\}", Main.getString("default_name"));
		}

		Collection<String> list = tags.get("usernames");
		if (list != null && list.size() > 0) {
			String item = getRandomItem(list);
			ret = ret.replaceAll("\\{user\\}", item);
			ret = ret.replaceAll("\\{userF\\}", item);
		} else {
			ret = ret.replaceAll("\\{user\\}", Main.getString("default_username"));
			ret = ret.replaceAll("\\{userF\\}", Main.getString("default_username"));
		}

		list = tags.get("abuses");
		if (list != null && list.size() > 0) {
			ret = ret.replaceAll("\\{abuse\\}", getRandomItem(list));
		} else ret = ret.replaceAll("\\{abuse\\}",Main.getString("default_abuse"));

		ret = ret.replaceAll("\\{time\\}", new SimpleDateFormat("HH:mm").format(new Date()));
		ret = ret.replaceAll("\\{date\\}", new SimpleDateFormat("d LLLL").format(new Date()));
		ret = ret.replaceAll("\\{year\\}", new SimpleDateFormat("YYYY").format(new Date()));
		ret = ret.replaceAll("\\{weekday\\}", new SimpleDateFormat("EEEE").format(new Date()));

		return ret;
	}

	public static CharacterPreset getFromFile(Path path) {
		try {
			String str = new String(Files.readAllBytes(path));
			return new CharacterPreset(XML.toJSONObject(str).getJSONObject("preset"));
		} catch (Exception e) {
			Main.log(e);
			return new CharacterPreset();
		}
	}
	
	public void saveInFile(Path path) throws IOException {
		try {
			Document doc = DocumentBuilderFactory.newInstance()
					                             .newDocumentBuilder()
					                             .parse(new InputSource(new StringReader(XML.toString(toJSON()))));

			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
			t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			t.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
			OutputStreamWriter stream = new OutputStreamWriter(new FileOutputStream(path.resolve("saved.preset").toFile()), "UTF-8");
			t.transform(new DOMSource(doc), new StreamResult(stream));
		} catch(Exception ex){
			Main.log(ex);
			Main.log("Error while writing preset file");
		}
	}
	
	public static CharacterPreset getFromFileUnsafe(Path path) {
		try {
			BufferedReader in = new BufferedReader(
					new InputStreamReader(new FileInputStream(path.toFile()), "UTF-8")
			);
			final StringBuilder str = new StringBuilder();
			String str2;
			while ((str2 = in.readLine()) != null) {
				str.append(str2);
				str.append("\n");
			}
			JSONObject obj = XML.toJSONObject(str.toString());
			return new CharacterPreset(obj.getJSONObject("preset"));
		} catch (Exception e) {
			Main.log(e);
			return new CharacterPreset();
		}
	}

	protected ArrayList<String> listFromJSON(JSONObject obj, String arrayname) {
		ArrayList<String> list = new ArrayList<>();
		if (obj == null || !obj.has(arrayname)) {
			return list;
		}

		JSONArray ar = null;
		if (obj.get(arrayname) instanceof String) {
			String sa = obj.getString(arrayname);
			if (sa.charAt(0) != '[') {
				sa = "[" + sa;
			}
			if (sa.charAt(sa.length() - 1) != '[') {
				sa = sa + "]";
			}
			ar = new JSONArray(sa);
		}
		if (obj.get(arrayname) instanceof JSONArray) {
			ar = obj.getJSONArray(arrayname);
		}
		if (ar == null) {
			return list;
		}
		for (int i = 0; i < ar.length(); i++) {
			list.add(ar.getString(i));
		}
		return list;
	}

	public String getDefaultSpriteType(){
		String characterImage = character.getDefaultSpriteType();

		if (characterImage == null)
			characterImage = emotionState.getDefaultSpriteType();

		return characterImage;
	}

}
