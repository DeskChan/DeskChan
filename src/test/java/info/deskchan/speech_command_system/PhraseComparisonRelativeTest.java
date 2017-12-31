package info.deskchan.speech_command_system;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PhraseComparisonRelativeTest {

    String a,b;
    Boolean expected;

    @Parameterized.Parameters(name = "{index}: PhraseComparison.relative({0},{1})={2}")
    public static Collection<Object[]> data(){
        return Arrays.asList(new Object[][] {
                {"привет", "перевод",  false},
                {"два",     "три",     false},
                {"два",     "две",     true},
                {"один",    "сотня",   false},
                {"шесть",   "шест",    true},
                {"четверг", "четверть",true},
                {"второй",  "второго", true},
                {"пятьсот", "пяццот",  true},
                {"второй",  "добрый",  false},
                {"сутки",   "суток" ,  true},
                {"шестой",  "шестого", true},
                {"две",     "двести",  false},
                {"сутки",   "сутками", true},
                //AssertionError when run test from IDEA 2017??
                {"шестьсот","шестого", false}
        });
    }

    public PhraseComparisonRelativeTest(String a, String b,Boolean expected) {
        this.a = a;
        this.b = b;
        this.expected = expected;
    }

    @Test
    public void testRelative() {
        float result = PhraseComparison.relative(a,b);
        assertEquals(result>PhraseComparison.ACCURACY,expected);
    }
}
