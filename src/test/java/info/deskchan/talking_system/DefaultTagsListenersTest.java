package info.deskchan.talking_system;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;

public class DefaultTagsListenersTest {

    TagsMap tags;

    String getDayOfWeek(int a){
        switch (1 + (a - 1) % 7){
            case Calendar.SUNDAY:
                return "sunday";
            case Calendar.MONDAY:
                return "monday";
            case Calendar.TUESDAY:
                return "tuesday";
            case Calendar.WEDNESDAY:
                return "wednesday";
            case Calendar.THURSDAY:
                return "thursday";
            case Calendar.FRIDAY:
                return "friday";
            case Calendar.SATURDAY:
                return "saturday";
            default: return null;
        }
    }
    void before(){
        Calendar cal = Calendar.getInstance();
        tags.put("possibleHour", Integer.toString(cal.get(Calendar.HOUR_OF_DAY)));
        tags.put("possibleMinute", Integer.toString(cal.get(Calendar.MINUTE)));
        tags.put("possibleDay", Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
        tags.put("possibleDayOfWeek", getDayOfWeek(cal.get(Calendar.DAY_OF_WEEK)));
        tags.put("possibleMonth", Integer.toString(cal.get(Calendar.MONTH)));
    }


    @Test
    public void test0(){
        Phrase phrase = new Phrase("");
        phrase.tags = tags;

        Assert.assertTrue(DefaultTagsListeners.dateListener.match(phrase));
    }

    @Test
    public void test1(){
        Phrase phrase = new Phrase("");
        Calendar cal = Calendar.getInstance();

        phrase.tags = new TagsMap(tags);
        /*phrase.tags.put("possibleHour", (cal.get(Calendar.HOUR_OF_DAY)+2) + "-" +(cal.get(Calendar.HOUR_OF_DAY)+3));
        System.out.println(phrase.tags);
        System.out.println(cal.get(Calendar.HOUR_OF_DAY));
        Assert.assertFalse(DefaultTagsListeners.dateListener.match(phrase));

        phrase.tags = new TagsMap(tags);
        phrase.tags.put("possibleMinute", (cal.get(Calendar.MINUTE)+2) + "-" +(cal.get(Calendar.MINUTE)+3));
        Assert.assertFalse(DefaultTagsListeners.dateListener.match(phrase));

        phrase.tags = new TagsMap(tags);
        phrase. tags.put("possibleDayOfWeek", getDayOfWeek(cal.get(Calendar.DAY_OF_WEEK + 1)));
        Assert.assertFalse(DefaultTagsListeners.dateListener.match(phrase));*/

        phrase.tags.put("possibleHour", (cal.get(Calendar.HOUR_OF_DAY)+2) + "-" +(cal.get(Calendar.HOUR_OF_DAY)+3));
        phrase.tags.put("possibleMinute", (cal.get(Calendar.MINUTE)+2) + "-" +(cal.get(Calendar.MINUTE)+3));
        phrase. tags.put("possibleDayOfWeek", getDayOfWeek(cal.get(Calendar.DAY_OF_WEEK + 1)));
        Assert.assertFalse(DefaultTagsListeners.dateListener.match(phrase));
    }

}
