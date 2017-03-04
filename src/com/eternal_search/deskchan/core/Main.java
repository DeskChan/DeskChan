package com.eternal_search.deskchan.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
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
				File errFile = new File("err.txt");
				FileOutputStream errOutputStream = new FileOutputStream(errFile);
				PrintStream errPrintStream = new PrintStream(errOutputStream);
				System.setErr(errPrintStream);
				File outFile = new File("out.txt");
				FileOutputStream outOutputStream = new FileOutputStream(outFile);
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
