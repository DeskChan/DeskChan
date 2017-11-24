package info.deskchan.talking_system;

import info.deskchan.core_utils.TextOperations;
import info.deskchan.talking_system.presets.SimpleCharacterPreset;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

public abstract class CharacterPreset {
	public String name;
	protected CharacterDefinite MainCharacter;
	public TextOperations.TagsContainer tags;
	public CharacterPreset() {
		tags=new TextOperations.TagsContainer();
		setStandart();
	}
	public ArrayList<String> quotesBaseList;
	public CharacterPreset(String jsonString) {
		JSONObject json;
		try {
			fillFromJSON(new JSONObject(jsonString));
		} catch (JSONException e) {
			setStandart();
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
	public void setTags(String text){
		tags=new TextOperations.TagsContainer(text);
	}
	public void fillFromJSON(JSONObject json) {
		if (json.has("name"))
			name = json.getString("name");
		else
			name = Main.getString("default_name");

		if (json.has("main"))
			MainCharacter = new CharacterDefinite(json.getJSONObject("main"));
		else
			MainCharacter = new CharacterDefinite();

		if (json.has("quotes"))
			quotesBaseList = listFromJSON(json, "quotes");

		if (quotesBaseList.size() == 0) {
			quotesBaseList = new ArrayList<>();
			quotesBaseList.add("main");
		}

		if (json.has("tags")) {
			json = json.getJSONObject("tags");
			tags = new TextOperations.TagsContainer();
			for (HashMap.Entry<String, Object> obj : json.toMap().entrySet()) {
				tags.put(obj.getKey(), (String) obj.getValue());
			}
		}
	}
	
	public void setStandart() {
		MainCharacter = new CharacterDefinite();
		name = Main.getString("default_name");
		quotesBaseList = new ArrayList<>();
		quotesBaseList.add("main");
		tags.put("gender: girl, userGender: boy, breastSize: small, species: ai, interests: anime, abuses: бака дурак извращенец");
	}
	public void setQuotesBases(ArrayList<String> list) {
		quotesBaseList = list;
	}
	public abstract CharacterDefinite getCharacter(EmotionsController emo);
	private static String[] requiredQuotesTags=new String[]{ "gender" , "species" , "interests" , "breastSize" , "userGender" };
	public boolean isTagsMatch(HashMap<String,Object> tagsToMatch){
		for(HashMap.Entry<String,Object> entry : tagsToMatch.entrySet()){
			boolean found=false;
			for(String reqTag : requiredQuotesTags)
				if(reqTag.equals(entry.getKey())){
					found=true;
					break;
				}
			if(!found) continue;
			if(!tags.isMatch(entry.getKey(),(List<String>)entry.getValue())) return false;
		}
		return true;
	}
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("main", MainCharacter.toJSON());

		JSONArray ar = new JSONArray();
		for (int i = 0; i < quotesBaseList.size(); i++) {
			ar.put(quotesBaseList.get(i));
		}
		JSONObject preset_tags=new JSONObject();
		for(Map.Entry<String,List<String>> entry : tags.entrySet()){
			StringBuilder sb=new StringBuilder();
			for(String arg : entry.getValue())
				sb.append("\""+arg+"\" ");
			sb.setLength(sb.length()-1);
			preset_tags.put(entry.getKey(),sb.toString());
		}
		json.put("quotes", ar);
		json.put("tags",preset_tags);
		json.put("type", this.getClass().getSimpleName());
		return json;
	}
	public Map<String,Object> toMap() {
		Map map = new HashMap<String,Object>();
		map.put("name", name);

		for(int i=0;i<CharacterSystem.getFeatureCount();i++)
			map.put(CharacterSystem.getFeatureName(i), MainCharacter.getValue(i));

		List<String> ar = new ArrayList<String>();
		ar.addAll(quotesBaseList);

		Map preset_tags=new HashMap<String,Object>(tags.toMap());
		map.put("quotes", ar);
		map.put("tags",preset_tags);
		return map;
	}
	public void inform(){
		Main.getPluginProxy().sendMessage("talk:character-updated",toMap());
	}
	public void applyInfluence(ArrayList<Influence> influences) {
		MainCharacter.applyInfluence(influences);
	}
	
	public void applyInfluence(Influence influence) {
		MainCharacter.applyInfluence(influence);
	}
	
	public static CharacterPreset getFromTypeName(String typeName) {
		CharacterPreset cp = null;
		try {
			Class<?> c = Class.forName("info.deskchan.talking_system.presets." + typeName);
			cp = (CharacterPreset) c.newInstance();
		} catch (Exception e) {
			cp = new SimpleCharacterPreset();
		}
		
		return cp;
	}
	public List<String> getUsernames(){
		return tags.get("usernames");
	}
	public String replaceTags(String quote) {
		String ret = quote;
		if (name!=null) {
			ret = ret.replaceAll("%NAME%", name);
		} else ret = ret.replaceAll("%NAME%", Main.getString("default_name"));
		List<String> list=tags.get("usernames");
		if (list!=null && list.size() > 0) {
			ret = ret.replaceAll("%USERNAME%", list.get(new Random().nextInt(list.size())));
		} else ret = ret.replaceAll("%USERNAME%",Main.getString("default_username"));
		list=tags.get("abuses");
		if (list!=null && list.size() > 0) {
			ret = ret.replaceAll("%ABUSE%", list.get(new Random().nextInt(list.size())));
		} else ret = ret.replaceAll("%ABUSE%",Main.getString("default_abuse"));
		ret = ret.replaceAll("%TIME%", new SimpleDateFormat("HH:mm").format(new Date()));
		ret = ret.replaceAll("%DATE%", new SimpleDateFormat("d LLLL").format(new Date()));
		ret = ret.replaceAll("%YEAR%", new SimpleDateFormat("YYYY").format(new Date()));
		ret = ret.replaceAll("%WEEKDAY%", new SimpleDateFormat("EEEE").format(new Date()));
		return ret;
	}
	
	public static CharacterPreset getFromFile(Path path) {
		CharacterPreset cp = null;
		try {
			String str = new String(Files.readAllBytes(path));
			cp = getFromJSON(XML.toJSONObject(str).getJSONObject("preset"));
		} catch (Exception e) {
			Main.log(e);
			cp = new SimpleCharacterPreset();
		}
		return cp;
	}
	
	public void saveInFile(Path path) throws IOException {
		String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<preset>"
				+ XML.toString(toJSON()).replace("&quot;","\"") + "</preset>";
		xml = xml.replace("><", ">\n<");
		Writer out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(path.resolve("saved.preset").toFile()), "UTF-8"));
		try {
			out.write(xml);
		} catch (Exception e) {
			Main.log("Error while writing preset file");
		} finally {
			out.close();
		}
	}
	
	public static CharacterPreset getFromFileUnsafe(Path path) {
		CharacterPreset cp = null;
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
			cp = getFromJSON(obj.getJSONObject("preset"));
		} catch (Exception e) {
			Main.log(e);
		}
		return cp;
	}
	
	public static CharacterPreset getFromJSON(JSONObject obj) {
		CharacterPreset cp = null;
		try {
			String type = obj.getString("type");
			Class<?> c = Class.forName("info.deskchan.talking_system.presets." + type);
			cp = (CharacterPreset) c.newInstance();
		} catch (Exception e) {
			cp = new SimpleCharacterPreset();
		}
		
		cp.fillFromJSON(obj);
		return cp;
	}
	
	public static ArrayList<String> presetTypeList = new ArrayList<String>() {{
		add("SimpleCharacterPreset");
		add("EmotionalCharacterPreset");
		add("TTriggerEmotionalCharacterPreset");
	}};
}
