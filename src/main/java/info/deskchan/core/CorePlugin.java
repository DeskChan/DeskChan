package info.deskchan.core;

import info.deskchan.MessageData.Core.SetPersistent;

import java.util.*;


// This plugin contains
//   - Alternatives infrastructure
//   - Fallbacks for some major DeskChan functions
//   - Commands interface initialization
public class CorePlugin implements Plugin {
	
	protected PluginProxyInterface pluginProxy = null;

	@Override
	public boolean initialize(PluginProxyInterface pluginProxy) {
		this.pluginProxy = pluginProxy;

		pluginProxy.getProperties().load();
		pluginProxy.getProperties().putIfHasNot("quitDelay", 2000);
		pluginProxy.getProperties().putIfHasNot("locale", Locale.getDefault().getLanguage());
		Locale.setDefault(new Locale(pluginProxy.getProperties().getString("locale")));

		pluginProxy.setResourceBundle("info/deskchan/strings");
        pluginProxy.setConfigField("name", pluginProxy.getString("core-plugin-name"));
		try {
			PluginProxy.Companion.updateResourceBundle();
		} catch (Exception e){
			pluginProxy.log(e);
		}

		CommandsProxy.initialize(pluginProxy);

		pluginProxy.addMessageListener("core:set-language", (sender, tag, data) -> {
			final String newValue;
			if (data instanceof Map)
				newValue = ((Map) data).get("value").toString();
			else
				newValue = data.toString();

			for(Map.Entry<String,String> locale : CoreInfo.locales.entrySet()){
				if(locale.getKey().equals(newValue) || locale.getValue().equals(newValue)){
					Locale.setDefault(new Locale(locale.getKey()));
					pluginProxy.getProperties().put("locale", locale.getKey());
					return;
				}
			}
			pluginProxy.log(new Exception("Unknown language key: " + newValue));
		});

		pluginProxy.addMessageListener("core:get-language", (sender, tag, data) -> {
			pluginProxy.sendMessage(sender, pluginProxy.getProperties().getString("locale"));
		});

		pluginProxy.addMessageListener("core:quit", (sender, tag, data) -> {
			int delay = pluginProxy.getProperties().getInteger("quitDelay", 2000);
			if (data != null) {
				if (data instanceof Map) {
					delay = (int) ((Map<String, Object>) data).getOrDefault("delay", delay);
				} else if (data instanceof Number) {
					delay = ((Number) data).intValue();
				}
			} else {
				delay = PluginManager.isDebugBuild() ? 0 : delay;
			}

			Map<String, Object> m = new HashMap<>();
			m.put("delay", delay);
			pluginProxy.log("Plugin " + sender + " requested application quit in " + delay / 1000 + " seconds.");
			pluginProxy.sendMessage("core:save-all-properties", null);
			if(delay > 20) {
				Timer quitTimer = new Timer();
				quitTimer.schedule(new TimerTask() {
					@Override public void run() {
						PluginManager.getInstance().quit();
					}
				}, delay);
			} else {
				PluginManager.getInstance().quit();
			}

		});

		/* DEPRECATED, use PluginProxyInterface.setAlternative instead */
		pluginProxy.addMessageListener("core:register-alternative", (sender, tag, data) -> {
			System.out.println("Message \"core:register-alternative\" is deprecated!");
			Map m = (Map) data;
			Alternatives.registerAlternative(m.get("srcTag").toString(), m.get("dstTag").toString(),
					sender, m.get("priority"));
		});

		/* DEPRECATED, use PluginProxyInterface.setAlternative instead */
		pluginProxy.addMessageListener("core:register-alternatives", (sender, tag, data) -> {
			System.out.println("Message \"core:register-alternatives\" is deprecated!");
			List<Map> alternativeList = (List<Map>) data;
			for (Map m : alternativeList) {
				Alternatives.registerAlternative(m.get("srcTag").toString(), m.get("dstTag").toString(),
						sender, m.get("priority"));
			}
		});

		/* DEPRECATED, use PluginProxyInterface.deleteAlternative instead */
		pluginProxy.addMessageListener("core:unregister-alternative", (sender, tag, data) -> {
			System.out.println("Message \"core:unregister-alternative\" is deprecated!");
			Map m = (Map) data;
			Alternatives.unregisterAlternative(m.get("srcTag").toString(), m.get("dstTag").toString(), sender);
		});

		pluginProxy.addMessageListener("core:query-alternatives-map", (sender, tag, data) -> {
			pluginProxy.sendMessage(sender, Alternatives.getAlternativesMap());
		});

		pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
			if(data == null) {
				PluginManager.log("attempt to unload null plugin");
				return;
			}
			String plugin = data.toString();
			Alternatives.unregisterAlternativesByPlugin(plugin);

		});

		pluginProxy.sendMessage("core:add-command", new HashMap<String, Object>(){{
			put("tag", "DeskChan:say");
			put("info", pluginProxy.getString("plugin-unload-info"));
			put("msgInfo", new HashMap<String, String>(){{
				put("text", pluginProxy.getString("text"));
				put("characterImage", pluginProxy.getString("sprite"));
				put("priority", pluginProxy.getString("priority"));
			}});
		}});

		pluginProxy.sendMessage("core:add-command", new HashMap(){{
			put("tag", "DeskChan:commands-list");
			put("info", pluginProxy.getString("commands-list-info"));
		}});

		pluginProxy.addMessageListener("core-events:log", (sender, tag, data) -> {
			Map mapLog = (Map) data;
			LoggerLevel level = (LoggerLevel) mapLog.getOrDefault("level", LoggerLevel.INFO);
			PluginManager.log(sender, (String) mapLog.get("message"),level);
		});

		pluginProxy.addMessageListener("core-events:error", (sender, tag, data) -> {
			Map error = (Map) data;
			String message = (error.get("class") != null ? error.get("class") : "") +
					         (error.get("message") != null ? ": " + error.get("message") : "");
			PluginManager.log(sender, message , (List) error.get("stacktrace"));
		});

		pluginProxy.addMessageListener("core:save-all-properties", (sender, tag, data) -> {
			PluginManager.getInstance().saveProperties();
		});

		pluginProxy.setAlternative("DeskChan:voice-recognition", "DeskChan:user-said", 50);
		pluginProxy.setAlternative( "DeskChan:user-said", "core:inform-no-speech-function", 1);
		pluginProxy.setAlternative( "DeskChan:notify", "core:notify", 1);

		pluginProxy.addMessageListener("core:inform-no-speech-function", (sender, tag, data) -> {
			pluginProxy.sendMessage("DeskChan:say", pluginProxy.getString("no-conversation"));
		});

		pluginProxy.addMessageListener("core:set-persistent", (sender, tag, data) -> {
			PluginManager.getInstance().setPersistentMessage(data.toString());
		});

		PluginManager.getInstance().setPersistentMessage("core-events:plugin-load");

		pluginProxy.addMessageListener("core:notify", (sender, tag, data) -> {
			Map ntf = (Map) data;
			if (ntf.containsKey("message"))
				pluginProxy.sendMessage("DeskChan:show-technical", new HashMap(){{
					put("text", ntf.get("message"));
				}});
			if (ntf.containsKey("speech"))
				pluginProxy.sendMessage("DeskChan:say", new HashMap(){{
					put("text", ntf.get("speech"));
					if (ntf.containsKey("priority"))
						put("priority", ntf.get("priority"));
				}});
			else
				pluginProxy.sendMessage("DeskChan:request-say", new HashMap(){{
					put("intent", ntf.getOrDefault("speech-intent", "NOTIFY"));
					if (ntf.containsKey("priority"))
						put("priority", ntf.get("priority"));
				}});

		});

		return true;
	}

	@Override
	public void unload(){
		pluginProxy.getProperties().save();
	}

}
