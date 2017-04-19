package info.deskchan.talking_system;

import info.deskchan.talking_system.presets.SimpleCharacterPreset;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public abstract class CharacterPreset {
	public String name;
	protected ArrayList<String> usernames;
	protected ArrayList<String> abuses = new ArrayList<String>() {{
		add("идиот");
		add("придурок");
		add("дурак");
		add("козёл");
		add("бака");
	}};
	protected ArrayList<String> quotesBaseList;
	protected ArrayList<String> interestsBaseList;
	protected CharacterDefinite MainCharacter;
	
	public CharacterPreset() {
		setStandart();
	}
	
	public CharacterPreset(String jsonString) {
		JSONObject json;
		try {
			fillFromJSON(new JSONObject(jsonString));
		} catch (JSONException e) {
			setStandart();
		}
	}
	
	protected ArrayList<String> fillListFromJSON(JSONObject obj, String arrayname) {
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
	
	public void fillFromJSON(JSONObject json) {
		name = json.getString("name");
		if (name.isEmpty()) {
			name = "noname";
		}
		usernames = fillListFromJSON(json, "usernames");
		if (usernames.size() == 0) {
			usernames = new ArrayList<>();
			usernames.add("Хозяин");
		}
		interestsBaseList = fillListFromJSON(json, "interests");
		quotesBaseList = fillListFromJSON(json, "quotes");
		if (quotesBaseList.size() == 0) {
			quotesBaseList = new ArrayList<>();
			quotesBaseList.add("main");
		}
		MainCharacter = new CharacterDefinite(json.getJSONObject("main"));
	}
	
	public void setStandart() {
		MainCharacter = new CharacterDefinite();
		name = "noname";
		usernames = new ArrayList<>();
		usernames.add("Хозяин");
		quotesBaseList = new ArrayList<>();
		quotesBaseList.add("main");
		interestsBaseList = new ArrayList<>();
	}
	
	public void setUsernames(ArrayList<String> list) {
		usernames = list;
	}
	
	public void setQuotesBases(ArrayList<String> list) {
		quotesBaseList = list;
	}
	
	public void setInterests(ArrayList<String> list) {
		interestsBaseList = list;
	}
	
	public abstract CharacterDefinite getCharacter(EmotionsController emo);
	
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		json.put("name", name);
		json.put("main", MainCharacter.toJSON());
		
		JSONArray ar = new JSONArray();
		for (int i = 0; i < usernames.size(); i++) {
			ar.put(usernames.get(i));
		}
		json.put("usernames", ar);
		
		ar = new JSONArray();
		for (int i = 0; i < quotesBaseList.size(); i++) {
			ar.put(quotesBaseList.get(i));
		}
		json.put("quotes", ar);
		
		ar = new JSONArray();
		for (int i = 0; i < interestsBaseList.size(); i++) {
			ar.put(interestsBaseList.get(i));
		}
		json.put("interests", ar);
		
		json.put("type", this.getClass().getSimpleName());
		return json;
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
	
	public String replaceTags(String quote) {
		String ret = quote;
		if (usernames.size() > 0) {
			ret = ret.replaceAll("%USERNAME%", usernames.get(new Random().nextInt(usernames.size())));
		}
		if (abuses.size() > 0) {
			ret = ret.replaceAll("%ABUSE%", abuses.get(new Random().nextInt(abuses.size())));
		}
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
	
	public static CharacterPreset getFromFileUnsafe(Path path) {
		CharacterPreset cp = null;
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile()), "UTF-8"));
			String str = "", str2;
			while ((str2 = in.readLine()) != null) {
				str += str2 + "\n";
			}
			JSONObject obj = XML.toJSONObject(str);
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
