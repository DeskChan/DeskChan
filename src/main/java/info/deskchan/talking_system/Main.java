package info.deskchan.talking_system;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginProperties;
import info.deskchan.core.PluginProxyInterface;
import info.deskchan.core_utils.TextOperations;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

public class Main implements Plugin {
	private static PluginProxyInterface pluginProxy;

	/** Major phrases pack. **/
	private final static String MAIN_PHRASES_URL =
			"https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/phrases_ru!A20:L10000?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI";

	/** Not official pack from developers. **/
	private final static String DEVELOPERS_PHRASES_URL =
			"https://sheets.googleapis.com/v4/spreadsheets/17qf7fRewpocQ_TT4FoKWQ3p7gU7gj4nFLbs2mJtBe_k/values/%D0%9D%D0%B5%D0%B2%D0%BE%D1%88%D0%B5%D0%B4%D1%88%D0%B5%D0%B5!A3:A800?key=AIzaSyDExsxzBLRZgPt1mBKtPCcSDyGgsjM3_uI";


	/** Current character preset. **/
	private CharacterPreset currentCharacter;

	/** Default delay between chat phrases. **/
	private final static Integer DEFAULT_CHATTING_TIMEOUT = 120000;

	/** Chat timer id given by plugin proxy. **/
	private int timerId = 0;

