package info.deskchan.talking_system;

import info.deskchan.core.*;
import info.deskchan.core_utils.TextOperations;
import org.json.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

public class Main implements Plugin {
	private static PluginProxyInterface pluginProxy;
	private static Main instance;

	/** Major phrases pack. **/
	private final static Map<String, String> MAIN_PHRASES_URL = new HashMap<String, String>(){{
		put("ru", "https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/phrases_ru!A3:L10000?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI");
		put("en", "https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/phrases_en!A3:L10000?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI");
	}};

	/** Not official pack from developers. **/
	//private final static String DEVELOPERS_PHRASES_URL =
	//		"https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/%D0%9D%D0%B5%D0%B2%D0%BE%D1%88%D0%B5%D0%B4%D1%88%D0%B5%D0%B5!A3:A800?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI";

	/** Not official pack from developers. **/
	private final static Map<String, String> MAIN_DATABASE_URL = new HashMap<String, String>(){{
		put("ru", "https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/database_ru!A3:L10000?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI");
		put("en", "https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/database_en!A3:L10000?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI");
	}};

	/** Current character preset. **/
	private CharacterPreset currentCharacter;

	/** Default delay between chat phrases. **/
	private final static Integer DEFAULT_CHATTING_TIMEOUT = 120000;

	/** Chat timer id given by plugin proxy. **/
	private int timerId = 0;

