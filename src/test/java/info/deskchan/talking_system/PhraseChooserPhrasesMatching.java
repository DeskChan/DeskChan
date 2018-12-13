package info.deskchan.talking_system;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

public class PhraseChooserPhrasesMatching {

    CharacterPreset preset = new CharacterPreset();
    CharacterController characterController = preset.character;
    EmotionsController emotionsController = preset.emotionState;
    PhrasesPack phrases = new PhrasesPack(PhrasesPack.PackType.USER);
    TagsMap pluginTags = new TagsMap("t2");
    String emotion;

    public void before(){
        preset.phrases.clear();
        phrases.clear();
        preset.phrases.add(phrases);
        emotionsController.reset();

        emotion = emotionsController.getEmotionsList().get(0);
        preset.setTags("t1: a");

        //empty
        phrases.add(new Phrase("0", new IntentList("HELLO")));

        // preset tag
        Phrase phrase = new Phrase("1", new IntentList("HELLO"));
        phrase.setTag("t1", "a");
        phrases.add(phrase);

        // plugin tag
        phrase = new Phrase("2", new IntentList("HELLO"));
        phrase.setTags("t2");
        phrases.add(phrase);

        // system tag
        phrase = new Phrase("3", new IntentList("HELLO"));
        phrase.setTag("possibleDay", Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
        phrases.add(phrase);

        // emotion tag
        phrase = new Phrase("4", new IntentList("HELLO"));
        phrase.setTag("emotion", emotion);
        phrases.add(phrase);

        // unknown tag
        phrase = new Phrase("5", new IntentList("HELLO"));
        phrase.setTag("t3", "a");
        phrases.add(phrase);

        // non matching character
        phrase = new Phrase("6", new IntentList("HELLO"));
        phrase.character = new CharacterRange(new int[][] {{-4, -4}, {4, 4}, {-1, 1}, {-4, 4}, {-4, 4}, {-4, 4}, {-4, 4}, {-4, 4}});
        phrases.add(phrase);

        // character matches only without emotion
        phrase = new Phrase("7", new IntentList("HELLO"));
        phrase.character = new CharacterRange(new int[][] {{-1, 1}, {-1, 1}, {-1, 1}, {-1, 1}, {-1, 1}, {-4, 4}, {-4, 4}, {-4, 4}});
        phrases.add(phrase);
    }

    @Test
    public void test0(){
        before();

        List<PhraseChooser.MatchingPhrase> matchingPhrases = PhraseChooser.recalculateMatchingPhrases(
              preset, pluginTags
        );
        List<Integer> results = getIntFromPhrases(matchingPhrases);
        Assert.assertTrue(compareLists(results, Arrays.asList(0, 1, 2, 3, 5, 7)));

        emotionsController.raiseEmotion(emotion, 2);
        matchingPhrases = PhraseChooser.recalculateMatchingPhrases(
                preset, pluginTags
        );
        results = getIntFromPhrases(matchingPhrases);
        Assert.assertTrue(compareLists(results, Arrays.asList(0, 1, 2, 3, 4, 5)));

        phrases.get(3).setTag("possibleHour",
                Integer.toString(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) - 1));
        matchingPhrases = PhraseChooser.recalculateMatchingPhrases(
                preset, new TagsMap()
        );
        results = getIntFromPhrases(matchingPhrases);
        Assert.assertTrue(compareLists(results, Arrays.asList(0, 1, 2, 3, 4, 5)));
        phrases.get(3).tags.remove("possibleHour");
    }

    @Test
    public void test1() {
        before();

        PhraseChooser.matchingPhrases = PhraseChooser.recalculateMatchingPhrases(
            preset, pluginTags
        );
        List<PhraseChooser.MatchingPhrase> matchingPhrases = PhraseChooser.getCurrentlyMatchingPhrases(
            new IntentList("HELLO"), preset, pluginTags, null
        );
        List<Integer> results = getIntFromPhrases(matchingPhrases);
        Assert.assertTrue(compareLists(results, Arrays.asList(0, 1, 2, 3, 7)));

        int hourNow = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        phrases.get(3).tags.put("possibleHour", (hourNow - 1) + "-" + (hourNow+1));
        matchingPhrases = PhraseChooser.getCurrentlyMatchingPhrases(
            new IntentList("HELLO"), preset, pluginTags, null
        );
        results = getIntFromPhrases(matchingPhrases);
        Assert.assertTrue(compareLists(results, Arrays.asList(0, 1, 2, 3, 7)));

        phrases.get(3).tags.put("possibleHour", (hourNow + 1) + "-" + (hourNow+3));
        matchingPhrases = PhraseChooser.getCurrentlyMatchingPhrases(
            new IntentList("HELLO"), preset, pluginTags, null
        );
        results = getIntFromPhrases(matchingPhrases);
        Assert.assertTrue(compareLists(results, Arrays.asList(0, 1, 2, 7)));

        matchingPhrases = PhraseChooser.getCurrentlyMatchingPhrases(
            new IntentList("HELLO"), preset, pluginTags, new TagsMap("t3: a")
        );
        results = getIntFromPhrases(matchingPhrases);
        Assert.assertTrue(compareLists(results, Arrays.asList(5)));

        emotionsController.raiseEmotion(emotion, 2);
        PhraseChooser.matchingPhrases = PhraseChooser.recalculateMatchingPhrases(
                preset, pluginTags
        );
        matchingPhrases = PhraseChooser.getCurrentlyMatchingPhrases(
            new IntentList("HELLO"), preset, pluginTags, null
        );
        results = getIntFromPhrases(matchingPhrases);
        Assert.assertTrue(compareLists(results, Arrays.asList(0, 1, 2)));
    }

    boolean compareLists(List<Integer> one, List<Integer> two){
        if (one.size() != two.size()) return false;
        for (Integer i : one)
            if (!two.contains(i)) return false;

        return true;
    }

    List<Integer> getIntFromPhrases(List<PhraseChooser.MatchingPhrase> list){
        List<Integer> result = new LinkedList<>();
        for (PhraseChooser.MatchingPhrase p : list)
            result.add(Integer.parseInt(p.phrase.phraseText));
        return result;
    }
}
