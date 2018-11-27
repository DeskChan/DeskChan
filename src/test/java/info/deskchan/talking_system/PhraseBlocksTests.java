package info.deskchan.talking_system;

import org.junit.Assert;
import org.junit.Test;

public class PhraseBlocksTests {

    @Test
    public void test0(){
        Assert.assertEquals(30,
                PhraseBlocks.findNext("{gdfg[] [gdf(gdf'}'gdf)gfd]gdf}", 1, '}'));
        Assert.assertEquals(1,
                PhraseBlocks.findNext("',',',", 1, ','));
        Assert.assertEquals(3,
                PhraseBlocks.findNext("',',',", 0, ','));
        Assert.assertEquals(5,
                PhraseBlocks.findNext("',',',", 2, ','));
    }

    @Test
    public void test1(){
        String input = "{test} тест {test1()} тест {test2(1)} тест";
        PhraseBlocks blocks = new PhraseBlocks(input);

        Assert.assertEquals(3, blocks.getBlocks().size());

        Assert.assertTrue(blocks.getBlocks().containsKey("test"));
        PhraseBlocks.BlockData data = blocks.getBlocks().get("test");
        Assert.assertEquals("test", data.name);
        Assert.assertNull(data.args);
        Assert.assertEquals(0, data.start);
        Assert.assertEquals(6, data.end);
        Assert.assertEquals("{test}", data.toString());

        Assert.assertTrue(blocks.getBlocks().containsKey("test1"));
        data = blocks.getBlocks().get("test1");
        Assert.assertEquals("test1", data.name);
        Assert.assertNull(data.args);
        Assert.assertEquals(12, data.start);
        Assert.assertEquals(21, data.end);
        Assert.assertEquals("{test1}", data.toString());

        Assert.assertTrue(blocks.getBlocks().containsKey("test2"));
        data = blocks.getBlocks().get("test2");
        Assert.assertEquals("test2", data.name);
        Assert.assertNotNull(data.args);
        Assert.assertEquals(1, data.args.length);
        Assert.assertEquals("1", data.args[0]);
        Assert.assertEquals(27, data.start);
        Assert.assertEquals(37, data.end);
        Assert.assertEquals("{test2(1)}", data.toString());

        input = "тест {test3(\"te\", 2, \'et\')} тест {test4}";
        blocks = new PhraseBlocks(input);

        Assert.assertEquals(2, blocks.getBlocks().size());

        Assert.assertTrue(blocks.getBlocks().containsKey("test3"));
        data = blocks.getBlocks().get("test3");
        Assert.assertEquals("test3", data.name);
        Assert.assertNotNull(data.args);
        Assert.assertEquals(3, data.args.length);
        Assert.assertEquals("te", data.args[0]);
        Assert.assertEquals("2", data.args[1]);
        Assert.assertEquals("et", data.args[2]);
        Assert.assertEquals(5, data.start);
        Assert.assertEquals(27, data.end);
        Assert.assertEquals("{test3(te,2,et)}", data.toString());

        Assert.assertTrue(blocks.getBlocks().containsKey("test4"));
        data = blocks.getBlocks().get("test4");
        Assert.assertEquals("test4", data.name);
        Assert.assertNull(data.args);
        Assert.assertEquals(33, data.start);
        Assert.assertEquals(40, data.end);
        Assert.assertEquals("{test4}", data.toString());
    }
}