	@Override
	public boolean initialize(PluginProxyInterface newPluginProxy) {
		instance = this;
		pluginProxy = newPluginProxy;
		pluginProxy.getProperties().load();
		pluginProxy.setResourceBundle("info/deskchan/talking_system/strings");
		pluginProxy.setConfigField("short-description", getString("plugin.short-description"));
		//pluginProxy.setConfigField("description", getString("plugin.description"));
		pluginProxy.setConfigField("name", pluginProxy.getString("plugin-name"));

		// initializing main components
		currentCharacter = new CharacterPreset(new JSONObject(getProperties().getString("characterPreset", "{}")));
		getProperties().putIfNotNull("quotesAutoSync", true);

		// synchronize phrases with server
		if(getProperties().getBoolean("quotesAutoSync", true)) {
			Thread syncThread = new Thread() {
				public void run() {
					if(PhrasesList.saveTo(MAIN_PHRASES_URL, "main")) {
						//PhrasesList.saveTo(DEVELOPERS_PHRASES_URL, "developers_base");
						PhrasesList.saveDatabaseTo(MAIN_DATABASE_URL, "database");
						currentCharacter.phrases.reload();
						currentCharacter.inform();
					}
				}
			};
			syncThread.start();
		}

		info.deskchan.talking_system.classification.Main.initialize(pluginProxy);

		/* Building DeskChan:request-say chain. */
		pluginProxy.setAlternative("DeskChan:request-say", "talk:request-say", 10000);
		pluginProxy.setAlternative("DeskChan:request-say", "talk:replace-by-preset-fields", 9990);
		pluginProxy.setAlternative("DeskChan:request-say", "talk:send-phrase", 100);

		/* Request a phrase with certain intent. If answer is not requested, automatically send it to DeskChan:say.
        * Public message
        * Params: intent: String?
        *      or
        *         Map
        *           intent: String
        * Returns: Phrase if requested, else None */
		pluginProxy.addMessageListener("talk:request-say", (sender, tag, dat) -> {
			MessageDataMap data = new MessageDataMap("intent", dat);
			data.put("sender", sender);
			if (data.containsKey("text")){
				sendPhrase(null, data);
			} else {
				if (data.containsKey("purpose") && !data.containsKey("intent"))
					data.put("intent", data.get("purpose"));

				phraseRequest(data);
			}
		});

		/* Replacing fields in brackets to values set in preset
        * Technical message */
		pluginProxy.addMessageListener("talk:replace-by-preset-fields", (sender, tag, dat) -> {
			Map data = (Map) dat;

			data.replace("text", currentCharacter.replaceTags((String) data.get("text")));


			pluginProxy.callNextAlternative(sender, "DeskChan:request-say", "talk:replace-by-preset-fields", data);
		});

		/* End of alternatives chain, sending phrase.
        * Technical message */
		pluginProxy.addMessageListener("talk:send-phrase", (sender, tag, data) -> {
			if (pluginProxy.isAskingAnswer(sender))
				pluginProxy.sendMessage(sender, data);
			else
				pluginProxy.sendMessage("DeskChan:say", data);
		});

		/* Make an influence on character features
        * Public message
        * Params: Map
        * 			feature: String? - name of feature
        *           value: Float? - influence strength
        * Returns: None */
		pluginProxy.addMessageListener("talk:make-character-influence", (sender, tag, dat) -> {
			Map<String, Object> data = (Map) dat;
			Object obj = data.get("feature");
			if (obj == null) return;

			int feature;
			float multiplier = 1;
			if (obj instanceof String)
				feature = CharacterFeatures.getFeatureIndex((String) obj);
			else if (obj instanceof Number)
				feature = ((Number) obj).intValue();
			else return;

			obj = data.getOrDefault("value", 1);
			if (obj instanceof String)
				multiplier = Float.valueOf((String) obj);
			else if (obj instanceof Number)
				multiplier = ((Number) obj).floatValue();

			currentCharacter.character.moveValue(feature, multiplier);
			currentCharacter.inform();
		});

		/* Make an influence on character emotion state
        * Public message
        * Params: Map
        *           emotion: String? - name of emotion
        *           value: Float? - influence strength
        * Returns: None */
		pluginProxy.addMessageListener("talk:make-emotion-influence", (sender, tag, dat) -> {
			Map<String, Object> data = (Map) dat;
			Object obj = data.get("feature");
			if (obj == null) return;

			String emotion;
			int multiplier = 1;
			emotion = (String) obj;

			obj = data.getOrDefault("value", 1);
			if (obj instanceof String)
				multiplier = Float.valueOf((String) obj).intValue();
			else if (obj instanceof Number)
				multiplier = ((Number) obj).intValue();

			currentCharacter.emotionState.raiseEmotion(emotion, multiplier);
		});

		/* Save options
        * Technical message
        * Returns: None */
		pluginProxy.addMessageListener("talk:save-options", (sender, tag, data) -> {
			saveOptions((Map) data);
		});

		/* Reset preset
        * Technical message
        * Returns: None */
		pluginProxy.addMessageListener("talk:reset-preset", (sender, tag, data) -> {
			currentCharacter = new CharacterPreset();
			updateOptionsTab();
			saveSettings();
		});

		/* Get preset
        * Public message
        * Returns: Map */
		pluginProxy.addMessageListener("talk:get-preset", (sender, tag, data) -> {
			pluginProxy.sendMessage(sender, currentCharacter.toInformationMap());
		});

		/* Save preset to file by character name
        * Technical message
        * Params: None
        * Returns: None */
		pluginProxy.addMessageListener("talk:save-preset", (sender, tag, data) -> {
			try {
				currentCharacter.saveInFile(getPresetsPath());
				pluginProxy.sendMessage("gui:show-notification", createMapFromString("text: "+getString("done")));
			} catch (Exception e) {
				Map<String, Object> list = new HashMap<>();
				list.put("name", getString("error"));
				list.put("text", "Error while writing file: " + e.getMessage());
				pluginProxy.sendMessage("gui:show-notification", list);
			}
		});

		/* Add plugin's phrases pack
        * Public message
        * Params: path: String! - path to pack
        *      or
        *         List<String>! - paths to packs
        * Returns: None */
		pluginProxy.addMessageListener("talk:add-plugin-phrases", (sender, tag, data) -> {
			if (data instanceof List)
				currentCharacter.phrases.add((List) data, PhrasesPack.PackType.PLUGIN);
			else
				currentCharacter.phrases.add(data.toString(), PhrasesPack.PackType.PLUGIN);
			currentCharacter.inform();
		});

		/* Download phrases from JSON at url and save them to file
        * Technical message
        * Params: Map
        *           url: String? - url to download
        *           filename: String? - filename
        * Returns: None */
		pluginProxy.addMessageListener("talk:create-phrases-base", (sender, tag, dat) -> {
			Map<String, Object> data = (Map) dat;
			PhrasesList.saveTo((String) data.getOrDefault("url", MAIN_PHRASES_URL), (String) data.getOrDefault("filename", "new_phrases"));
		});

		/* Print to screen character combinations with the lowest count of suitable phrases.
        * Technical message
        * Params: Map
        *           intent: String? - intent
        * Returns: None */
		pluginProxy.addMessageListener("talk:print-phrases-lack", (sender, tag, data) -> {
			currentCharacter.phrases.printPhrasesLack( (String) ((Map) data).getOrDefault("intent", "CHAT") );
		});

		/* Supply resources.
        * Technical message
        * Params: Map
        *           preset: String? - preset file
        *           phrases: String? - phrases file
        *           phrases #default - only "main" pack
        *           phrases #clear - clear pack selection
        * Returns: None */
		pluginProxy.addMessageListener("talk:supply-resource",
				(sender, tag, data) -> {
					try {
						Map<String, Object> map = (Map) data;
						if (map.containsKey("preset")) {
							currentCharacter = CharacterPreset.getFromFileUnsafe(new File((String) map.get("preset")));
						}
						String type = (String) map.getOrDefault("phrases", null);
						if (type != null && type.length() > 0) {
							if (type.equals("#default")) {
								currentCharacter.phrases = PhrasesList.getDefault(currentCharacter.character);
							} else if (type.equals("#clear")){
								currentCharacter.phrases.clear();
							} else {
								if(!type.startsWith(getPhrasesDirPath().toString())){
									Path resFile = Paths.get(type);
									Path newPath = getPhrasesDirPath().resolve(resFile.getFileName());
									if(!newPath.toFile().exists())
										Files.copy(resFile,newPath);
									type=newPath.toString();
								}
								currentCharacter.phrases.add(type, PhrasesPack.PackType.USER);
								currentCharacter.updatePhrases();
							}
						}

					} catch(Exception e){
						log(e);
					}
					updateOptionsTab();
					currentCharacter.inform();
				}
		);

		/* Reload phrases and check for matching
		* Public message
        * Params: None
        * Returns: None */
		pluginProxy.addMessageListener("talk:update-phrases", (sender, tag, data) -> {
			currentCharacter.updatePhrases();
		});

		/* Saying 'Hello' at launch. */
		pluginProxy.addMessageListener("core-events:loading-complete", (sender, tag, dat) -> {
			pluginProxy.sendMessage("DeskChan:request-say", createMapFromString("intent: HELLO, priority: 20001"));
		});

		/* Saying 'Bye' when someone requests quit. */
		pluginProxy.addMessageListener("core:quit", (sender, tag, data) -> {
			pluginProxy.sendMessage("DeskChan:request-say", createMapFromString("intent: BYE, priority: 10000"));
		});

		/* Adding request to say command */
		pluginProxy.sendMessage("core:add-command", new HashMap<String, Object>() {{
			put("tag", "DeskChan:request-say");
			put("info", getString("request-say-info"));
			put("msgInfo", getString("request-say-data-info"));
		}});

		/* Adding "Say something" button to menu. */
		pluginProxy.sendMessage("core:set-event-link", new HashMap<String, Object>() {{
			put("eventName", "gui:menu-action");
			put("commandName", "DeskChan:request-say");
			put("rule", getString("say_phrase"));
			put("msgData", "CHAT");
		}});

		/* Saving last conversation timestamp every time user writes us something. */
		pluginProxy.addMessageListener("DeskChan:user-said", (sender, tag, data) -> {
			getProperties().put("lastConversation", Instant.now().toEpochMilli());
		});

		updateOptionsTab();

		// Standard phrases parsers
		/// OS
		pluginProxy.addMessageListener("talk:remove-quote", DefaultTagsListeners::parseForTagsRemove);
		/// Time
		pluginProxy.addMessageListener("talk:reject-quote", DefaultTagsListeners::parseForTagsReject);

		/// Character preset
		pluginProxy.addMessageListener("talk:remove-quote", (sender, tag, data) ->
			DefaultTagsListeners.checkCondition(sender, data, quote -> !currentCharacter.tagsMatch(quote))
		);

		pluginProxy.addMessageListener("core-events:error", (sender, tag, data) -> {
			currentCharacter.phrases.requestRandomQuote("ERROR", null, (quote) -> {
				Map ret = quote.toMap();
				ret.put("priority", 5000);
				pluginProxy.sendMessage("DeskChan:say", ret);
			});
		});

		pluginProxy.addMessageListener("recognition:get-words", (sender, tag, data) -> {
			HashSet<String> set = new HashSet<>();
			set.add(currentCharacter.name);
			pluginProxy.sendMessage(sender, set);
		});

		resetTimer();

		return true;
	}

