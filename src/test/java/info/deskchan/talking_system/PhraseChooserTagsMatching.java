package info.deskchan.talking_system;

import org.junit.Assert;
import org.junit.Test;

public class PhraseChooserTagsMatching {

    class TestData {
        TagsMap data;
        TagsMap result;
        TestData(String input, String output){
            data = new TagsMap(input);
            result = output != null ? new TagsMap(output) : null;
        }
    }

    TestData[] testData = new TestData[]{
            new TestData("", ""),
            new TestData("k1", ""),
            new TestData("k1: v11", ""),
            new TestData("k1: v12", null),
            new TestData("k1: v11, !k4, k6, k7", "k7")
    };

    @Test
    public void test0(){
        TagsMap presetTags = new TagsMap("k1: v11, k2: v21 v22, k3");
        TagsMap pluginsTags = new TagsMap("!k4, k5: !v51, k6");

        for (TestData test : testData){
            TagsMap result = PhraseChooser.MatchingPhrase.checkTagsMatching(test.data, presetTags, pluginsTags);
            Assert.assertEquals(result, test.result);
        }
    }
}
