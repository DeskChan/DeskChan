package info.deskchan.groovy_support;

import groovy.lang.Closure;
import groovy.lang.Script;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public abstract class Scenario extends Script{

    private Answer answer;

    private final Locker lock = new Locker();

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
            if (data instanceof Map)
                answer = new Answer((Map) data);
            else
                answer = new Answer(data.toString());

            lock.unlock();
        });
        lock.lock();
        return answer.text;
    }

    protected synchronized void sleep(long delay){
        Map data = new HashMap();
        data.put("value", delay);
        ScenarioPlugin.pluginProxy.sendMessage("core-utils:notify-after-delay", data, (sender, d) -> {
            lock.unlock();
        });
        lock.lock();
    }

    private boolean whenCycle;
    void when(Object obj, Closure cl) {
        while(true){
            whenCycle = false;
            CaseCollector caseCollector = new CaseCollector();
            cl.setDelegate(caseCollector);
            cl.call();
            Function result = caseCollector.execute(obj);
            if (result != null)
                result.apply(obj);
            if(!whenCycle) return;
            obj = receive();
        }
    }

    void again() {
        whenCycle = true;
    }

    class CaseCollector{

        private Map<Object, Function> matches = new HashMap<>();
        private LinkedList<Object> queue = new LinkedList<>();

        class RegularRule { String rule; RegularRule(String rule){ this.rule = rule.toUpperCase(); } }

        private void clearQueue(Function action){
            for (Object obj : queue)
                matches.put(obj, action);
            queue.clear();
        }

        void is(String obj) {
            queue.add(obj);
        }
        void is(String[] obj) {
            for (String o : obj)
                queue.add(o);
        }
        void is(String obj, Function action) {
            matches.put(obj, action);
            clearQueue(action);
        }
        void is(String[] obj, Function action) {
            for (String o : obj)
                matches.put(obj, action);
            clearQueue(action);
        }

        void match(String obj) {
            queue.add(new RegularRule(obj));
        }
        void match(String[] obj) {
            for (String o : obj)
                queue.add(new RegularRule(o));
        }
        void match(String[] obj, Function action) {
            for (String o : obj)
                matches.put(new RegularRule(o), action);
            clearQueue(action);
        }
        void match(String obj, Function action) {
            matches.put(new RegularRule(obj), action);
            clearQueue(action);
        }

        void otherwise(Function action) {
            matches.put(false, action);
            clearQueue(action);
        }

        Function execute(Object key) {
            final AtomicReference<Function> action = new AtomicReference<>(matches.get(false));

            System.out.println(answer.purpose);
            if(!(key instanceof String) && !(key instanceof Answer)) return action.get();

            Answer current;
            if (key.toString().equals(answer.toString()))
                current = answer;
            else
                current = new Answer(key.toString());

            List<String> rules = new LinkedList<>();

            for(Map.Entry<Object, Function> entry : matches.entrySet()){
                if(entry.getKey() instanceof String && current.purpose.contains(entry.getKey())){
                    return entry.getValue();
                }
                if(entry.getKey() instanceof RegularRule){
                    rules.add(((RegularRule) entry.getKey()).rule);
                }
            }


            ScenarioPlugin.pluginProxy.sendMessage("speech:match-any", new HashMap<String, Object>(){{
                put("speech", current.text);
                put("rules", rules);
            }}, (sender, data) -> {
                Integer i = ((Number) data).intValue();

                if (i >= 0){
                    for(Map.Entry<Object, Function> entry : matches.entrySet()){
                        if(entry.getKey() instanceof RegularRule && ((RegularRule) entry.getKey()).rule.equals(rules.get(i))){
                            action.set(entry.getValue());
                            break;
                        }
                    }
                }

                lock.unlock();
            });
            lock.lock();

            return action.get();
        }

    }

    protected void quit(){ Thread.currentThread().interrupt(); }

    private static class Answer{

        String text;
        List<String> purpose = null;

        Answer(String text){ this.text = text; }
        Answer(Map data){
            this.text = data.get("value").toString();
            Object p = data.get("purpose");
            if (p == null) return;
            if (p instanceof Collection){
                purpose = new LinkedList<>((Collection) p);
            } else {
                purpose = new LinkedList<>();
                purpose.add(p.toString());
            }
        }

        public String toString(){ return text; }
    }

    private static class Locker {

        boolean notified = false;

        public void lock(){
            if (!notified) {
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            notified = false;
        }

        public void unlock(){
            notified = true;
            synchronized (this) {
                this.notify();
            }
        }
    }
}