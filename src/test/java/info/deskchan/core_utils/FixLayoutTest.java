package info.deskchan.core_utils;

import org.junit.Assert;
import org.junit.Test;

public class FixLayoutTest {

    @Test
    public void test0(){
        String missedRussianText = "Ghbdtn? rfr ndjb ltkf&",
               originRussianText = "Привет, как твои дела?";

        String missedEnglishText = "Руддщб рщц фку нщг,",
               originEnglishText = "Hello, how are you?";

        Assert.assertTrue (FixLayout.isLayoutMissedRussian(missedRussianText) > 0.5);
        Assert.assertTrue(FixLayout.isLayoutMissedRussian(originRussianText) < 0.5);

        Assert.assertTrue (FixLayout.isLayoutMissedEnglish(missedEnglishText) > 0.5);
        Assert.assertTrue(FixLayout.isLayoutMissedEnglish(originEnglishText) < 0.5);

        Assert.assertEquals(FixLayout.fixRussianEnglish(missedRussianText), originRussianText);
        Assert.assertEquals(FixLayout.fixRussianEnglish(missedEnglishText), originEnglishText);
    }
}
