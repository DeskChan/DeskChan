package info.deskchan.talking_system;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class TagsMapTests {

    @Test
    public void test0(){
        TagsMap map = new TagsMap("part: head legs");

        Assert.assertTrue(map.match("part: legs"));
        Assert.assertTrue(map.match("part", "head"));
        Assert.assertFalse(map.match("part", "spine"));
        Assert.assertTrue(map.match("part", new ArrayList<String>(){{
            add("!spine");
        }}));
        Assert.assertTrue(map.match("part: legs head"));
        Assert.assertTrue(map.match("part"));
    }

    @Test
    public void test1(){
        TagsMap map = new TagsMap("species: ai, sleepTime, !os");

        Assert.assertTrue(map.match("sleepTime"));
        Assert.assertFalse(map.match("dayTime"));
        Assert.assertTrue(map.match("!dayTime"));
        Assert.assertFalse(map.match("!sleepTime"));
        Assert.assertFalse(map.match("os: win"));
        Assert.assertTrue(map.match("sleepTime, species: !android"));
        Assert.assertFalse(map.match("sleepTime, species: android"));
    }

    @Test
    public void test2(){
        TagsMap map = new TagsMap();
        try {
            map.putFromText("!sleepTime: k1");
            Assert.fail();
        } catch (IllegalArgumentException e){ }
        try {
            map.put("!sleepTime", Arrays.asList("k1", "k2"));
            Assert.fail();
        } catch (IllegalArgumentException e){ }
    }

    @Test
    public void test3(){
        TagsMap map = new TagsMap("species: ai, sleepTime");
        map.put("!sleepTime");
        Assert.assertTrue(!map.containsKey("sleepTime") && map.containsKey("!sleepTime"));
    }

    @Test
    public void test5(){
        TagsMap map1 = new TagsMap("k1: v1 v2, k2"), map2 = new TagsMap("k2, k1: v2 v1");
        Assert.assertEquals(map1, map2);
        map2.put("!k3");
        Assert.assertNotEquals(map1, map2);
        map2.put("k3");
        Assert.assertNotEquals(map1, map2);
    }
}
