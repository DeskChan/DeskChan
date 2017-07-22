package info.deskchan.talking_system;

import org.apache.commons.lang3.SystemUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class DefaultTagsListeners {
    public static void parseForTagsRemove(String sender, String messagetag, Object dat) {
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
            List<String> tag;

            /// operation system
            try {
                tag = (List<String>) entry.getOrDefault("os", null);
                if (tag != null && tag.size()>0) {
                    String os = null;
                    if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX) os = "mac";
                    else if (SystemUtils.IS_OS_UNIX) os = "linux";
                    else if (SystemUtils.IS_OS_WINDOWS) os = "windows";
                    if (os == null) quotes_list.add(entry);
                    else {
                        boolean found = false;
                        for (String arg : tag) {
                            if (arg.charAt(0) == '"' && arg.charAt(arg.length() - 1) == '"')
                                arg = arg.substring(1, arg.length() - 1);
                            if (arg.equals(os)) found = true;
                        }
                        if (!found) quotes_list.add(entry);
                    }
                }
            } catch(Exception e){ }
        }
        Main.getPluginProxy().sendMessage(sender,ret);
    }
    public static void parseForTagsReject(String sender, String messagetag, Object dat){
        HashMap<String,Object> data=(HashMap<String,Object>) dat;
        ArrayList<HashMap<String,Object>> list=(ArrayList<HashMap<String,Object>>) data.getOrDefault("quotes",null);
        HashMap<String,Object> ret=new HashMap<>();
        ret.put("seq",data.get("seq"));
        ArrayList<HashMap<String,Object>> quotes_list=new ArrayList<>();
        if(list==null){
            Main.getPluginProxy().sendMessage(sender,ret);
            return;
        }
        ret.put("quotes",quotes_list);
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());

        for(HashMap<String,Object> entry : list){
            List<String> tag;

            /// possibleHour
            try {
                tag = (List<String>) entry.getOrDefault("possibleHour", null);
                TextBooleanSet set=new TextBooleanSet(24);
                if(tag!=null){
                for(String arg : tag)
                    set.fillFromString(arg);
                if(!set.get(cal.get(Calendar.HOUR_OF_DAY))) quotes_list.add(entry);}
            } catch(Exception e){ Main.log("1"); Main.log(e); }

            /// possibleMinute
            try {
                tag = (List<String>) entry.getOrDefault("possibleMinute", null);
                TextBooleanSet set=new TextBooleanSet(60);
                if(tag!=null){
                for(String arg : tag)
                    set.fillFromString(arg);
                if(!set.get(cal.get(Calendar.MINUTE))) quotes_list.add(entry);}
            } catch(Exception e){ Main.log("2"); Main.log(e); }

            /// possibleDay
            try {
                tag = (List<String>) entry.getOrDefault("possibleDay", null);
                TextBooleanSet set=new TextBooleanSet(31);
                set.offset=1;
                if(tag!=null){
                for(String arg : tag)
                    set.fillFromString(arg);
                if(!set.get(cal.get(Calendar.DAY_OF_MONTH))) quotes_list.add(entry);}
            } catch(Exception e){ Main.log("3"); Main.log(e); }

            /// possibleDayOfWeek
            try {
                tag = (List<String>) entry.getOrDefault("possibleDayOfWeek", null);
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                dow=(dow == 1 ? 7 : dow-1)-1;
                TextBooleanSet set=new TextBooleanSet(7);
                String[] days=new String[]{"monday","tuesday","wednesday","thursday","saturday","sunday"};
                if(tag!=null){
                for(String arg : tag){
                    boolean found=false;
                    for(int i=0;i<days.length;i++)
                        if(days[i].equals(arg)) {
                            set.set(i, true);
                            found = true;
                        }
                    if(!found) set.fillFromString(arg);
                }
                if(!set.get(dow)) quotes_list.add(entry);}
            } catch(Exception e){ Main.log("4"); Main.log(e); }

            /// possibleMonth
            try {
                tag = (List<String>) entry.getOrDefault("possibleMonth", null);
                TextBooleanSet set=new TextBooleanSet(12);
                String[] months=new String[]{"january","february","march","april","may","june","july","august","september","october","november","december"};
                if(tag!=null){
                for(String arg : tag){
                    boolean found=false;
                    for(int i=0;i<months.length;i++)
                        if(months[i].equals(arg)){
                            set.set(i, true);
                            found = true;
                        }
                    if(!found) set.fillFromString(arg);
                }
                if(!set.get(cal.get(Calendar.MONTH))) quotes_list.add(entry);}
            } catch(Exception e){ Main.log("5"); Main.log(e); }

            /// lastConversation
            try {
                tag = (List<String>) entry.getOrDefault("lastConversation", null);
                if (tag != null && tag.size()>0) {
                    Integer barrier = Integer.parseInt(tag.get(0));
                    Instant lastConversation=Instant.parse(Main.getProperty("lastConversation","0"));
                    if(Duration.between(lastConversation,Instant.now()).toMinutes()<barrier)
                        quotes_list.add(entry);
                }
            } catch(Exception e){ }
        }
        Main.getPluginProxy().sendMessage(sender,ret);
            //if(list==null)
            //for()
    }
}

class TextBooleanSet {
    public int offset = 0;
    boolean[] set;
    public TextBooleanSet(int length) {
        set = new boolean[length];
        for (int i = 0; i < length; i++) {
            set[i] = false;
        }
    }
    public void set(int index, boolean value) {
        if (index - offset < set.length && index >= 0) {
            set[index - offset] = value;
        }
    }
    public boolean get(int index) {
        return set[index - offset];
    }
    public void fillFromString(String text) {
        if (text == null || text.length() == 0)
            return;
        if (text.charAt(0)=='"' && text.charAt(text.length()-1)=='"')
            text=text.substring(1,text.length()-1);
        if (text.charAt(0) == 'x' || text.charAt(0) == '_') {
            for (int i = 0; i < text.length(); i++) {
                set[i] = (text.charAt(i) == 'x');
            }
            return;
        }
        String[] ar = text.split(" ");
        for (String di : ar) {
            if (di.contains("-")) {
                String[] di2 = di.split("-");
                try {
                    int n1 = Integer.valueOf(di2[0]) - offset;
                    int n2 = Integer.valueOf(di2[di2.length - 1]) - offset;
                    for (int i = n1; i != n2; i = (i + 1) % set.length) {
                        set[i] = true;
                    }
                } catch (Exception e) { }
            } else {
                try {
                    int n = Integer.valueOf(di);
                    set[n - offset] = true;
                } catch (Exception e) { }
            }
        }
    }
    public String toString() {
        StringBuilder sb=new StringBuilder();
        for (int i = 0; i < set.length; i++) {
            sb.append(set[i] ? 'x' : '_');
        }
        return sb.toString();
    }
}