package info.deskchan.core;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
	public static void main(String[] args) {
		PluginManager pluginManager = PluginManager.getInstance();
		pluginManager.initialize(args);
		pluginManager.tryLoadPluginByPackageName("info.deskchan.groovy_support");
		pluginManager.tryLoadPluginByPackageName("info.deskchan.gui_javafx");
		try {
			Path pluginsDirPath = PluginManager.getPluginsDirPath();
			DirectoryStream<Path> directoryStream = Files.newDirectoryStream(pluginsDirPath);
			for (Path path : directoryStream) {
				pluginManager.tryLoadPluginByPath(path);
			}
		} catch (IOException e) {
			PluginManager.log(e);
		}
	}
}
