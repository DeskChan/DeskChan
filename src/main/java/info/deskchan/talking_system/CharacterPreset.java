package info.deskchan.talking_system;

import info.deskchan.core.Path;
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
			e.printStackTrace();
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
					inform();
				}
			});
			return controller;
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

	/** Additional character and user features like gender, species, interests. **/
	public TagsMap tags;

	/** Phrases list. **/
	public PhrasesPackList phrases;

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
	public void setTags(String text) {
		tags = new TagsMap(text);
	}

	public void setTags(Map newTags) {
		tags = new TagsMap(newTags);
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

		phrases = new PhrasesPackList();
		if (json.has("phrases")){
			List<String> quotesFiles = listFromJSON(json, "phrases");
			if (quotesFiles.size() > 0){
				phrases.set(quotesFiles);
			}
		}
		phrases.reload();

		emotionState = getDefaultEmotionsController();
		try {
            emotionState.setFromJSON(json.getJSONObject("emotions"));
        } catch (Exception e){ }

		tags = new TagsMap();
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
		name = "DeskChan";
		phrases = new PhrasesPackList();
		tags = new TagsMap();
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

		json.put("phrases", phrases.toPacksList(PhrasesPack.PackType.USER, PhrasesPack.PackType.INTENT_DATABASE));
		JSONObject preset = new JSONObject();
		preset.put("preset", json);
		return preset;
	}

	public Map<String, Object> toInformationMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("name", name);

		for(int i = 0; i < CharacterFeatures.getFeatureCount(); i++)
			map.put(CharacterFeatures.getFeatureName(i), character.getValue(i));

		map.put("phrases", phrases.toPacksList(PhrasesPack.PackType.USER, PhrasesPack.PackType.INTENT_DATABASE));
		map.put("tags", tags);
		map.put("emotion", emotionState.getCurrentEmotionName());
		return map;
	}

	public Runnable onChange = null;
	protected void inform(){
		if (onChange != null) onChange.run();
	}

	public static CharacterPreset getFromFile(Path path) {
		try {
			String str = new String(path.readAllBytes());
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
			OutputStreamWriter stream = new OutputStreamWriter(new FileOutputStream(path.resolve(name + ".preset")), "UTF-8");
			t.transform(new DOMSource(doc), new StreamResult(stream));
		} catch(Exception ex){
			Main.log(ex);
			Main.log("Error while writing preset file");
		}
	}
	
	public static CharacterPreset getFromFileUnsafe(File path) {
		try {
			if (!path.isAbsolute())
				path = Main.getPluginProxy().getAssetsDirPath().resolve("presets").resolve(path.getName());
			BufferedReader in = new BufferedReader(
					new InputStreamReader(new FileInputStream(path), "UTF-8")
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
			try {
                ar = new JSONArray(sa);
            } catch (Exception e){
			    list.add(sa);
            }

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

	@Override
	public int hashCode() {
		int hash = name.hashCode();

		CharacterController cc = emotionState.construct(character);
		for (int i = 0; i < CharacterFeatures.getFeatureCount(); i++)
			hash += cc.getValue(i) * (5 + i);

		hash += emotionState.getCurrentEmotionName() != null ? emotionState.getCurrentEmotionName().hashCode() : 0;

		hash += tags.dataHashCode();

		hash += phrases.hashCode();
		for (PhrasesPack pack : phrases) {
			hash += pack.hashCode();
		}

		return hash;
	}
}
