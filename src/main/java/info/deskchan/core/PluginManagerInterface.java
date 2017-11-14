package info.deskchan.core;


public interface PluginManagerInterface {

    void registerPluginLoader(PluginLoader loader);
    void registerPluginLoader(PluginLoader loader, String[] extensions);
    void registerPluginLoader(PluginLoader loader, String extension);

    void unregisterPluginLoader(PluginLoader loader);

    boolean initializePlugin(String id, Plugin plugin, PluginConfigInterface config) throws Throwable;

}
