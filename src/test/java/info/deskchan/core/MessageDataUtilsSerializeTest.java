package info.deskchan.core;

import info.deskchan.MessageData.Core.AddCommand;
import info.deskchan.MessageData.Core.Quit;
import info.deskchan.MessageData.GUI.ShowCharacter;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MessageDataUtilsSerializeTest {
    @Test
    public void test0() {

        Map<MessageData, Object> tests = new HashMap<>();
        tests.put(
            new AddCommand ("1", "2"),
            new HashMap<String, Object>(){{
                put("tag", "1");
                put("info", "2");
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

        for (Map.Entry<MessageData, Object> test : tests.entrySet()){
            assert testEntry(test.getKey(), test.getValue());
        }
    }

    private boolean testEntry(MessageData data, Object compare){
        Object result = MessageDataUtils.serialize(data);

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
