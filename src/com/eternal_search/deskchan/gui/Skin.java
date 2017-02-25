package com.eternal_search.deskchan.gui;

import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Skin implements Comparable<Skin> {
	
	static final int INVALID = 0;
	static final int SINGLE_IMAGE = 1;
	static final int IMAGE_SET = 2;
	
	private final Path basePath;
	private final int type;
	private final boolean builtin;
	
	Skin(Path path, boolean builtin) {
		this.builtin = builtin;
		basePath = path;
		if (basePath == null) {
			type = INVALID;
			return;
		}
		final String extension = FilenameUtils.getExtension(basePath.getFileName().toString());
		if (Files.isDirectory(basePath)) {
			type = IMAGE_SET;
		} else if (Files.isReadable(basePath) && Arrays.asList(ImageIO.getReaderFileSuffixes()).contains(extension)) {
			type = SINGLE_IMAGE;
		} else {
			type = INVALID;
		}
	}
	
	Skin(Path path) {
		this(path, false);
	}
	
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
				if (Files.isReadable(imagePath)) {
					try {
						return ImageIO.read(imagePath.toFile());
					} catch (IOException e) {
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
