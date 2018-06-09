package info.deskchan.speech_command_system;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ParsersParseIntegerTest1 {
    private String a;
    private Long expected;
    @Parameterized.Parameters(name = "{index}: data({0})={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"две тыщи", 2000L},
                {"один", 1L},
                {"два", 2L},
                {"три", 3L},
                {"четыре",4L},
                {"пять",5L},
                {"шесть",6L},
                {"семь",7L},
                {"восемь",8L},
                {"девять",9L},
                {"десять", 10L},
                {"седьмое", 7L},
                {"второе", 2L},
                {"две тысячи шестого", 2006L},
                {"девятое прошлого", 9L},
                {"триста миллионов семсот сорак пят тысяч двести тридать один двадцать четыре тысячи три миллиарда", 300745231L},
                {"пятьбдесят", 50L},
                {"птдесят", 50L},
                {"пятдесчт", 50L},
                {"пяддесят пят", 55L},
                {"пяццот", 500L},
                {"пятсот", 500L},
                {"двадцат второй", 22L},
                {"добрый день господа", null},
                {"мне 70 лет", 70L},
                {"20 тысяч 543", 20543L},
                {"тысяча 200 сорок три", 1243L},
                {"пятае", 5L},
                {"сенадцать",17L},
                {"тринадцать восемьдесят девять",1389L},
                {"восемь восемьсот пять пять пять три пять три пять",8805553535L},
                {"восемь восемьсот пробел пять пять пять три пять три пять",88005553535L},
                {"девятьсот шестнадцать",916L},
                {"восемь девятьсот шестнадцать",8916L}
        });
    }

        public ParsersParseIntegerTest1(String a,Long expected) {
            this.a = a;
            this.expected = expected;
        }

    @Test
    public void test0() {
        ArrayList<String> words = PhraseComparison.toClearWords(a);
        Long result = Parsers.parseInteger(new Words(words));
        assertEquals(result,expected);
    }
}