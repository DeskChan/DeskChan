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
}
