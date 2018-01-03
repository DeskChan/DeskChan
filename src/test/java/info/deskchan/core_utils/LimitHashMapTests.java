package info.deskchan.core_utils;

import org.junit.Assert;
import org.junit.Test;

public class LimitHashMapTests {

    @Test
    public void test0(){
        LimitHashMap<String,Integer> lhm = new LimitHashMap<>(5);
        lhm.put("a",44);
        lhm.put("b",92);
        lhm.put("c",28);
        lhm.put("d",71);
        lhm.put("e",97);
        lhm.put("f",12);
        lhm.put("g",94);

        Assert.assertEquals(lhm.size(),5);
        Assert.assertNull(lhm.get("a"));
        Assert.assertNull(lhm.get("b"));
        Assert.assertTrue(lhm.get("c")==28);
    }

}
