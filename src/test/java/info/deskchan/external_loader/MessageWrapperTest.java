package info.deskchan.external_loader;

import info.deskchan.core.MessageDataMap;
import info.deskchan.external_loader.wrappers.InlineMessageWrapper;
import info.deskchan.external_loader.wrappers.MessageWrapper;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;


public class MessageWrapperTest {

    @Test
    public void test0(){
        InlineMessageWrapper wrapper = new InlineMessageWrapper();

        Map<Object, String> data = new HashMap<>();

        data.put(
                Arrays.asList("one", "two"),
                "[\"one\",\"two\"]"
        );
        Map map = new LinkedHashMap();
        map.put("one", 1);
        map.put("two", "2");
        data.put(
                map,
                "{\"one\":1,\"two\":\"2\"}"
        );
        data.put("1\n2\n3", "1\t2\t3");
        data.put(false, "false");

        for (Map.Entry<Object, String> entry : data.entrySet()){
            String res = wrapper.serialize(entry.getKey()).toString();
            Assert.assertEquals(entry.getValue(), res);
            if (entry.getKey() instanceof Map){
                Map map2 = (Map) wrapper.deserialize(res);
                Map map1 = (Map) entry.getKey();
                Assert.assertEquals(map1.size(), map2.size());
                for (Object e : map1.keySet()){
                    Assert.assertEquals(map1.get(e).toString(), map2.get(e).toString());
                }
            } else {
                Assert.assertEquals(entry.getKey(),wrapper.deserialize(res) );
            }
        }

        Assert.assertEquals("null", wrapper.serialize(null).toString());
        Assert.assertEquals(null, wrapper.deserialize("null"));

    }

    @Test
    public void test1(){
        InlineMessageWrapper wrapper = new InlineMessageWrapper();

        Map<MessageWrapper.Message, String> messages = new HashMap<>();

        messages.put(
                new MessageWrapper.Message("initializationCompleted", new LinkedList<>(), new HashMap<>()),
                "initializationCompleted\n"
        );

        messages.put(
                new MessageWrapper.Message("setTimer", Arrays.asList(Arrays.asList("1", 2, null, "3"), "two", new MessageDataMap("key1", "val1", "key2", 2)), new HashMap<>()),
                "setTimer [\"1\",2,\"null\",\"3\"] two {\"key1\":\"val1\",\"key2\":2}\n"
        );

        messages.put(
                new MessageWrapper.Message("log", Arrays.asList("1"), new MessageDataMap("key1", "val1", "key2", 2, "key3", null, "key4", Arrays.asList("1\n2\n3", 2, null, 3.5))),
                "log 1\nkey1 val1\nkey2 2\nkey4 [\"1\\t2\\t3\",2,\"null\",3.5]\n"
        );

        for (Map.Entry<MessageWrapper.Message, String> message : messages.entrySet()){
            String res = wrapper.wrap(message.getKey()).toString();
            Assert.assertEquals(message.getValue(), res);

            MessageWrapper.Message mes1 = message.getKey(), mes2 = wrapper.unwrap(res);

            try {
                Assert.assertEquals(mes1.getType(), mes2.getType());
                Assert.assertEquals(mes1.getRequiredArguments().size(), mes2.getRequiredArguments().size());
                for (int i = 0; i < mes1.getRequiredArguments().size(); i++) {
                    Assert.assertEquals("Argument " + i, mes1.getRequiredArguments().get(i).toString(), mes2.getRequiredArguments().get(i).toString());
                }
            } catch (Throwable e){
                System.out.println(message.getValue());
                throw e;
            }

        }
    }
}
