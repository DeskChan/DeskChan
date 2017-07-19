package info.deskchan.talking_system;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core.ResponseListener;
import info.deskchan.talking_system.presets.SimpleCharacterPreset;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main implements Plugin {
	private static PluginProxyInterface pluginProxy;
	
	private final static String MAIN_PHRASES_URL = "https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/%D0%9D%D0%BE%D0%B2%D1%8B%D0%B9%20%D1%84%D0%BE%D1%80%D0%BC%D0%B0%D1%82!A3:N800?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI";
	private final static String DEVELOPERS_PHRASES_URL =
			"https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/%D0%9D%D0%B5%D0%B2%D0%BE%D1%88%D0%B5%D0%B4%D1%88%D0%B5%D0%B5!A3:A800?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI";
	private boolean autoPhrasesSync;

	private CharacterPreset currentPreset;
	private EmotionsController emotionsController;

	boolean mouseReaction = true;
	private boolean applyInfluence = true;
	private int messageTimeout;
	private Quotes quotes;
	private PriorityQueue<Quote> quoteQueue;
	private PerkContainer perkContainer;
	private LinkedList<String> recievers;
	private final ResponseListener chatTimerListener = new ResponseListener() {
		
		private Object lastSeq = null;
		
		@Override
		public void handle(String sender, Object data) {
			Main.this.phraseRequest("CHAT");
			lastSeq = null;
			start();
		}
		
		void start() {
			if (lastSeq != null)
				stop();
			lastSeq = pluginProxy.sendMessage("core-utils:notify-after-delay", new HashMap<String, Object>() {{
						put("delay", (long) messageTimeout);
			}}, this);
		}
		
		void stop() {
			if (lastSeq != null)
				pluginProxy.sendMessage("core-utils:notify-after-delay", new HashMap<String, Object>() {{
							put("seq", lastSeq);
							put("delay", (long) -1);
				}});
		}
	};
	
	@Override
	public boolean initialize(PluginProxyInterface newPluginProxy) {
		pluginProxy = newPluginProxy;
		recievers=new LinkedList<>();
		recievers.add("gui:say");
		Properties properties = new Properties();
		emotionsController = new EmotionsController();
		quoteQueue = new PriorityQueue<>();
		perkContainer = new PerkContainer();
		quotes = new Quotes();
		currentPreset = new SimpleCharacterPreset();
		Influence.globalMultiplier = 0.05f;
		messageTimeout = 40000;
		autoPhrasesSync=true;
		pluginProxy.setResourceBundle("info/deskchan/talking_system/talk-strings");

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
			} catch (Exception e) { }
			try {
				mouseReaction = properties.getProperty("mouseReaction").equals("1");
			} catch (Exception e) { }
			try {
				currentPreset = CharacterPreset.getFromJSON(new JSONObject(properties.getProperty("characterPreset")));
			} catch (Exception e) { }
			try {
				Influence.globalMultiplier = Float.valueOf(properties.getProperty("influenceMultiplier"));
			} catch (Exception e) { }
			try {
				messageTimeout = Integer.valueOf(properties.getProperty("messageTimeout"));
			} catch (Exception e) { }
			try {
				autoPhrasesSync = properties.getProperty("autoPhrasesSync").equals("1");
			} catch (Exception e) { }
		}
		log("Loaded options");
		try {
			chatTimerListener.getClass().getDeclaredMethod("start").invoke(chatTimerListener);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			log(e);
		}
		pluginProxy.addMessageListener("talk:request", (sender, tag, data) -> {
			phraseRequest((Map<String, Object>) data);
		});
		pluginProxy.addMessageListener("talk:make-influence", (sender, tag, data) -> {
			Map<String, Object> dat = (Map<String, Object>) data;
			float multiplier = 1;
			Object obj = dat.getOrDefault("multiplier", 1);
			if (obj instanceof String)
				multiplier = Float.valueOf((String) obj);
			if (obj instanceof Float)
				multiplier = (Float) obj;
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
		pluginProxy.addMessageListener("talk:add-reciever",
				(sender, tag, data) -> {
					Map<String, Object> map = (Map<String, Object>) data;
					String rec=(String)map.getOrDefault("tag",null);
					if(rec==null || rec.length()<2) return;
					if(!recievers.contains(rec)) recievers.add(rec);
				}
		);
		pluginProxy.addMessageListener("talk:supply-resource",
				(sender, tag, data) -> {
					try {
						Map<String, Object> map = (Map<String, Object>) data;
						if (map.containsKey("preset")) {
							currentPreset = CharacterPreset.getFromFileUnsafe(Paths.get((String) map.get("preset")));
						}
						String type = (String) map.getOrDefault("quotes", null);
						if (type != null && type.length() > 0) {
							if (type.equals("#default")) {
								currentPreset.quotesBaseList = new ArrayList<>();
								currentPreset.quotesBaseList.add("main");
							} else {
								if(!type.startsWith(pluginProxy.getDataDirPath().toString())){
									Path resFile=Paths.get(type);
									Path newPath=pluginProxy.getDataDirPath().resolve(resFile.getFileName());
									if(!newPath.toFile().exists())
										Files.copy(resFile,newPath);
									type=newPath.toString();
								}
								currentPreset.quotesBaseList.add(type);
								quotes.load(currentPreset.quotesBaseList);
							}
						}
						updateOptionsTab();
					} catch(Exception e){
						log(e);
					}
				}
		);
		pluginProxy.sendMessage("DeskChan:register-simple-action", new HashMap<String, Object>() {{
			put("name", getString("say_phrase"));
			put("msgTag", "talk:request");
		}});
		updateOptionsTab();
		Main.getPluginProxy().addMessageListener("talk:reject-quote",(sender, tag, dat) -> 	DefaultTagsListeners.parseForTagsReject(sender,tag,dat));
		Main.getPluginProxy().addMessageListener("talk:remove-quote",(sender, tag, dat) -> 	DefaultTagsListeners.parseForTagsRemove(sender,tag,dat));
		Main.getPluginProxy().addMessageListener("talk:remove-quote",(sender, tag, dat) -> 	{
			HashMap<String, Object> data = (HashMap<String, Object>) dat;
			ArrayList<HashMap<String, Object>> list = (ArrayList<HashMap<String, Object>>) data.getOrDefault("quotes", null);
			HashMap<String, Object> ret = new HashMap<>();
			ret.put("seq", data.get("seq"));
			ArrayList<HashMap<String, Object>> quotes_list = new ArrayList<>();
			ret.put("quotes",quotes_list);
			if (list == null) {
				Main.getPluginProxy().sendMessage(sender, ret);
				return;
			}
			for(HashMap<String,Object> entry : list) {
				if(!currentPreset.isTagsMatch(entry))
					quotes_list.add(entry);
				else if(!emotionsController.isTagsMatch(entry))
					quotes_list.add(entry);
			}
			Main.getPluginProxy().sendMessage(sender,ret);
		});
		EventsCommentary.initialize();
		//currentPreset=CharacterPreset.getFromFile(pluginProxy.getDataDirPath(),"preset1");
		if(autoPhrasesSync) {
			Quotes.saveTo(MAIN_PHRASES_URL, "main");
			Quotes.saveTo(DEVELOPERS_PHRASES_URL, "developers_base");
		}
		quotes.load(currentPreset.quotesBaseList);
		phraseRequest("HELLO");
		/*MeaningExtractor extractor=new MeaningExtractor();
		for(Quote quote : quotes.toArray()){
			extractor.teach(quote.quote,quote.purposeType);
		}
		extractor.print();*/
		return true;
	}

	private static final int defaultPriority=1100;

	public void phraseRequest(Map<String, Object> data) {
		String purpose = "CHAT";
		final int priority;
		Integer p=defaultPriority;
		if (data != null) {
			String np = (String) data.getOrDefault("purpose", null);
			if (np != null) purpose = np;
			if(data.containsKey("priority")){
				Object a=data.get("priority");
				if(a instanceof String) p=Integer.valueOf( (String) a );
				else if(a instanceof Integer) p=(Integer) a;
			}
		}
		priority=p;
		quotes.update(currentPreset.getCharacter(emotionsController));
		quotes.requestRandomQuote(purpose,new Quotes.GetQuoteCallback(){
			public void call(Quote quote){ sendPhrase(quote,priority); }
		});
	}

	public void phraseRequest(String purpose) {
		quotes.update(currentPreset.getCharacter(emotionsController));
		quotes.requestRandomQuote(purpose,new Quotes.GetQuoteCallback(){
			public void call(Quote quote){ sendPhrase(quote,defaultPriority); }
		});
	}
	void sendPhrase(Quote quote,int priority){
		HashMap<String,Object> ret=quote.toMap();
		if (ret.get("characterImage").equals("AUTO")) {
			String ci = emotionsController.getSpriteType();
			if (ci == null) {
				ci = currentPreset.getCharacter(emotionsController).getSpriteType();
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
		ret.put("priority", priority);
		for(String rec : recievers)
			pluginProxy.sendMessage(rec, ret);
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
				put("id", "quotes");
				put("type", "FilesManager");
				put("label", getString("quotes_list"));
				put("value", new ArrayList<String>(currentPreset.quotesBaseList));
				put("hint",getString("help.quotes_pack"));
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
				put("hint",getString("help.character_type"));
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "tags");
				put("type", "TextField");
				put("label", getString("tags"));
				put("value", currentPreset.tags.toString());
				put("hint",getString("help.tags"));
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
				ch.put("hint",getString("help."+CharacterSystem.getFeatureName(i)));
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
				put("id", "autoSync");
				put("type", "CheckBox");
				put("value", autoPhrasesSync);
				put("label", getString("packs_auto_sync"));
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
			try{
				currentPreset.quotesBaseList=(ArrayList<String>)(data.get("quotes"));
			} catch(Exception e){
				errorMessage += e.getMessage() + "\n";
			}
			try{
				currentPreset.setTags((String)data.getOrDefault("tags",null));
			} catch(Exception e){
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
		boolean oa=autoPhrasesSync;
		autoPhrasesSync = (Boolean) data.getOrDefault("autoSync", true);
		if(!oa && autoPhrasesSync){
			Quotes.saveTo(MAIN_PHRASES_URL, "main");
			Quotes.saveTo(DEVELOPERS_PHRASES_URL, "developers_base");
		}
		if (errorMessage.length() > 1) {
			HashMap<String, Object> list = new HashMap<String, Object>();
			list.put("name", "Ошибка");
			list.put("text", errorMessage);
			pluginProxy.sendMessage("gui:show-notification", list);
			//System.out.println(errorMessage);
		}
		try {
			chatTimerListener.getClass().getDeclaredMethod("start").invoke(chatTimerListener);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			log(e);
		}
		quotes.setPacks(currentPreset.quotesBaseList);
		updateOptionsTab();
		saveSettings();
	}

	void saveSettings() {
		Properties properties = new Properties();
		properties.setProperty("applyInfluence", applyInfluence ? "1" : "0");
		properties.setProperty("autoPhrasesSync", autoPhrasesSync ? "1" : "0");
		properties.setProperty("characterPreset", currentPreset.toJSON().toString());
		properties.setProperty("influenceMultiplier", String.valueOf(Influence.globalMultiplier));
		properties.setProperty("messageTimeout", String.valueOf(messageTimeout));
		properties.setProperty("mouseReaction", String.valueOf(mouseReaction));
		try {
			OutputStream ip = Files.newOutputStream(pluginProxy.getDataDirPath().resolve("config.properties"));
			properties.store(ip, "config fot talking system");
			ip.close();
		} catch (IOException e) {
			log(e);
		}
	}

	public static String getString(String text){
		return pluginProxy.getString(text);
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
		phraseRequest("BYE");
	}

	static PluginProxyInterface getPluginProxy() {
		return pluginProxy;
	}

}