	/** Default messages priority. **/
	private static final int DEFAULT_PRIORITY = 1100;

	/** Request phrase with any data. **/
	public void phraseRequest(Map<String, Object> data) {
		String intent = "CHAT";
		if (data != null)
			intent = data.getOrDefault("intent", intent).toString();

		currentCharacter.phrases.requestRandomQuote( intent, data, quote -> sendPhrase(quote, data) );
	}

	/** Request phrase with intent. **/
	public void phraseRequest(String intent) {
		currentCharacter.phrases.requestRandomQuote( intent, quote -> sendPhrase(quote, null) );
	}

	/** Convert phrase to map format and send to DeskChan:say or sender. **/
	void sendPhrase(Phrase phrase, Map<String, Object> data){
		Map<String, Object> ret = phrase != null ? phrase.toMap() : new HashMap<>();

		if (ret.getOrDefault("characterImage", "AUTO").equals("AUTO")) {
			String characterImage = currentCharacter.getDefaultSpriteType();

			if (characterImage == null)
				characterImage = "NORMAL";

			ret.replace("characterImage", characterImage);
		}
		ret.put("priority", DEFAULT_PRIORITY);
		if(data != null)
			ret.putAll(data);
		;
		pluginProxy.callNextAlternative(data.get("sender").toString(), "DeskChan:request-say", "talk:request-say", ret);
	}