	@Override
	public boolean initialize(PluginProxyInterface newPluginProxy) {
		pluginProxy = newPluginProxy;
		pluginProxy.getProperties().load();
		pluginProxy.setResourceBundle("info/deskchan/talking_system/strings");
		pluginProxy.setConfigField("short-description", getString("plugin.short-description"));
		pluginProxy.setConfigField("description", getString("plugin.description"));

		// initializing main components
		currentCharacter = new CharacterPreset(new JSONObject(getProperties().getString("characterPreset", "{}")));
		getProperties().putIfNotNull("quotesAutoSync", true);

		// synchronize phrases with server
		if(getProperties().getBoolean("quotesAutoSync", true)) {
			Thread syncThread = new Thread() {
				public void run() {
					if(PhrasesList.saveTo(MAIN_PHRASES_URL, "main")) {
						PhrasesList.saveTo(DEVELOPERS_PHRASES_URL, "developers_base");
						currentCharacter.phrases.reload();
					}
				}
			};
			syncThread.start();
		}

		currentCharacter.inform();

		/* Building DeskChan:request-say chain. */
		pluginProxy.sendMessage("core:register-alternatives", new ArrayList<Map>(){{
			add(createMapFromString("srcTag: \"DeskChan:request-say\", dstTag: \"talk:request-say\", priority: 10000"));
			add(createMapFromString("srcTag: \"DeskChan:request-say\", dstTag: \"talk:replace-by-preset-fields\", priority: 9990"));
			add(createMapFromString("srcTag: \"DeskChan:request-say\", dstTag: \"talk:send-phrase\", priority: 100"));
		}});

		/* Request a phrase with certain purpose. If answer is not requested, automatically send it to DeskChan:say.
        * Public message
        * Params: purpose: String?
        *      or
        *         Map
        *           purpose: String
        * Returns: Phrase if requested, else None */
		pluginProxy.addMessageListener("talk:request-say", (sender, tag, dat) -> {
			Map data = createMapFromObject(dat, "purpose");
			if (sender.contains("#"))
				data.put("sender", sender);

			phraseRequest(data);
		});

		/* Replacing fields in brackets to values set in preset
        * Technical message */
		pluginProxy.addMessageListener("talk:replace-by-preset-fields", (sender, tag, dat) -> {
			Map data = (Map) dat;

			data.replace("text", currentCharacter.replaceTags((String) data.get("text")));

			pluginProxy.sendMessage("DeskChan:request-say#talk:replace-by-preset-fields", data);
		});

		/* End of alternatives chain, sending phrase.
        * Technical message */
		pluginProxy.addMessageListener("talk:send-phrase", (sender, tag, dat) -> {
			Map data = (Map) dat;
			String dst = (String) data.remove("dstTag");
			if (dst == null)
				dst = "DeskChan:say";

			pluginProxy.sendMessage(dst, data);
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

			currentCharacter.character.setValue(feature, multiplier);
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
        *           purpose: String? - purpose
        * Returns: None */
		pluginProxy.addMessageListener("talk:print-phrases-lack", (sender, tag, data) -> {
			currentCharacter.phrases.printPhrasesLack( (String) ((Map) data).getOrDefault("purpose", "CHAT") );
		});

		/* Supply resources.
        * Technical message
        * Params: Map
        *           preset: String? - preset file
        *           phrases: String? - phrases file
        * Returns: None */
		pluginProxy.addMessageListener("talk:supply-resource",
				(sender, tag, data) -> {
					try {
						Map<String, Object> map = (Map) data;
						if (map.containsKey("preset")) {
							currentCharacter = CharacterPreset.getFromFileUnsafe(Paths.get((String) map.get("preset")));
						}
						String type = (String) map.getOrDefault("phrases", null);
						if (type != null && type.length() > 0) {
							if (type.equals("#default")) {
								currentCharacter.phrases = PhrasesList.getDefault(currentCharacter.character);
							} else {
								if(!type.startsWith(pluginProxy.getDataDirPath().toString())){
									Path resFile=Paths.get(type);
									Path newPath=pluginProxy.getDataDirPath().resolve(resFile.getFileName());
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
			pluginProxy.sendMessage("DeskChan:request-say", createMapFromString("purpose: HELLO, priority: 20001"));
		});

		/* Saying 'Bye' when someone requests quit. */
		pluginProxy.addMessageListener("core:quit", (sender, tag, data) -> {
			pluginProxy.sendMessage("DeskChan:request-say", createMapFromString("purpose: BYE, priority: 10000"));
		});

		/* Adding "Say something" button to menu. */
		pluginProxy.sendMessage("DeskChan:register-simple-action", new HashMap<String, Object>() {{
			put("name", getString("say_phrase"));
			put("msgTag", "DeskChan:request-say");
		}});

		/* Saving last conversation timestamp every time user writes us something. */
		pluginProxy.addMessageListener("DeskChan:user-said", (sender, tag, data) -> {
			getProperties().put("lastConversation", Instant.now().toEpochMilli());
		});

		updateOptionsTab();

		// Standard phrases parsers
		/// OS
		Main.getPluginProxy().addMessageListener("talk:remove-quote", DefaultTagsListeners::parseForTagsRemove);
		/// Time
		Main.getPluginProxy().addMessageListener("talk:reject-quote", DefaultTagsListeners::parseForTagsReject);

		/// Character preset
		Main.getPluginProxy().addMessageListener("talk:remove-quote", (sender, tag, data) ->
			DefaultTagsListeners.checkCondition(sender, data, quote -> !currentCharacter.tagsMatch(quote))
		);

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
		String purpose = "CHAT";
		if (data != null)
			purpose = data.getOrDefault("purpose", purpose).toString();

		currentCharacter.phrases.requestRandomQuote( purpose, data, quote -> sendPhrase(quote, data) );
	}

	/** Request phrase with purpose. **/
	public void phraseRequest(String purpose) {
		currentCharacter.phrases.requestRandomQuote( purpose, quote -> sendPhrase(quote, null) );
	}

	/** Convert phrase to map format and send to DeskChan:say or sender. **/
	void sendPhrase(Phrase phrase, Map<String, Object> data){
		Map<String, Object> ret = phrase.toMap();

		if (ret.get("characterImage").equals("AUTO")) {
			String characterImage = currentCharacter.getDefaultSpriteType();

			if (characterImage == null)
				characterImage = "NORMAL";

			ret.replace("characterImage", characterImage);
		}
		ret.put("priority", DEFAULT_PRIORITY);
		ret.put("dstTag", "DeskChan:say");
		if(data != null)
			ret.putAll(data);

		pluginProxy.sendMessage("DeskChan:request-say#talk:request-say", ret);
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
				put("type", "FilesManager");
				put("label", getString("quotes_list"));
				put("value", currentCharacter.phrases.toList(PhrasesPack.PackType.USER));
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
		String val = (String) data.getOrDefault("file", "");
		String errorMessage = "";
		if (val.isEmpty()) {
			try {
				val = (String) data.getOrDefault("name", "");
				if (!val.isEmpty()) {
					currentCharacter.name = val;
				}
			} catch (Exception e) {
				errorMessage += e.getMessage() + "\n";
			}
			try{
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
				val = (String) data.getOrDefault("usernames", "");
				if (!val.isEmpty()) {
					currentCharacter.tags.put("usernames", val);
				}
			} catch (Exception e) {
				errorMessage += e.getMessage() + "\n";
			}
			try {
				for (int i = 0; i < CharacterFeatures.getFeatureCount(); i++) {
					Number k = (Number) data.getOrDefault(CharacterFeatures.getFeatureName(i), 0);
					currentCharacter.character.setValue(i, k.intValue());
				}
			} catch (Exception e) {
				errorMessage += e.getMessage() + "\n";
			}
		} else {
			currentCharacter = CharacterPreset.getFromFileUnsafe(Paths.get(val));
		}
		currentCharacter.updatePhrases();
		currentCharacter.inform();

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

		try {
			for (int i = 0; i < CharacterFeatures.getFeatureCount(); i++) {
				Number k = (Number) data.getOrDefault(CharacterFeatures.getFeatureName(i), 0);
				currentCharacter.character.setValue(i, k.intValue());
			}
		} catch (Exception e) {
			errorMessage += e.getMessage() + "\n";
		}

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

	@Override
	public void unload() {
		saveSettings();
	}

	static PluginProxyInterface getPluginProxy() {
		return pluginProxy;
	}

	static PluginProperties getProperties() { return getPluginProxy().getProperties(); }

	static Map createMapFromObject(Object object, String key){
		Map<String, Object> data;
		if(object instanceof Map){
			data = (Map) object;
		} else {
			data = new HashMap<>();
			if(object != null)
				data.put("purpose", object.toString());
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
