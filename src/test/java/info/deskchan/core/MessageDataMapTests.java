package info.deskchan.core;

import info.deskchan.MessageData.GUI.Control;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MessageDataMapTests {

    @Test
    public void test0(){
        MessageDataMap map = new MessageDataMap(new HashMap<String, Object> (){{
            put("str", "value");
            put("null", null);
            put("int", "  5 ");
            put("bool", "tRue   ");
        }});
        Assert.assertEquals(map.size(), 3);

        Assert.assertTrue(map.containsKey("str"));
        Assert.assertEquals(map.get("str"), "value");
        Assert.assertEquals(map.getString("str"), "value");

        Assert.assertTrue(map.containsKey("int"));
        Assert.assertEquals(map.get("int"), "  5 ");
        Assert.assertEquals(map.getString("int"), "  5 ");
        Assert.assertTrue(map.getInteger("int") == 5);

        Assert.assertTrue(map.containsKey("bool"));
        Assert.assertEquals(map.get("bool"), "tRue   ");
        Assert.assertEquals(map.getString("bool"), "tRue   ");
        Assert.assertTrue(map.getBoolean("bool") == true);
    }

    @Test
    public void test1(){
        MessageDataMap map = new MessageDataMap();

        Assert.assertEquals(map.getInteger("a", 5), 5);
        Assert.assertEquals(map.getLong("a", 5L), 5L);
        Assert.assertTrue(map.getFloat("a", 5F) == 5F);
        Assert.assertTrue(map.getDouble("a", 5.0) == 5.0);
        Assert.assertEquals(map.getString("a", "str"), "str");
        Assert.assertEquals(map.getBoolean("a", true), true);
        Assert.assertEquals(map.getOneOf("a", Control.ControlType.values(), Control.ControlType.Button), Control.ControlType.Button);
        Assert.assertEquals(map.getFile("a", "."), new File("."));

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(5L);
        Assert.assertEquals(map.getDateTimeFromStamp("a", 5L), cal);
    }

    @Test
    public void test2(){
        MessageDataMap map = new MessageDataMap();

        Assert.assertFalse(map.containsKey("null"));
        map.put("null", null);
        Assert.assertFalse(map.containsKey("null"));
        map.put("null", "null");
        Assert.assertFalse(map.containsKey("null"));
        map.put("null", "");
        Assert.assertFalse(map.containsKey("null"));
    }

    @Test
    public void test3(){
        Map<Object, Integer> data = new HashMap<>();
        data.put(3, 5);
        data.put("key", 7);
        MessageDataMap map = new MessageDataMap(data);

        Assert.assertFalse(map.containsKey(3));
        Assert.assertTrue(map.containsKey("3"));

        Assert.assertTrue(map.containsKey("key"));
        Assert.assertEquals(map.get("key"), 7);

        map = new MessageDataMap("key", 7);
        Assert.assertTrue(map.containsKey("key"));
        Assert.assertEquals(map.get("key"), 7);
    }
}
