package info.deskchan.core;

import java.util.*;

/**
 * This class handles all alternatives from all plugins.
 * Alternatives - our special implementation of hooking pattern.
 * You can register queue of functions and name it by tag, like 'DeskChan:say'.
 * Any plugin can interfere by adding its own function inside queue.
 *
 * Any function added inside queue needs to have its own tag and priority.
 *
 * Don't use this class outside core plugin, use messages instead.
 */
public class Alternatives {
    
    protected static final Map<String, List<AlternativeInfo>> alternatives = new HashMap<>();

    public static void registerAlternative(String srcTag, String dstTag, String plugin, Object priority) {
        int _priority;
        if (priority instanceof Number)
            _priority = ((Number) priority).intValue();
        else {
            try {
                _priority = Integer.parseInt(priority.toString());
            } catch (Exception e){
                throw new ClassCastException(
                        "Cannot cast '" + priority.toString() + "', type=" + priority.getClass() + "  to integer");
            }
        }

        List<AlternativeInfo> list = alternatives.get(srcTag);
        if (list == null) {
            list = new LinkedList<>();
            alternatives.put(srcTag, list);
            PluginManager.getInstance().registerMessageListener(srcTag, listener);
            PluginManager.getInstance().registerMessageListener(srcTag+"#", listener);
        }

        Iterator<AlternativeInfo> iterator = list.iterator();
        while (iterator.hasNext()) {
            AlternativeInfo info = iterator.next();
            if (info.isEqual(dstTag, plugin)) {
                iterator.remove();
                break;
            }
        }

        list.add(new AlternativeInfo(dstTag, plugin, _priority));
        list.sort(new Comparator<AlternativeInfo>() {
            @Override
            public int compare(AlternativeInfo o1, AlternativeInfo o2) {
                return Integer.compare(o2.priority, o1.priority);
            }
        });

        PluginManager.log("Registered alternative " + dstTag + " for tag " + srcTag + " with priority: " + priority + ", by plugin " + plugin);
    }

    public static void unregisterAlternative(String srcTag, String dstTag, String plugin) {
        List<AlternativeInfo> list = alternatives.get(srcTag);
        if (list == null)
            return;

        Iterator<AlternativeInfo> iterator = list.iterator();
        while (iterator.hasNext()) {
            AlternativeInfo info = iterator.next();
            if (info.isEqual(dstTag, plugin)) {
                iterator.remove();
                PluginManager.log("Unregistered alternative " + dstTag + " for tag " + srcTag);
                break;
            }
        }

        if (list.isEmpty()) {
            alternatives.remove(srcTag);
            PluginManager.getInstance().unregisterMessageListener(srcTag, listener);
            PluginManager.getInstance().unregisterMessageListener(srcTag+"#", listener);
            PluginManager.log("No more alternatives for " + srcTag);
        }
    }

    public static void unregisterAlternativesByPlugin(String plugin){
        synchronized (Alternatives.class) {
            Iterator < Map.Entry<String, List<AlternativeInfo>> > mapIterator = alternatives.entrySet().iterator();
            while (mapIterator.hasNext()) {
                Map.Entry <String, List<AlternativeInfo>> entry = mapIterator.next();
                List<AlternativeInfo> l = entry.getValue();
                Iterator<AlternativeInfo> iterator = l.iterator();
                while (iterator.hasNext()) {
                    AlternativeInfo info = iterator.next();
                    if (info.plugin.equals(plugin)) {
                        iterator.remove();
                        PluginManager.log("Unregistered alternative " + info.tag + " for tag " + entry.getKey());
                    }
                }
                if (l.isEmpty()) {
                    String srcTag = entry.getKey();
                    PluginManager.getInstance().unregisterMessageListener(srcTag, listener);
                    PluginManager.log("No more alternatives for " + srcTag);
                    mapIterator.remove();
                }
            }
        }
    }

    public static Map<String, Object> getAlternativesMap() {
        Map<String, Object> m = new HashMap<>();
        for (Map.Entry<String, List<AlternativeInfo>> entry : alternatives.entrySet()) {
            List<Map<String, Object>> l = new ArrayList<>();
            for (AlternativeInfo info : entry.getValue()) {
                l.add(new HashMap<String, Object>() {{
                    put("tag", info.tag);
                    put("plugin", info.plugin);
                    put("priority", info.priority);
                }});
            }
            m.put(entry.getKey(), l);
        }
        return m;
    }

    public static void callNextAlternative(String sender, String tag, String currentAlternative, Object data) {
        List<AlternativeInfo> list = alternatives.get(tag);
        if (list == null || list.isEmpty())
            return;

        Iterator<AlternativeInfo> iterator = list.iterator();
        if (currentAlternative != null){
            do {
                if (!iterator.hasNext()){
                    PluginManager.log("Warning: tag \"" + currentAlternative + "\" is not subscribed to \"" + tag + "\"");
                    return;
                }
                AlternativeInfo nextInfo = iterator.next();
                if (nextInfo.tag.equals(currentAlternative)) break;
            } while (true);
        }

        try {
            AlternativeInfo info = iterator.next();
            PluginManager.getInstance().sendMessage(sender, info.tag, data);
        } catch (NoSuchElementException e) { }
    }

    private static final MessageListener listener = new MessageListener() {
        @Override
        public void handleMessage(String sender, String tag, Object data) {
            int delimiter = tag.indexOf("#");
            String senderTag = null;
            if (delimiter > 0) {
                senderTag = tag.substring(delimiter + 1);
                tag = tag.substring(0, delimiter);
            }

            List<AlternativeInfo> list = alternatives.get(tag);
            if (list == null || list.isEmpty())
                return;

            Iterator<AlternativeInfo> iterator = list.iterator();
            if (senderTag != null){
                do {
                    if (!iterator.hasNext()) return;
                    AlternativeInfo nextInfo = iterator.next();
                    if (nextInfo.tag.equals(senderTag)) break;
                } while (true);
            }

            try {
                AlternativeInfo info = iterator.next();
                PluginManager.getInstance().sendMessage(sender, info.tag, data);
            } catch (NoSuchElementException e) { }
        }
    };

    static class AlternativeInfo {
        String tag;
        String plugin;
        int priority;

        AlternativeInfo(String tag, String plugin, int priority) {
            this.tag = tag;
            this.plugin = plugin;
            this.priority = priority;
        }

        boolean isEqual(String tag, String plugin){
            return this.tag.equals(tag) && this.plugin.equals(plugin);
        }

        @Override
        public String toString(){
            return tag + "(" + plugin + ")" + "=" + priority;
        }
    }
    
}
