package com.eternal_search.deskchan.gui;

import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Skin implements Comparable<Skin> {
	
	static final int INVALID = 0;
	static final int SINGLE_IMAGE = 1;
	static final int IMAGE_SET = 2;
	
	private final Path basePath;
	private final int type;
	private final boolean builtin;
	private String description;
	
	Skin(Path path, boolean builtin) {
		this.builtin = builtin;
		basePath = path;
		description = "N/A";
		if (basePath == null) {
			type = INVALID;
			return;
		}
		final String extension = FilenameUtils.getExtension(basePath.getFileName().toString());
		if (Files.isDirectory(basePath)) {
			type = IMAGE_SET;
			try {
				description = new String(Files.readAllBytes(basePath.resolve("info.txt")), "UTF-8");
			} catch (IOException e) {
				// info.txt not found: do nothing
			}
		} else if (Files.isReadable(basePath) && Arrays.asList(ImageIO.getReaderFileSuffixes()).contains(extension)) {
			type = SINGLE_IMAGE;
		} else {
			type = INVALID;
		}
	}
	
	Skin(Path path) {
		this(path, false);
	}
	
	@Override
	public String toString() {
		return basePath.getFileName().toString() + " [" + typeToString(type) + "]";
	}
	
	String getName() {
		return basePath.getFileName().toString();
	}
	
	Path getBasePath() {
		return basePath;
	}
	
	int getType() {
		return type;
	}
	
	String getDescription() {
		return description;
	}
	
	Image getImage(String name) {
		switch (type) {
			case SINGLE_IMAGE:
				try {
					return ImageIO.read(Files.newInputStream(basePath));
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case IMAGE_SET:
				Path imagePath = basePath.resolve(name + ".png");
				if (Files.isDirectory(imagePath)) {
					try {
						DirectoryStream<Path> directoryStream = Files.newDirectoryStream(imagePath, "*.png");
						List<Path> variants = new ArrayList<>();
						for (Path path : directoryStream) {
							variants.add(path);
						}
						int i = (int) Math.floor(Math.random() * variants.size());
						imagePath = variants.get(i);
						return ImageIO.read(Files.newInputStream(imagePath));
					} catch (Throwable e) {
						e.printStackTrace();
					}
				} else if (Files.isReadable(imagePath)) {
					try {
						return ImageIO.read(Files.newInputStream(imagePath));
					} catch (Throwable e) {
						e.printStackTrace();
					}
				} else if (!name.equals("normal")) {
					return getImage("normal");
				}
		}
		return null;
	}
	
	boolean isBuiltin() {
		return builtin;
	}
	
	@Override
	public int compareTo(Skin skin) {
		return toString().compareTo(skin.toString());
	}
	
	static String typeToString(int type) {
		switch (type) {
			case SINGLE_IMAGE:
				return "SINGLE IMAGE";
			case IMAGE_SET:
				return "IMAGE SET";
		}
		return "INVALID";
	}
	
}
