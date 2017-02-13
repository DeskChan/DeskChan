package com.eternal_search.DeskChan.Core;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

public class Utils {
	
	private static FileSystem jarFileSystem = null;
	
	public static Path getResourcePath(String resourceName) {
		final URL url = Utils.class.getClassLoader().getResource(resourceName);
		if (url != null) {
			try {
				final URI uri = url.toURI();
				Path path;
				if (uri.getScheme().equals("jar")) {
					if (jarFileSystem == null) {
						jarFileSystem = FileSystems.newFileSystem(uri, new HashMap<String, String>());
					}
					path = jarFileSystem.getPath(resourceName);
				} else {
					path = Paths.get(uri);
				}
				return path;
			} catch (IOException | URISyntaxException e) {
				return null;
			}
		}
		return null;
	}
	
}
