package com.eternal_search.deskchan.core;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {
	public static void main(String[] args) {
		if (Arrays.asList(args).contains("--use-log-files")) {
			try {
				OutputStream errOutputStream = Files.newOutputStream(PluginManager.getDataDir().resolve("stderr.txt"));
				PrintStream errPrintStream = new PrintStream(errOutputStream);
				System.setErr(errPrintStream);
				OutputStream outOutputStream = Files.newOutputStream(PluginManager.getDataDir().resolve("stdout.txt"));
				PrintStream outPrintStream = new PrintStream(outOutputStream);
				System.setOut(outPrintStream);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		PluginManager pluginManager = PluginManager.getInstance();
		pluginManager.initialize();
		pluginManager.loadPluginByPackageName("com.eternal_search.deskchan.groovy_support");
		pluginManager.loadPluginByPackageName("com.eternal_search.deskchan.gui");
		try {
			Path jarPath = Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
			Path pluginsDirPath;
			if (Files.isDirectory(jarPath)) {
				pluginsDirPath = jarPath.resolve("../../../plugins");
			} else {
				pluginsDirPath = jarPath.getParent().resolve("../plugins");
			}
			DirectoryStream<Path> directoryStream = Files.newDirectoryStream(pluginsDirPath);
			for (Path path : directoryStream) {
				pluginManager.tryLoadPluginByPath(path);
			}
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
