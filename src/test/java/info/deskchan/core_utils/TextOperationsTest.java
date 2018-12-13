package info.deskchan.core_utils;

import info.deskchan.talking_system.DialogHandler;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TextOperationsTest {

    @Test
    public void test0(){
        Assert.assertEquals(TextOperations.simplifyWord("неопределённый"), "неопределен");
    }

    @Test
    public void test1(){
        List<String> r = TextOperations.simplifyWords(Arrays.asList("её", "глаза", "не", "зелёные"));
        Assert.assertEquals(r.size(), 3);
        Assert.assertEquals(r.get(0), "ее");
        Assert.assertEquals(r.get(1), "глаз");
        Assert.assertEquals(r.get(2), "незелен");
    }

    @Test
    public void test2(){
        String testString = "Неопределённый, неболЬшой     текст?";

        List<String> r = TextOperations.extractWords(testString);
        Assert.assertEquals(r.size(), 3);
        Assert.assertEquals(r.get(0), "Неопределённый");
        Assert.assertEquals(r.get(1), "неболЬшой");
        Assert.assertEquals(r.get(2), "текст");

        r = TextOperations.extractWordsLower(testString);
        Assert.assertEquals(r.size(), 3);
        Assert.assertEquals(r.get(0), "неопределённый");
        Assert.assertEquals(r.get(1), "небольшой");
        Assert.assertEquals(r.get(2), "текст");

        r = TextOperations.extractWordsUpper(testString);
        Assert.assertEquals(r.size(), 3);
        Assert.assertEquals(r.get(0), "НЕОПРЕДЕЛЁННЫЙ");
        Assert.assertEquals(r.get(1), "НЕБОЛЬШОЙ");
        Assert.assertEquals(r.get(2), "ТЕКСТ");

        r = TextOperations.extractSpeechParts(testString);
        Assert.assertEquals(r.size(), 4);
        Assert.assertEquals(r.get(0), "неопределённый");
        Assert.assertEquals(r.get(1), "небольшой");
        Assert.assertEquals(r.get(2), "текст");
        Assert.assertEquals(r.get(3), "?");
    }

    @Test
    public void test3(){
        List<String> r = TextOperations.defaultWordsExtraction("Привет, {user}! Как твои дела?");
        Assert.assertEquals(r.size(), 7);
        Assert.assertEquals(r.get(0), "привет");
        Assert.assertEquals(r.get(1), "user");
        Assert.assertEquals(r.get(2), "!");
        Assert.assertEquals(r.get(3), "как");
        Assert.assertEquals(r.get(4), "тв");
        Assert.assertEquals(r.get(5), "дел");
        Assert.assertEquals(r.get(6), "?");
    }

    @Test
    public void test4(){
        String input = "  привет! Как твои дела?у меня   всё хорошо.   Действительно хорошо... Верь       ";
        String output = "Привет! Как твои дела? У меня всё хорошо. Действительно хорошо... Верь.";

        Assert.assertEquals(TextOperations.prettifyText(input), output);
    }

    @Test
    public void test5(){
        String input = "  привет, юзер! Как твои дела; нормально?у меня   всё хорошо."
         + "    Действительно ,на самом деле, хорошо... Верь       ";

        List<List<String>> output = TextOperations.splitSentence(input);
        Assert.assertEquals(5, output.size());

        Assert.assertEquals(4, output.get(0).size());
        List<String> expected = Arrays.asList("привет", ",", "юзер", "!");
        for (int i = 0; i < 4; i++)
            Assert.assertEquals(expected.get(i), output.get(0).get(i));

        Assert.assertEquals(4, output.get(1).size());
        expected = Arrays.asList("Как твои дела", ";", "нормально", "?");
        for (int i = 0; i < 4; i++)
            Assert.assertEquals(expected.get(i), output.get(1).get(i));

        Assert.assertEquals(2, output.get(2).size());
        expected = Arrays.asList("у меня   всё хорошо", ".");
        for (int i = 0; i < 2; i++)
            Assert.assertEquals(expected.get(i), output.get(2).get(i));

        Assert.assertEquals(6, output.get(3).size());
        expected = Arrays.asList("Действительно", ",", "на самом деле", ",", "хорошо", "...");
        for (int i = 0; i < 6; i++)
            Assert.assertEquals(expected.get(i), output.get(3).get(i));

        Assert.assertEquals(2, output.get(4).size());
        expected = Arrays.asList("Верь", ".");
        for (int i = 0; i < 2; i++)
            Assert.assertEquals(expected.get(i), output.get(4).get(i));
    }
}
