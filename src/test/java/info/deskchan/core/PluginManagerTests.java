package info.deskchan.core;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

public class PluginManagerTests {
    static PluginManager pluginManager;
    @BeforeClass
    public static void before() {
        pluginManager = PluginManager.getInstance();
        String[] args = {};
        pluginManager.initialize(args);
    }

    @Test
    public void test0() {
        System.out.println("Plugins list");
        List<String> plugins = pluginManager.getPlugins();
        for (String plugin : plugins) {
            System.out.println(plugin);
        }
    }
}
