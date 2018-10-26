package info.deskchan.core_utils;

import org.junit.Test;

import java.util.ArrayList;

public class TagsMapTests {

    @Test
    public void test0(){
        TextOperations.TagsMap map = new TextOperations.TagsMap("part: head legs");
        assert (map.match("part: legs"));
        assert (map.match("part", "head"));
        assert (!map.match("part", "spine"));
        assert (map.match("part", new ArrayList<String>(){{
            add("!spine");
        }}));
        assert (map.match("part: legs head"));
        assert (map.match("part"));
    }

    @Test
    public void test1(){
        TextOperations.TagsMap map = new TextOperations.TagsMap("species: ai, sleepTime");
        assert (map.match("sleepTime"));
        assert (!map.match("dayTime"));
        assert (!map.match("!sleepTime"));
        assert (map.match("sleepTime, species: !android"));
        assert (!map.match("sleepTime, species: android"));
    }
}
