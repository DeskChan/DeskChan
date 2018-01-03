package info.deskchan.core_utils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class LimitHashMapTests {

    private  LimitHashMap<String,Integer> lhm;

    @Before
    public void before(){
        lhm = new LimitHashMap<>(5);
        lhm.put("a",44);
        lhm.put("b",92);
        lhm.put("c",28);
        lhm.put("d",71);
        lhm.put("e",97);
        lhm.put("f",12);
        lhm.put("g",94);
    }

    @Test
    public void test0(){
        Assert.assertEquals(lhm.size(),5);
        Assert.assertNull(lhm.get("a"));
        Assert.assertNull(lhm.get("b"));
        Assert.assertTrue(lhm.get("c")==28);
        Assert.assertNotNull(lhm.get("c"));
        Assert.assertNotNull(lhm.get("d"));
        Assert.assertNotNull(lhm.get("e"));
        Assert.assertNotNull(lhm.get("f"));
        Assert.assertNotNull(lhm.get("g"));
    }

    @Test
    public void testRemove(){
        Assert.assertEquals(lhm.size(),5);
        lhm.remove("f");
        Assert.assertEquals(lhm.size(),4);
    }

    @Test
    public void test1(){
        lhm.remove("c");
        lhm.remove("d");
        lhm.remove("e");
        Assert.assertEquals(lhm.size(),2);
        Assert.assertNotNull(lhm.get("f"));
        Assert.assertNotNull(lhm.get("g"));

        lhm.put("h",952);
        lhm.put("i",72);
        lhm.put("j",92);
        Assert.assertEquals(lhm.size(),5);
        lhm.put("k",42);
        Assert.assertNull(lhm.get("f"));
    }

}
