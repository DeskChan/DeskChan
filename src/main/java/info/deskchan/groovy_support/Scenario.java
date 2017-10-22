package info.deskchan.groovy_support;

import groovy.lang.Script;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public abstract class Scenario extends Script{

    private String answer;
    private final Object lock = new Object();

    protected void sendMessage(String tag) {
        ScenarioPlugin.pluginProxy.sendMessage(tag, null);
    }

    protected void sendMessage(String tag, Object data) {
        ScenarioPlugin.pluginProxy.sendMessage(tag, data);
    }

    protected String getString(String key){
        return ScenarioPlugin.pluginProxy.getString(key);
    }

    protected Path getDataDirPath() {
        return ScenarioPlugin.pluginProxy.getDataDirPath();
    }

    protected void log(String text) {
        ScenarioPlugin.pluginProxy.log(text);
    }

    protected void log(Throwable e) {
        ScenarioPlugin.pluginProxy.log(e);
    }

    protected void say(String text){
        ScenarioPlugin.pluginProxy.sendMessage("DeskChan:say", text);
    }
    protected void requestPhrase(String text){
        ScenarioPlugin.pluginProxy.sendMessage("talk:request", text);
    }

    protected void sprite(String text){
        ScenarioPlugin.pluginProxy.sendMessage("gui:set-image", text);
    }

    protected synchronized String receive(){
        ScenarioPlugin.pluginProxy.sendMessage("DeskChan:request-user-speech", null, (sender, data) -> {
            answer = ((Map) data).get("value").toString();
            synchronized (lock) {
                lock.notify();
            }
        });
        synchronized (lock) {
            try {
                lock.wait();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
        return answer;
    }

    protected synchronized void sleep(long delay){
        final Object currentThread = this;
        Map data = new HashMap();
        data.put("value", delay);
        ScenarioPlugin.pluginProxy.sendMessage("core-utils:notify-after-delay", data, (sender, d) -> {
            synchronized (lock) {
                lock.notify();
            }
        });
        synchronized (lock) {
            try {
                lock.wait();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    protected void quit(){ Thread.currentThread().interrupt(); }
}
