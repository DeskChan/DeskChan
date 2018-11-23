package info.deskchan.talking_system.speech_exchange;

import info.deskchan.core.Path;
import info.deskchan.talking_system.CharacterController;
import info.deskchan.talking_system.IntentList;
import info.deskchan.talking_system.StandardCharacterController;
import org.junit.Assert;
import org.junit.Test;

public class SpeechExchangerTest {

    @Test
    public void test0() {
        CharacterController character = new StandardCharacterController();
        DialogLog log = null;
        try {
            Path file = new Path(getClass().getResource(".").toURI());
            file = new Path(file.toString().replace("classes", "resources")).resolve("DefaultDialog.log");
            log = new DialogLog(file);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
        Assert.assertEquals(log.size(), 9);
        float[] characterAuthor = new float[]{1, -1, 1, 2, -3, -1, 2, 2};
        float[] userAuthor = new float[]{0, -1, 2, 1, 1, 1, -4, 1};
        Assert.assertTrue(log.get(0) instanceof DialogLog.StartDialogLine);
        Assert.assertTrue(log.get(0).equals(new DialogLine(
                new Reaction(new IntentList("HELLO"), null),
                characterAuthor,
                DialogLine.Author.CHARACTER
        )));
        Assert.assertFalse(log.get(1) instanceof DialogLog.StartDialogLine);
        Assert.assertTrue(log.get(1).equals(new DialogLine(
                new Reaction(new IntentList("HELLO"), null),
                userAuthor,
                DialogLine.Author.USER
        )));
        Assert.assertFalse(log.get(2) instanceof DialogLog.StartDialogLine);
        Assert.assertTrue(log.get(2).equals(new DialogLine(
                new Reaction(new IntentList("WHAT_ARE_YOU_DOING"), null),
                characterAuthor,
                DialogLine.Author.CHARACTER
        )));
        Assert.assertFalse(log.get(3) instanceof DialogLog.StartDialogLine);
        Assert.assertTrue(log.get(3).equals(new DialogLine(
                new Reaction(new IntentList("ACTIONS_REPORT"), null),
                userAuthor,
                DialogLine.Author.USER
        )));
        Assert.assertFalse(log.get(4) instanceof DialogLog.StartDialogLine);
        Assert.assertTrue(log.get(4).equals(new DialogLine(
                new Reaction(new IntentList("OKAY, EVALUATION"), "happiness"),
                characterAuthor,
                DialogLine.Author.CHARACTER
        )));
        Assert.assertFalse(log.get(5) instanceof DialogLog.StartDialogLine);
        Assert.assertTrue(log.get(5).equals(new DialogLine(
                new Reaction("Не скучай", null),
                userAuthor,
                DialogLine.Author.USER
        )));
        Assert.assertFalse(log.get(6) instanceof DialogLog.StartDialogLine);
        Assert.assertTrue(log.get(6).equals(new DialogLine(
                new Reaction("Не буду", null),
                characterAuthor,
                DialogLine.Author.CHARACTER
        )));
        Assert.assertTrue(log.get(7) instanceof DialogLog.StartDialogLine);
        Assert.assertTrue(log.get(7).equals(new DialogLine(
                new Reaction(new IntentList("HELLO"), null),
                new float[]{0, 0, 1, 1, 0, 0, 0, 0},
                DialogLine.Author.USER
        )));
        Assert.assertFalse(log.get(8) instanceof DialogLog.StartDialogLine);
        Assert.assertTrue(log.get(8).equals(new DialogLine(
                new Reaction(new IntentList("GO_AWAY"), null),
                new float[]{-3, 1, 0, 0, 0, 0, 0, -2},
                DialogLine.Author.CHARACTER
        )));


        InputData<IExchangeable> input = log.getInputData(0);
        Assert.assertArrayEquals(input.history, new IExchangeable[]{});
        Assert.assertEquals(input.input, new IntentsData("HELLO"));
        Assert.assertArrayEquals(input.coords, combine(characterAuthor, new float[]{0,0,0,0,0,0,0,0}), 0.01F);


        input = log.getInputData(3);
        Assert.assertArrayEquals(input.history, new IExchangeable[]{
                new IntentsData("WHAT_ARE_YOU_DOING"),
                new IntentsData("HELLO"),
                new IntentsData("HELLO")
        });
        Assert.assertEquals(input.input, new IntentsData("ACTIONS_REPORT"));
        Assert.assertArrayEquals(
                input.coords,
                new float[]{0.0F, -0.75F, 1.5F, 0.75F, 0.75F, 0.75F, -3.0F, 0.75F, 0.75F, -0.75F, 0.75F, 1.5F, -2.25F, -0.75F, 1.5F, 1.5F},
                0.01F
        );


        input = log.getInputData(7);
        Assert.assertArrayEquals(input.history, new IExchangeable[]{});
        Assert.assertEquals(input.input, new IntentsData("HELLO"));
        Assert.assertArrayEquals(
                input.coords,
                new float[]{0F, 0F, 1F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F},
                0.01F
        );
    }
    private float[] combine(float[] a1, float[] a2){
        float[] r = new float[a1.length + a2.length];
        for (int i = 0; i < a1.length; i++)
            r[i] = a1[i];
        for (int i = 0; i < a2.length; i++)
            r[i + a1.length] = a2[i];
        return r;
    }
    @Test
    public void test1(){
        CharacterController character = new StandardCharacterController();
        SpeechExchanger exchanger = new SpeechExchanger(character);
        try {
            Path file = new Path(getClass().getResource(".").toURI());
            file = new Path(file.toString().replace("classes", "resources"));
            exchanger.loadLogs(file);
        } catch (Exception e){
            e.printStackTrace();
            Assert.fail();
        }
        DialogLine next = exchanger.next();
        Assert.assertTrue(next.equals(new DialogLine(
                new Reaction(new IntentList("HELLO"), null),
                character,
                DialogLine.Author.CHARACTER
        )));

        next = exchanger.next(new DialogLine(
                new Reaction(new IntentList("HELLO")),
                new float[]{0,0,0,0,0,0,0,0},
                DialogLine.Author.USER
        ));
        Assert.assertTrue(next.equals(new DialogLine(
                new Reaction(new IntentList("WHAT_ARE_YOU_DOING"), null),
                character,
                DialogLine.Author.CHARACTER
        )));

        character.setValues(new float[]{-2, 0, 0, 0, 0, 0, 0, -2});
        exchanger.setNewDialog();
        next = exchanger.next(new DialogLine(
                new Reaction(new IntentList("HELLO")),
                new float[]{0,0,0,0,0,0,0,0},
                DialogLine.Author.USER
        ));
        Assert.assertTrue(next.equals(new DialogLine(
                new Reaction(new IntentList("GO_AWAY"), null),
                character,
                DialogLine.Author.CHARACTER
        )));

    }
    @Test
    public void test2() {
        CharacterController character = new StandardCharacterController();
        SpeechExchanger exchanger = new SpeechExchanger(character);
        DialogLine next = exchanger.next();
        Assert.assertTrue(next.equals(new DialogLine(
                new Reaction(new IntentList("NOTHING"), null),
                character,
                DialogLine.Author.CHARACTER
        )));

        exchanger.setNewDialog();
        next = exchanger.next(new DialogLine(
                new Reaction("Hello", "fun"),
                character,
                DialogLine.Author.USER
        ));
        Assert.assertTrue(next.equals(new DialogLine(
                new Reaction("Hello", "fun"),
                character,
                DialogLine.Author.CHARACTER
        )));
    }
}
