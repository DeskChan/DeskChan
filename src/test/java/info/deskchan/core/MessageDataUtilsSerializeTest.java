package info.deskchan.core;

import info.deskchan.MessageData.Core.AddCommand;
import info.deskchan.MessageData.Core.Quit;
import info.deskchan.MessageData.GUI.Control;
import info.deskchan.MessageData.GUI.ShowCharacter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MessageDataUtilsSerializeTest {
    @Test
    public void test0() {

        Map<Object, Object> tests = new HashMap<>();
        tests.put(
            new AddCommand ("1", "2"),
            new HashMap<String, Object>(){{
                put("tag", "1");
                put("info", "2");
            }}
        );

        tests.put(
                new Control(Control.ControlType.Button, "id", "text", "label", "lab"),
                new HashMap<String, Object>(){{
                    put("type", "Button");
                    put("id", "id");
                    put("value", "text");
                    put("label", "lab");
                }}
        );

        tests.put(
                new ShowCharacter(),
                null
        );

        tests.put(
                new Quit(5),
                5L
        );

        for (Map.Entry<Object, Object> test : tests.entrySet()){
            try {
                assert testEntry(MessageDataUtils.serialize(test.getKey()), test.getValue());
            } catch (Error e){
                System.out.println("Actual: " + test.getKey());
                System.out.println("Expected: " + test.getValue());
                Assert.fail();
            }
        }
    }

    private boolean testEntry(Object result, Object compare){

        if (result == null && compare == null) return true;
        if (result instanceof Map && compare instanceof Map){
            Map _r = (Map<String, Object>) result,
                _c = (Map<String, Object>) compare;

            for (String key : (Collection<String>) _r.keySet()){
                if (!_r.get(key).equals(_c.get(key))) return false;
            }

            for (String key : (Collection<String>) _c.keySet()){
                if (!_c.get(key).equals(_r.get(key))) return false;
            }
            return true;
        }
        if (result != null && result.equals(compare)) return true;

        return false;
    }

}