	void resetTimer(){
		pluginProxy.cancelTimer(timerId);
		timerId = pluginProxy.setTimer(getProperties().getLong("messageTimeout", DEFAULT_CHATTING_TIMEOUT), -1,
				(sender, data) -> {
					getPluginProxy().sendMessage("DeskChan:request-say", "CHAT");
				}
		);
	}

	void updateOptionsTab() {
		pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>() {{
			put("name", getString("character"));
			put("id", "character");
			put("msgTag", "talk:save-options");
			put("action", "set");
			List<Map<String, Object>> list = new LinkedList<>();
			list.add(new HashMap<String, Object>() {{
				put("id", "name");
				put("type", "TextField");
				put("label", getString("name"));
				put("value", currentCharacter.name);
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "usernames");
				put("type", "TextField");
				put("label", getString("usernames_list"));
				put("value", currentCharacter.tags.getAsString("usernames"));
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "phrases");
				put("type", "AssetsManager");
				put("multiple", true);
				put("folder", "phrases");
				put("acceptedExtensions", new ArrayList<String>(){{
					add(".phrases"); add(".database");
				}});
				put("label", getString("quotes_list"));
				put("value", currentCharacter.phrases.toList(PhrasesPack.PackType.DATABASE, PhrasesPack.PackType.USER));
				put("hint",getString("help.quotes_pack"));
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "tags");
				put("type", "TextField");
				put("label", getString("tags"));
				put("value", currentCharacter.tags.toString());
				put("hint",getString("help.tags"));
			}});
			list.add(new HashMap<String, Object>() {{
				put("type", "Separator");
				put("label", getString("primary_character_values"));
			}});
			for (int i = 0; i < CharacterFeatures.getFeatureCount(); i++) {
				HashMap<String, Object> ch = new HashMap<>();
				ch.put("id", CharacterFeatures.getFeatureName(i));
				ch.put("label", getString(CharacterFeatures.getFeatureName(i)));
				ch.put("value", currentCharacter.character.getValue(i));
				ch.put("type", "Spinner");
				ch.put("min", -CharacterFeatures.BORDER);
				ch.put("max", CharacterFeatures.BORDER);
				ch.put("step", 1);
				ch.put("hint", getString("help."+ CharacterFeatures.getFeatureName(i)));
				list.add(ch);
			}
			list.add(new HashMap<String, Object>() {{
				put("type", "Separator");
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "file");
				put("type", "FileField");
				put("label", getString("load_preset"));
				put("initialDirectory", getPresetsPath().toString());
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "message_interval");
				put("type", "Spinner");
				put("min", 0);
				put("max", 10000);
				put("step", 1);
				put("hint",getString("help.delay"));
				put("value", getProperties().getLong("messageTimeout", DEFAULT_CHATTING_TIMEOUT) / 1000);
				put("label", getString("message_interval"));
			}});
			list.add(new HashMap<String, Object>() {{
				put("id", "autoSync");
				put("type", "CheckBox");
				put("value", getProperties().getBoolean("quotesAutoSync", true));
				put("label", getString("packs_auto_sync"));
			}});
			Map buttons = new HashMap<String, Object>();
			buttons.put("elements", new ArrayList<Map>(){{
				add(new HashMap<String, Object>() {{
					put("type", "Button");
					put("value", getString("save_preset"));
					put("msgTag", "talk:save-preset");
				}});
				add(new HashMap<String, Object>() {{
					put("type", "Button");
					put("value", getString("reset"));
					put("msgTag", "talk:reset-preset");
				}});
			}});
			list.add(buttons);
			list.add(new HashMap<String, Object>() {{
				put("id", "fromUrl");
				put("type", "TextField");
				put("label", getString("pack_from_url"));
			}});
			put("controls", list);
		}});
	}

	void saveOptions(Map<String, Object> data) {
		MessageDataMap map = new MessageDataMap(data);
		File presetFile = map.getFile("file");
		String errorMessage = "";
		if (presetFile  == null) {
			String val;
			try {
				val = map.getString("name");
				if (val != null) {
					currentCharacter.name = val;
				}
			} catch (Exception e) {
				errorMessage += e.getMessage() + "\n";
			}
			try {
				currentCharacter.phrases.set((List<String>) data.get("phrases"));
			} catch(Exception e){
				errorMessage += e.getMessage() + "\n";
			}
			try{
				currentCharacter.setTags(data.getOrDefault("tags", null));
			} catch(Exception e){
				errorMessage += e.getMessage() + "\n";
			}
			try {
				val = map.getString("usernames");
				if (val != null) {
					currentCharacter.tags.put("usernames", val);
				}
			} catch (Exception e) {
				errorMessage += e.getMessage() + "\n";
			}
			try {
				for (int i = 0; i < CharacterFeatures.getFeatureCount(); i++) {
					int k = map.getInteger(CharacterFeatures.getFeatureName(i), 0);
					currentCharacter.character.setValue(i, k);
				}
			} catch (Exception e) {
				errorMessage += e.getMessage() + "\n";
			}
		} else {
			currentCharacter = CharacterPreset.getFromFileUnsafe(presetFile);
		}
		currentCharacter.updatePhrases();

		try {
			getProperties().put("messageTimeout", (Integer) data.getOrDefault("message_interval", 40) * 1000);
		} catch (Exception e) {
			errorMessage += e.getMessage();
		}
		getProperties().put("quotesAutoSync", data.getOrDefault("autoSync", true));
		if (errorMessage.length() > 1) {
			HashMap<String, Object> list = new HashMap<String, Object>();
			list.put("name", getString("error"));
			list.put("text", errorMessage);
			pluginProxy.sendMessage("gui:show-notification", list);
			//System.out.println(errorMessage);
		}

		String fromUrl = (String) data.get("fromUrl");
		if (fromUrl != null && fromUrl.length() > 0)
			PhrasesList.saveTo(fromUrl, (String) data.getOrDefault("filename", "new_phrases"));

		currentCharacter.inform();

		if (errorMessage.length() > 0)
			Main.log(new Exception(errorMessage));

		resetTimer();

		updateOptionsTab();
		saveSettings();
	}

	void saveSettings() {
		getProperties().put("characterPreset", currentCharacter.toJSON().toString());
		getProperties().save();
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

	public static Path getDataDirPath() {
		return pluginProxy.getDataDirPath();
	}

	public static Path getPresetsPath() {
		Path path = pluginProxy.getAssetsDirPath().resolve("presets/");
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

	public static CharacterPreset getCharacterPreset(){
		return instance.currentCharacter;
	}

	@Override
	public void unload() {
		saveSettings();
	}

	static PluginProxyInterface getPluginProxy() {
		return pluginProxy;
	}

	static Path getPhrasesDirPath(){ return getPluginProxy().getAssetsDirPath().resolve("phrases"); }

	static PluginProperties getProperties() { return getPluginProxy().getProperties(); }

	static Map createMapFromObject(Object object, String key){
		Map<String, Object> data;
		if(object instanceof Map){
			data = (Map) object;
		} else {
			data = new HashMap<>();
			if(object != null)
				data.put("intent", object.toString());
		}
		return data;
	}

	static Map createMapFromString(String text){
		TextOperations.TagsMap<String, Collection> map = new TextOperations.TagsMap<>(text);
		Map result = new HashMap<>();
		for (Map.Entry<String, Collection> entry : map.entrySet()){
			try {
				result.put(entry.getKey(), entry.getValue().iterator().next());
			} catch (Exception e){ }
		}
		return result;
	}
}
