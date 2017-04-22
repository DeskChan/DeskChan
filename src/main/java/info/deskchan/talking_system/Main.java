package info.deskchan.talking_system;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxy;
import info.deskchan.talking_system.presets.SimpleCharacterPreset;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * TODO
 * нужно написать хуки
 * нужно написать команды, чтобы плагины могли всем этим пользоваться
 * нужно возвращать характер по требованию
 */
public class Main implements Plugin {
	private static PluginProxy pluginProxy;
	
	private final static String MAIN_PHRASES_URL = "https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/%D0%9D%D0%BE%D0%B2%D1%8B%D0%B9%20%D1%84%D0%BE%D1%80%D0%BC%D0%B0%D1%82!A3:M800?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI";
	private final static String DEVELOPERS_PHRASES_URL =
			"https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/%D0%9D%D0%B5%D0%B2%D0%BE%D1%88%D0%B5%D0%B4%D1%88%D0%B5%D0%B5!A3:A800?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI";
	private CharacterPreset currentPreset;
	private EmotionsController emotionsController;
	
	private boolean applyInfluence = true;
	private Timer chatTimer;
	private int messageTimeout;
	private Quotes quotes;
	private PriorityQueue<Quote> quoteQueue;
	private PerkContainer perkContainer;
	
	@Override
	public boolean initialize(PluginProxy newPluginProxy) {
		pluginProxy = newPluginProxy;
		Properties properties = new Properties();
		emotionsController = new EmotionsController();
		quoteQueue = new PriorityQueue<>();
		perkContainer = new PerkContainer();
		quotes = new Quotes();
		currentPreset = new SimpleCharacterPreset();
		Influence.globalMultiplier = 0.05f;
		messageTimeout = 40000;
		try {
			InputStream ip = Files.newInputStream(pluginProxy.getDataDirPath().resolve("config.properties"));
			properties.load(ip);
			ip.close();
		} catch (Exception e) {
			properties = null;
			log("Cannot find file: " + pluginProxy.getDataDirPath().resolve("config.properties"));
			//log(e);
		}
		if (properties != null) {
			try {
				applyInfluence = properties.getProperty("applyInfluence").equals("1");
			} catch (Exception e) {
			}
			try {
				currentPreset = CharacterPreset.getFromJSON(new JSONObject(properties.getProperty("characterPreset")));
			} catch (Exception e) {
			}
			try {
				Influence.globalMultiplier = Float.valueOf(properties.getProperty("influenceMultiplier"));
			} catch (Exception e) {
			}
			try {
				messageTimeout = Integer.valueOf(properties.getProperty("messageTimeout"));
			} catch (Exception e) {
			}
		}
		log("Loaded options");
		chatTimer = new Timer();
		chatTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				operatePhraseRequest("CHAT");
			}
		}, messageTimeout, messageTimeout);
		pluginProxy.addMessageListener("talk:request", (sender, tag, data) -> {
			operatePhraseRequest((Map<String, Object>) data);
		});
		pluginProxy.addMessageListener("talk:make-influence", (sender, tag, data) -> {
			Map<String, Object> dat = (Map<String, Object>) data;
			float multiplier = 1;
			Object obj = dat.getOrDefault("multiplier", 1);
			if (obj instanceof String) {
				multiplier = Float.valueOf((String) obj);
			}
			if (obj instanceof Float) {
				multiplier = (Float) obj;
			}
			if (dat.getOrDefault("type", "character") != "emotional") {
				currentPreset.applyInfluence(Influence.CreateCharacterInfluence(
						(String) dat.getOrDefault("feature", "sympathy"),
						multiplier)
				);
			} else {
				emotionsController.applyInfluence(Influence.CreateEmotionInfluence(
						(String) dat.getOrDefault("emotion", "happiness"),
						multiplier)
				);
			}
		});
		pluginProxy.addMessageListener("talk:options-saved", (sender, tag, data) -> {
			saveOptions((Map<String, Object>) data);
		});
		pluginProxy.addMessageListener("talk:save_preset", (sender, tag, data) -> {
			try {
				currentPreset.saveInFile(getPresetsPath());
				HashMap<String, Object> list = new HashMap<String, Object>();
				list.put("text", "Success");
				pluginProxy.sendMessage("gui:show-notification", list);
			} catch (Exception e) {
				HashMap<String, Object> list = new HashMap<String, Object>();
				list.put("name", "Ошибка");
				list.put("text", "Error while writing file: " + e.getMessage());
				pluginProxy.sendMessage("gui:show-notification", list);
			}
		});
		pluginProxy.addMessageListener("talk:register-perk",
				(sender, tag, data) -> perkContainer.add((String) sender, (Map<String, Object>) data)
		);
		pluginProxy.addMessageListener("talk:create-quotes-base",
				(sender, tag, data) -> {
					Map<String, Object> da = (Map<String, Object>) data;
					Quotes.saveTo((String) da.getOrDefault("url", MAIN_PHRASES_URL), (String) da.getOrDefault("filename", "new_quotes"));
				}
		);
		pluginProxy.addMessageListener("talk:unregister-perk",
				(sender, tag, data) -> perkContainer.remove((String) sender)
		);
		pluginProxy.addMessageListener("talk:perk-answer",
				(sender, tag, data) -> perkContainer.getAnswerFromPerk(sender, (Map<String, Object>) data)
		);
		pluginProxy.sendMessage("DeskChan:register-simple-action", new HashMap<String, Object>() {{
			put("name", getString("say_phrase"));
			put("msgTag", "talk:request");
		}});
		updateOptionsTab();
		//currentPreset=CharacterPreset.getFromFile(pluginProxy.getDataDirPath(),"preset1");
		Quotes.saveTo(MAIN_PHRASES_URL, "main");
		Quotes.saveTo(DEVELOPERS_PHRASES_URL, "developers_base");
		quotes.load(pluginProxy.getDataDirPath(), currentPreset.quotesBaseList);
		operatePhraseRequest("HELLO");
		return true;
	}
	
	static String getListAsString(List<String> s) {
		if (s.size() == 0) {
			return "";
		}
		String ret = "";
		for (String n : s) {
			ret = ret + n + ";";
		}
		
		return ret.substring(0, ret.length() - 1);
	}
	
	static ArrayList<String> getListFromString(String s) {
		return new ArrayList<>(Arrays.asList(s.split(";")));
	}
	
	void operatePhraseRequest(Map<String, Object> data) {
		String purpose = "CHAT";
		if (data != null) {
			String np = (String) data.getOrDefault("purpose", null);
			if (np != null) {
				purpose = np;
			}
		}
		operatePhraseRequest(purpose);
	}
	
	void operatePhraseRequest(String purpose) {
		CharacterDefinite cd = currentPreset.getCharacter(emotionsController);
		quotes.update(cd);
		HashMap<String, Object> ret = quotes.getRandomQuote(purpose).toMap();
		if (ret.get("characterImage").equals("AUTO")) {
			String ci = emotionsController.getSpriteType();
			if (ci == null) {
				ci = cd.getSpriteType();
			}
			if (ci == null) {
				ci = "NORMAL";
			}
			ret.replace("characterImage", ci);
		}
		ret.replace("text", currentPreset.replaceTags((String) ret.get("text")));
		String t = perkContainer.send("text", (String) ret.get("text"));
		if (t != null) {
			ret.replace("text", t);
		}
		ret.put("priority", 1100);
		pluginProxy.sendMessage("gui:say", ret);
	}
	
	void updateOptionsTab() {
		pluginProxy.sendMessage("gui:setup-options-tab", new HashMap<String, Object>() {{
			put("name", getString("character"));
			put("msgTag", "talk:options-saved");
			List<HashMap<String, Object>> list = new LinkedList<HashMap<String, Object>>();
			list.add(new HashMap<String, Object>() {{
				put("id", "name");
				put("type", "TextField");
				put("label", getString("name"));
				put("value", currentPreset.name);
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "usernames");
				put("type", "TextField");
				put("label", getString("usernames_list"));
				put("value", getListAsString(currentPreset.usernames));
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "quotes");
				put("type", "TextField");
				put("label", getString("quotes_list"));
				put("value", getListAsString(currentPreset.quotesBaseList));
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "type");
				put("type", "ComboBox");
				put("label", getString("character_type"));
				String className = currentPreset.getClass().getSimpleName();
				int i = 0;
				for (i = 0; i < CharacterPreset.presetTypeList.size(); i++) {
					if (CharacterPreset.presetTypeList.get(i).equals(className)) {
						break;
					}
				}
				if (i >= CharacterPreset.presetTypeList.size()) {
					CharacterPreset.presetTypeList.add(className);
				}
				put("values", CharacterPreset.presetTypeList);
				put("value", i);
			}});
			list.add(new HashMap<String, Object>() {{
				put("type", "Label");
				put("label", getString("primary_character_values"));
			}});
			for (int i = 0; i < CharacterSystem.getFeatureCount(); i++) {
				HashMap<String, Object> ch = new HashMap<>();
				ch.put("id", CharacterSystem.getFeatureName(i));
				ch.put("label", getString(CharacterSystem.getFeatureName(i)));
				ch.put("value", currentPreset.MainCharacter.getValueString(i));
				ch.put("type", "TextField");
				list.add(ch);
			}
			list.add(new HashMap<String, Object>() {{
				put("id", "file");
				put("type", "FileField");
				put("label", getString("load_preset"));
				put("initialDirectory", getPresetsPath().toString());
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "message_interval");
				put("type", "Spinner");
				put("min", 10);
				put("max", 1000);
				put("step", 1);
				put("value", messageTimeout / 1000);
				put("label", getString("message_interval"));
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "save_preset");
				put("type", "Button");
				put("value", getString("save_preset"));
				put("msgTag", "talk:save_preset");
			}});
			put("controls", list);
		}});
	}
	
	void saveOptions(Map<String, Object> data) {
		String val = (String) data.getOrDefault("file", "");
		String errorMessage = "";
		if (val.isEmpty()) {
			try {
				currentPreset = CharacterPreset.getFromTypeName(
						CharacterPreset.presetTypeList.get((Integer) data.get("type"))
				);
			} catch (Exception e) {
				errorMessage += e.getMessage() + "\n";
			}
			try {
				val = (String) data.getOrDefault("name", "");
				if (!val.isEmpty()) {
					currentPreset.name = val;
				}
			} catch (Exception e) {
				errorMessage += e.getMessage() + "\n";
			}
			try {
				val = (String) data.getOrDefault("usernames", "");
				if (!val.isEmpty()) {
					currentPreset.usernames = getListFromString(val);
				}
			} catch (Exception e) {
				errorMessage += e.getMessage() + "\n";
			}
			try {
				val = (String) data.getOrDefault("quotes", "");
				if (!val.isEmpty()) {
					currentPreset.quotesBaseList = getListFromString(val);
				}
			} catch (Exception e) {
				errorMessage += e.getMessage() + "\n";
			}
			try {
				for (int i = 0; i < CharacterSystem.getFeatureCount(); i++) {
					val = (String) data.getOrDefault(CharacterSystem.getFeatureName(i), "");
					if (!val.isEmpty()) {
						currentPreset.MainCharacter.setValues(i, val);
					}
				}
			} catch (Exception e) {
				errorMessage += e.getMessage() + "\n";
			}
		} else {
			CharacterPreset newPreset = CharacterPreset.getFromFileUnsafe(Paths.get(val));
			if (newPreset == null) {
				errorMessage = "Wrong or corrupted file!";
			} else {
				currentPreset = newPreset;
			}
		}
		try {
			messageTimeout = (Integer) data.getOrDefault("message_interval", 40) * 1000;
		} catch (Exception e) {
			errorMessage += e.getMessage();
			messageTimeout = 40000;
		}
		if (errorMessage.length() > 1) {
			HashMap<String, Object> list = new HashMap<String, Object>();
			list.put("name", "Ошибка");
			list.put("text", errorMessage);
			pluginProxy.sendMessage("gui:show-notification", list);
			//System.out.println(errorMessage);
		}
		chatTimer.cancel();
		chatTimer = new Timer();
		chatTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				operatePhraseRequest("CHAT");
			}
		}, messageTimeout, messageTimeout);
		updateOptionsTab();
		quotes.clear();
		quotes.load(pluginProxy.getDataDirPath(), currentPreset.quotesBaseList);
		saveSettings();
	}
	
	void saveSettings() {
		Properties properties = new Properties();
		properties.setProperty("applyInfluence", applyInfluence ? "1" : "0");
		properties.setProperty("characterPreset", currentPreset.toJSON().toString());
		properties.setProperty("influenceMultiplier", String.valueOf(Influence.globalMultiplier));
		properties.setProperty("messageTimeout", String.valueOf(messageTimeout));
		try {
			OutputStream ip = Files.newOutputStream(pluginProxy.getDataDirPath().resolve("config.properties"));
			properties.store(ip, "config fot talking system");
			ip.close();
		} catch (IOException e) {
			log(e);
		}
	}
	
	private static final ResourceBundle strings =
			ResourceBundle.getBundle("info/deskchan/talking_system/talk-strings");
	
	static synchronized String getString(String key) {
		try {
			String s = strings.getString(key);
			return new String(s.getBytes("ISO-8859-1"), "UTF-8");
		} catch (Throwable e) {
			return key;
		}
	}
	
	static void log(String text) {
		pluginProxy.log(text);
	}
	
	static void log(Throwable e) {
		pluginProxy.log(e);
	}
	
	public static void sendToProxy(String tag, Map<String, Object> data) {
		pluginProxy.sendMessage(tag, data);
	}
	
	public static Path getDataDirPath() {
		return pluginProxy.getDataDirPath();
	}
	
	public static Path getPresetsPath() {
		Path path = pluginProxy.getRootDirPath().resolve("presets/");
		if (!Files.isDirectory(path)) {
			try {
				Files.createDirectories(path);
			} catch (Exception e) {
				log(e);
				return pluginProxy.getRootDirPath();
			}
		}
		return path;
	}
	
	@Override
	public void unload() {
		saveSettings();
		operatePhraseRequest("BYE");
	}
	
}
