package info.deskchan.jar_loader;

import info.deskchan.core.Plugin;
import info.deskchan.core.PluginLoader;
import info.deskchan.core.PluginManager;
import info.deskchan.core.PluginProxyInterface;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Main implements Plugin, PluginLoader {

    @Override
    public boolean initialize(PluginProxyInterface pluginProxy) {
        PluginManager.getInstance().registerPluginLoader(this);
        return true;
    }

    @Override
    public void unload() {
        PluginManager.getInstance().unregisterPluginLoader(this);
    }

    @Override
    public boolean matchPath(Path path) {
        if (Files.isDirectory(path)) {
            File[] files=path.toFile().listFiles();
            if(files==null) return false;
            for(File file : files){
                if(file.toString().endsWith(".jar")) return true;
            }
            return false;
        } else {
            return path.getFileName().toString().endsWith(".jar");
        }
    }

    @Override
    public void loadByPath(Path path) throws Throwable {
        File[] files=path.toFile().listFiles();
        if(files==null) return;

        URL[] url=new URL[1];

        for(File file : files){
            if(!file.toString().endsWith(".jar")) continue;

            url[0]=file.toURI().toURL();
            URLClassLoader child = new URLClassLoader (url, this.getClass().getClassLoader());

            JarFile jarFile = new JarFile(file.toString());
            Enumeration en = jarFile.entries();

            while (en.hasMoreElements()) {
                JarEntry jf=(JarEntry)en.nextElement();
                String fn = jf.getName();
                if (fn.endsWith(".class")){
                    String classname = fn.replace('/', '.').substring(0, fn.length() - 6);
                    if(!classname.equals("Main")) continue;
                    try {
                        Class classToLoad = Class.forName(classname, true, child);
                        fn=file.getName();
                        classname=file.getName().substring(0,file.getName().length()-4)+"."+classname;
                        Object plugin = classToLoad.newInstance();
                        if (plugin instanceof Plugin) {
                            PluginManager.getInstance().initializePlugin(classname, (Plugin) plugin);
                        }
                    } catch(Exception e){ }
                }
            }
        }
    }

}
