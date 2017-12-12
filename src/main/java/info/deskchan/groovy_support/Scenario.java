package info.deskchan.groovy_support;

import groovy.lang.Closure;
import groovy.lang.Script;
import info.deskchan.speech_command_system.RegularRule;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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
        ScenarioPlugin.pluginProxy.sendMessage("DeskChan:request-say", text);
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

    private boolean whenCycle;
    void when(Object obj, Closure cl) {
        while(true){
            whenCycle = false;
            CaseCollector caseCollector = new CaseCollector();
            cl.setDelegate(caseCollector);
            cl.call();
            Function result = caseCollector.execute(obj);
            result.apply(obj);
            if(!whenCycle) return;
            obj = receive();
        }
    }

    void again() {
        whenCycle = true;
    }

    static class CaseCollector{

        private Map<Object, Function> matches = new HashMap<>();

        void is(String obj, Function action) {
            try {
                matches.put(RegularRule.create(obj), action);
            } catch (Exception e){
                ScenarioPlugin.pluginProxy.log(e);
                matches.put(false, action);
            }
        }

        void is(Number obj, Function action) {
            matches.put(obj, action);
        }

        void otherwise(Function action) {
            matches.put(false, action);
        }

        Function execute(Object key) {
            Function action = matches.get(false);

            if(!(key instanceof String)) return action;

            RegularRule.MatchResult maxResult = null;
            for(Map.Entry<Object, Function> entry : matches.entrySet()){
                if(entry.getKey() instanceof RegularRule){
                    RegularRule.MatchResult result = ((RegularRule) entry.getKey()).parse((String) key);
                    if(result.better(maxResult)){
                        maxResult = result;
                        action = entry.getValue();
                    }
                }
            }
            return action;
        }

    }
    protected void quit(){ Thread.currentThread().interrupt(); }
}
