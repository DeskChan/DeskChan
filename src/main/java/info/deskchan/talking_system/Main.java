package info.deskchan.talking_system;

import info.deskchan.MessageData.Core.SetPersistent;
import info.deskchan.MessageData.DeskChan.RequestSay;
import info.deskchan.core.*;
import org.json.JSONObject;

import java.io.File;
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
	private final static Integer DEFAULT_CHATTING_TIMEOUT = 300000;

	/** Chat timer id given by plugin proxy. **/
	private int chatTimerId = 0, emotionTimerId = 0;

	private TagsMap pluginsTags = new TagsMap();

	@Override
	public boolean initialize(PluginProxyInterface newPluginProxy) {
		instance = this;
		pluginProxy = newPluginProxy;
		getProperties().load();
		getProperties().putIfHasNot("user-phrases", "user-phrases");
		getProperties().putIfHasNot("quotesAutoSync", true);
		getProperties().putIfHasNot("messageTimeout", DEFAULT_CHATTING_TIMEOUT);
		getProperties().putIfHasNot("sleepTimeStart", 22);
		getProperties().putIfHasNot("sleepTimeEnd", 6);

		pluginsTags.putAll(DefaultTagsListeners.getDefaultTags());

		pluginProxy.setResourceBundle("info/deskchan/talking_system/strings");
		pluginProxy.setConfigField("short-description", getString("plugin.short-description"));
		//pluginProxy.setConfigField("description", getString("plugin.description"));
		pluginProxy.setConfigField("name", pluginProxy.getString("plugin-name"));

		// initializing main components

        PhraseBlocks.initialize();
		try {
			currentCharacter = new CharacterPreset(new JSONObject(getProperties().getString("characterPreset")));
		} catch (Exception e){
			currentCharacter = getDefaultCharacterPreset();
		}

		getProperties().putIfNotNull("quotesAutoSync", true);

		Thread syncThread = new Thread() {
			public void run() {
				// synchronize phrases with server
				if(getProperties().getBoolean("quotesAutoSync", true)) {
					if (PhrasesPackList.saveTo(MAIN_PHRASES_URL, "main")) {
						//PhrasesList.saveTo(DEVELOPERS_PHRASES_URL, "developers_base");
						PhrasesPackList.saveDatabaseTo(MAIN_DATABASE_URL, "database");
						currentCharacter.phrases.reload();
						currentCharacter.inform();
					}
				}

				DialogHandler.initialize();

				if (pluginProxy.getProperties().getBoolean("calc-cc-model", false)){
					//pluginProxy.getProperties().put("calc-cc-model", false);
					//pluginProxy.getProperties().save();
					List<Phrase> phrases = Main.getCurrentCharacter().phrases.toPhrasesList();
					new NeuralCharacterClassifier(phrases, getCharacterClassifierModelPath());
				}
			}
		};
		syncThread.start();

		/* Building DeskChan:request-say chain. */
		pluginProxy.setAlternative("DeskChan:request-say", "talk:request-say", 10000);
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
			currentCharacter = getDefaultCharacterPreset();
			updateOptionsTab();
			saveSettings();
		});

		/* Get preset
        * Public message
        * Returns: Map */
		pluginProxy.addMessageListener("talk:get-preset", (sender, tag, data) -> {
			pluginProxy.sendMessage(sender, currentCharacter.toInformationMap());
		});

		pluginProxy.sendMessage(new SetPersistent("talk:character-updated"));

		/* Save preset to file by character name
        * Technical message
        * Params: None
        * Returns: None */
		pluginProxy.addMessageListener("talk:save-preset", (sender, tag, data) -> {
			try {
				currentCharacter.saveInFile(getPresetsPath());
				pluginProxy.sendMessage(new RequestSay("DONE"));
			} catch (Exception e) {
				Main.log(e);
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
			PhrasesPackList.saveTo((String) data.getOrDefault("url", MAIN_PHRASES_URL), (String) data.getOrDefault("filename", "new_phrases"));
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
						MessageDataMap map = new MessageDataMap(data);

						if (map.containsKey("preset")) {
							currentCharacter = CharacterPreset.getFromFileUnsafe(new File((String) map.get("preset")));
						}

						String type = map.getString("phrases");
						if (type != null && type.length() > 0) {
							switch (type){
								case "#default": {
									currentCharacter.phrases = getDefaultPhrases();
								} break;
								case "#clear": {
									currentCharacter.phrases.clear();
								} break;
								default: {
									currentCharacter.phrases.add(type, PhrasesPack.PackType.USER);
								} break;
							}
						}
					} catch(Exception e){
						log(e);
					}
					updateOptionsTab();
					currentCharacter.inform();
				}
		);

		/* Set tag to filter phrases
		* Public message
		* Params: Map<String, Object>
		*    tags to add. Any non-string value will be manually converted to string. Set "*" as value to delete tag.
		* Returns: None */
		pluginProxy.addMessageListener("talk:set-preset-tags", (sender, tag, data) -> {
			setPresetTags(new MessageDataMap(data));
		});

		/* Saying 'Hello' at launch. */
		pluginProxy.addMessageListener("core-events:loading-complete", (sender, tag, dat) -> {
			phraseRequest(new HashMap<String, Object>(){{
				put("intent", "HELLO");
				put("priority", 20001);
			}});
		});

		/* Saying 'Bye' when someone requests quit. */
		pluginProxy.addMessageListener("core:quit", (sender, tag, data) -> {
			phraseRequest(new HashMap<String, Object>(){{
				put("intent", "BYE");
				put("priority", 20001);
			}});
		});

		/* Adding request to say command */
		pluginProxy.sendMessage("core:add-command", new HashMap<String, Object>() {{
			put("tag", "DeskChan:request-say");
			put("info", getString("request-say-info"));
			put("msgInfo", getString("request-say-data-info"));
		}});

		/* Adding request to say chat phrase */
		pluginProxy.sendMessage("core:add-command", new HashMap<String, Object>() {{
			put("tag", "talk:chat");
			put("info", getString("chat-info"));
		}});

		/* Adding "Say something" button to menu. */
		pluginProxy.sendMessage("core:set-event-link", new HashMap<String, Object>() {{
			put("eventName", "gui:menu-action");
			put("commandName", "talk:chat");
			put("rule", getString("say_phrase"));
		}});

		/* Saving last conversation timestamp every time user writes us something. */
		pluginProxy.addMessageListener("DeskChan:user-said", (sender, tag, data) -> {
			getProperties().put("lastConversation", Instant.now().toEpochMilli());
		});

		/* Saving last conversation timestamp every time user writes us something. */
		pluginProxy.addMessageListener("talk:chat", (sender, tag, data) -> {
			callChatPhrase();
		});

		updateOptionsTab();

		pluginProxy.addMessageListener("core-events:error", (sender, tag, d) -> {
			Phrase errorMessage = PhraseChooser.get(
					new IntentList("ERROR"),
					getCurrentCharacter(),
					pluginsTags,
					null
			);
			Map data = errorMessage.toPreparedPhrase();
			data.put("priority", 5000);
			pluginProxy.sendMessage("DeskChan:say", data);
		});

		pluginProxy.addMessageListener("recognition:get-words", (sender, tag, data) -> {
			Set<String> set = new HashSet<>();
			set.add(currentCharacter.name);
			pluginProxy.sendMessage(sender, set);
		});

		resetTimer();

		return true;
	}

	public void setPresetTags(MessageDataMap tags){
		for (String key : tags.keySet()){
			if (tags.getBoolean(key) != null){
				pluginsTags.put((tags.getBoolean(key) ? "!" : "") + key);
			} else if (tags.getString(key).equals("*")){
				pluginsTags.remove(key);
			} else {
				pluginsTags.put(key, tags.getString(key));
			}
		}
	}

	/** Default messages priority. **/
	private static final int DEFAULT_PRIORITY = 1100;

	/** Request phrase with any data. **/
	public static void phraseRequest(Map<String, Object> data) {
		IntentList intents = null;
		if (data != null && data.containsKey("intent")){
			if (data.get("intent") instanceof Collection) {
				intents = new IntentList((Collection) data.get("intent"));
				data.remove("intent");
			} else if (data.get("intent") instanceof String){
				intents = new IntentList((String) data.get("intent"));
				data.remove("intent");
			}
		}
		TagsMap tagsMap = null;
		try {
			tagsMap = new TagsMap((Map) data.get("tags"));
			data.remove("tags");
		} catch (Exception e){ }

		Phrase phrase = PhraseChooser.get(intents, getCurrentCharacter(), instance.pluginsTags, tagsMap);
		sendPhrase(phrase, data);
	}

	/** Request phrase with intent. **/
	public static void phraseRequest(String intent) {
		Phrase phrase = PhraseChooser.get(new IntentList(intent), getCurrentCharacter(), instance.pluginsTags, null);
		sendPhrase(phrase, null);
	}

	/** Convert phrase to map format and send to DeskChan:say or sender. **/
	static void sendPhrase(Phrase phrase, Map<String, Object> data){
		Map<String, Object> ret = phrase != null ? phrase.toPreparedPhrase() : new HashMap<>();

		if (ret.getOrDefault("characterImage", "AUTO").equals("AUTO")) {
			String characterImage = getCurrentCharacter().getDefaultSpriteType();

			if (characterImage == null)
				characterImage = "NORMAL";

			ret.replace("characterImage", characterImage);
		}
		ret.put("priority", DEFAULT_PRIORITY);
		if (data != null)
			ret.putAll(data);

		pluginProxy.callNextAlternative (
				(data != null && data.get("sender") != null) ? data.get("sender").toString() : Main.getPluginProxy().getId(),
				"DeskChan:request-say",
				"talk:request-say",
				ret
		);
	}



	void callChatPhrase(){
		Phrase phrase = PhraseChooser.get(null, getCurrentCharacter(), instance.pluginsTags, null);
		sendPhrase(phrase, null);
	}

	void resetTimer(){
		pluginProxy.cancelTimer(chatTimerId);
		chatTimerId = pluginProxy.setTimer(getProperties().getLong("messageTimeout", DEFAULT_CHATTING_TIMEOUT), -1,
				(sender, data) -> {
					callChatPhrase();
				}
		);
		pluginProxy.cancelTimer(emotionTimerId);
		emotionTimerId = pluginProxy.setTimer(100000, -1, (sender, data) -> {
			if (new Random().nextFloat() > 0.8)
				getCurrentCharacter().emotionState.raiseRandomEmotion();
		});
	}

	void updateOptionsTab() {
		pluginProxy.sendMessage("gui:set-panel", new HashMap<String, Object>() {{
			put("name", getString("character"));
			put("id", "character");
			put("msgTag", "talk:save-options");
			put("action", "set");
			List<Map<String, Object>> list = new LinkedList<>();

			list.add(new HashMap<String, Object>() {{
				put("type", "Button");
				put("value", getString("open-talk-debug-panel"));
				put("dstPanel", "talking_system-dialog-debug");
			}});

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
				put("value", currentCharacter.phrases.toPacksList(PhrasesPack.PackType.INTENT_DATABASE, PhrasesPack.PackType.USER));
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
				currentCharacter.setTags((String) data.getOrDefault("tags", null));
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

		try {
			getProperties().put("messageTimeout", (Integer) data.getOrDefault("message_interval", 40) * 1000);
		} catch (Exception e) {
			errorMessage += e.getMessage();
		}
		getProperties().put("quotesAutoSync", data.getOrDefault("autoSync", true));
		if (errorMessage.length() > 1) {
			Map<String, Object> list = new HashMap<String, Object>();
			list.put("name", getString("error"));
			list.put("text", errorMessage);
			pluginProxy.sendMessage("gui:show-notification", list);
			//System.out.println(errorMessage);
		}

		String fromUrl = (String) data.get("fromUrl");
		if (fromUrl != null && fromUrl.length() > 0)
			PhrasesPackList.saveTo(fromUrl, (String) data.getOrDefault("filename", "new_phrases"));

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

	public static CharacterPreset getDefaultCharacterPreset(){
		CharacterPreset preset = new CharacterPreset();

		preset.name = Main.getString("default_name");

		preset.phrases = getDefaultPhrases();

		preset.tags.putFromText("gender: girl, userGender: boy, breastSize: small, species: ai, interests: anime, abuses: бака дурак извращенец");
		preset.onChange = new Runnable() {
			@Override public void run() {
				Main.getPluginProxy().sendMessage("talk:character-updated", getCurrentCharacter().toInformationMap());
			}
		};
		return preset;
	}

	public static PhrasesPackList getDefaultPhrases(){
		PhrasesPackList pack = new PhrasesPackList();
		pack.add("main", PhrasesPack.PackType.USER);
		pack.add("database", PhrasesPack.PackType.INTENT_DATABASE);
		pack.add(Main.getProperties().getString("user-phrases"), PhrasesPack.PackType.USER);
		return pack;
	}

	public static String getString(String text){
		return pluginProxy.getString(text);
	}

	public static void log(String text) {
		pluginProxy.log(text);
	}

	public static void log(Throwable e) {
		pluginProxy.log(e);
	}

	public static Path getPresetsPath() {
		return pluginProxy.getAssetsDirPath().resolve("presets/");
	}

	@Override
	public void unload() {
		saveSettings();
	}

	static Main getInstance(){ return instance; }

	static PluginProxyInterface getPluginProxy() {
		return pluginProxy;
	}

	static Path getPhrasesDirPath(){ return getPluginProxy().getAssetsDirPath().resolve("phrases"); }

	static Path getDialogLogsDirPath(){ return getPluginProxy().getAssetsDirPath().resolve("dialog_logs"); }

	static Path getCharacterClassifierModelPath(){
		return getPluginProxy().getAssetsDirPath().resolve("models").resolve("character_classifier.model");
	}

	static CharacterPreset getCurrentCharacter(){
		return instance.currentCharacter;
	}

	static PluginProperties getProperties() { return getPluginProxy().getProperties(); }
}
