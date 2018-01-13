package info.deskchan.core;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CorePluginTests {
    private static PluginManager pluginManager;

    private Map<String, List<CorePlugin.AlternativeInfo>> alternatives = null;
    private static PluginProxy pluginProxy = null;

    private void getAlternatives(){
        alternatives = null;
        pluginProxy.sendMessage("core:debug-output-alternatives", null, (sender, data) -> {
            alternatives = (Map<String, List<CorePlugin.AlternativeInfo>>) data;
            System.out.println("++++++++++++++");
            System.out.println(alternatives);
            System.out.println("++++++++++++++");
        });
        Assert.assertNotNull(alternatives);
    }

    @BeforeClass
    public static void before() {
        pluginManager = PluginManager.getInstance();
        String[] args = {};
        pluginManager.initialize(args);
        pluginManager.unloadPlugin("core");
        System.out.println("original core was unload");
        pluginManager.tryLoadPluginByClass(ProxyCore.class);
        System.out.println("ProxyCore was load");
        pluginProxy = pluginManager.getPlugin("core");
    }

    @AfterClass
    public static void after() {
        pluginManager.quit();
    }

    @Test
    public void test0() throws InterruptedException {

        pluginProxy.addMessageListener("core:test1", (sender, tag, data) -> {
            System.out.println("test 1");
            pluginProxy.sendMessage("DeskChan:test#core:test1", null);
        });

        pluginProxy.addMessageListener("core:test2", (sender, tag, data) -> {
            System.out.println("test 2");
            pluginProxy.sendMessage("DeskChan:test#core:test2", null);
        });

        pluginProxy.addMessageListener("core:test3", (sender, tag, data) -> {
            System.out.println("test 3");
        });

        System.out.println("sendMessage->core:register-alternative@(srcTag:DeskChan:test->dstTag:core:test1,priority:1000)");
        pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>() {{
            put("srcTag", "DeskChan:test");
            put("dstTag", "core:test1");
            put("priority", 1000);
        }});

        System.out.println("sendMessage->core:register-alternative");
        pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>() {{
            put("srcTag", "DeskChan:test");
            put("dstTag", "core:test2");
            put("priority", 500);
        }});

        getAlternatives();


        List<CorePlugin.AlternativeInfo> list = alternatives.get("DeskChan:test");
        Assert.assertEquals(list.get(0).tag, "core:test1");
        Assert.assertEquals(list.get(0).plugin,"core");
        Assert.assertEquals(list.get(0).priority,1000);

        CorePlugin.AlternativeInfo alternativeInfo2 = list.get(1);
        Assert.assertEquals(list.get(1).tag, "core:test2");
        Assert.assertEquals(list.get(1).plugin,"core");
        Assert.assertEquals(list.get(1).priority,500);

        pluginProxy.sendMessage("DeskChan:test", null);

        pluginProxy.sendMessage("core:unregister-alternative", new HashMap<String, Object>(){{
            put("srcTag", "DeskChan:test");
            put("dstTag", "core:test1");
        }});

        getAlternatives();
        Assert.assertEquals(list.get(0).tag, "core:test2");
        Assert.assertEquals(list.get(0).plugin, "core");
        Assert.assertEquals(list.get(0).priority, 500);

        pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>(){{
            put("srcTag", "DeskChan:test");
            put("dstTag", "core:test3");
            put("priority", 1500);
        }});

        getAlternatives();
        Assert.assertEquals(list.get(0).tag, "core:test3");
        Assert.assertEquals(list.get(0).plugin, "core");
        Assert.assertEquals(list.get(0).priority, 1500);

        pluginProxy.sendMessage("DeskChan:test", null);

        pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>(){{
            put("srcTag", "DeskChan:test");
            put("dstTag", "core:test3");
            put("priority", 400);
        }});
        getAlternatives();
        Assert.assertEquals(list.get(0).tag, "core:test2");
        Assert.assertEquals(list.get(0).plugin, "core");
        Assert.assertEquals(list.get(0).priority, 500);

        Assert.assertEquals(list.get(1).tag, "core:test3");
        Assert.assertEquals(list.get(1).plugin, "core");
        Assert.assertEquals(list.get(1).priority, 400);
        
        pluginProxy.sendMessage("DeskChan:test", null);
    }
}


