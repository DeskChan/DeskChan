package info.deskchan.talking_system;

import org.apache.commons.lang3.SystemUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class DefaultTagsListeners {

    public static TagsMap getDefaultTags(){
        TagsMap tags = new TagsMap();
        tags.put("os", fillOS());
        return tags;
    }

    private static String fillOS(){
        String os = null;
        if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX) os = "mac";
        else if (SystemUtils.IS_OS_UNIX) os = "linux";
        else if (SystemUtils.IS_OS_WINDOWS) os = "windows";

        return os;
    }

    protected static PhraseChooser.TagListener lastConversationListener = new PhraseChooser.TagListener() {
        @Override
        public boolean match(Phrase phrase) {
            try {
                Set<String> tag = phrase.getTag("lastConversation");

                if (tag != null && tag.size() > 0) {
                    int left = 0, right = -1;
                    try {
                        String text = tag.iterator().next();
                        if (text.contains("-")) {
                            String[] di = text.split("-");
                            left = Integer.valueOf(di[0]);
                            right = Integer.valueOf(di[1]);
                        } else left = Integer.parseInt(text);
                    } catch (Exception e) { }

                    Instant lastConversation = Instant.ofEpochMilli(Main.getProperties().getLong("lastConversation", 0));
                    long length = Duration.between(lastConversation, Instant.now()).toMinutes();
                    if(length < left || length >= right) return false;
                }
            } catch(Exception e){ }
            return true;
        }
    };

    protected static PhraseChooser.TagListener dateListener = new PhraseChooser.TagListener() {
        @Override public boolean match(Phrase phrase) {
            Calendar cal = Calendar.getInstance();

            // Filling TextBooleanSet, so date checking will be easier.
            // Example: listOf("1-3", "5-6") -> [false, true, true, true, false, true, true, false...]
            // 'false' at specific index means we cannot use phrase at current time

            Set<String> tag;
            /// possibleHour
            try {
                tag = phrase.getTag("possibleHour");
                if (tag != null && !new TextBooleanSet(24, tag).get(cal.get(Calendar.HOUR_OF_DAY)))
                    return false;
            } catch(Exception e){ Main.log(e); }

            /// possibleMinute
            try {
                tag = phrase.getTag("possibleMinute");
                if(tag != null && !new TextBooleanSet(60, tag).get(cal.get(Calendar.MINUTE)))
                    return false;
            } catch(Exception e){ Main.log(e); }

            /// possibleDay
            try {
                tag = phrase.getTag("possibleDay");
                if(tag != null && !new TextBooleanSet(31, 1, tag).get(cal.get(Calendar.DAY_OF_MONTH)))
                    return false;
            } catch(Exception e){ Main.log(e); }

            /// possibleDayOfWeek
            try {
                tag = phrase.getTag("possibleDayOfWeek");

                if(tag != null){
                    int dayIndex = cal.get(Calendar.DAY_OF_WEEK);
                    dayIndex = (dayIndex == 1 ? 7 : dayIndex - 1) - 1;

                    TextBooleanSet set = new TextBooleanSet(7);
                    for (String arg : tag){
                        int pos = DAYS.indexOf(arg.toLowerCase());
                        if (pos >= 0) set.set(pos, true);
                    }
                    if(!set.get(dayIndex))
                        return false;
                }
            } catch(Exception e){ Main.log(e); }

            /// possibleMonth
            try {
                tag = phrase.getTag("possibleMonth");

                if(tag != null){
                    TextBooleanSet set = new TextBooleanSet(12);
                    for(String arg : tag){
                        if (!MONTHS.contains(arg.toLowerCase())) set.fillFromString(arg);
                    }
                    if(!set.get(cal.get(Calendar.MONTH)))
                        return false;
                }
            } catch(Exception e){ Main.log(e); }

            /// dayTime, sleepTime
            try {
                if (phrase.tags.containsKey("sleepTime")) {
                    Calendar left = Calendar.getInstance();
                    while (left.get(Calendar.HOUR_OF_DAY) != Main.getProperties().getInteger("sleepTimeStart", 22))
                        left.add(Calendar.HOUR_OF_DAY, -1);

                    Calendar right = (Calendar) left.clone();
                    while (right.get(Calendar.HOUR_OF_DAY) != Main.getProperties().getInteger("sleepTimeEnd", 6))
                        right.add(Calendar.HOUR_OF_DAY, 1);

                    right.set(Calendar.MINUTE, 0);
                    Calendar current = Calendar.getInstance();
                    if(current.getTimeInMillis() < left.getTimeInMillis() ||
                            current.getTimeInMillis() > right.getTimeInMillis())
                        return false;
                }
            } catch(Exception e){ }

            return true;
        }
    };

    private static final List<String> DAYS = Arrays.asList(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"
    );
    private static final List<String> MONTHS = Arrays.asList(
            "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"
    );

    private static class TextBooleanSet {
        public int offset = 0;
        boolean[] set;

        public TextBooleanSet(int length) {
            set = new boolean[length];
            for (int i = 0; i < length; i++) {
                set[i] = false;
            }
        }

        public TextBooleanSet(int length, Collection<String> selected) {
            this(length);

            for(String item : selected)
                fillFromString(item);
        }

        public TextBooleanSet(int length, int offset, Collection<String> selected) {
            this(length);
            this.offset = offset;
            for(String item : selected)
                fillFromString(item);
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

        @Override
        public String toString() {
            StringBuilder sb=new StringBuilder();
            for (int i = 0; i < set.length; i++) {
                sb.append(set[i] ? 'x' : '_');
            }
            return sb.toString();
        }
    }
}

