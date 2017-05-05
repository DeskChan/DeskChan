package info.deskchan.gui_javafx;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;

import java.util.Map;
import java.util.PriorityQueue;

class Character extends MovablePane {
	
	private static final String DEFAULT_SKIN_NAME = "illia.image_set";
	
	enum LayerMode {
		ALWAYS_NORMAL,
		TOP_IF_MESSAGE,
		ALWAYS_TOP
	}
	
	private static final int DEFAULT_MESSAGE_PRIORITY = 1000;
	
	private final String id;
	private ImageView imageView = new ImageView();
	private Skin skin = null;
	private String imageName = "normal";
	private String idleImageName = "normal";
	private PriorityQueue<MessageInfo> messageQueue = new PriorityQueue<>();
	private Balloon balloon = null;
	private String layerName = "top";
	private LayerMode layerMode = LayerMode.ALWAYS_TOP;
	private Balloon.PositionMode balloonPositionMode;
	private float scaleFactor = 1.0f;
	
	Character(String id, Skin skin) {
		this.id = id;
		scaleFactor = Float.parseFloat(Main.getProperty("skin.scale_factor", "1.0"));
		getChildren().add(imageView);
		setSkin(skin);
		setPositionStorageID("character." + id);
		balloonPositionMode = Balloon.PositionMode.valueOf(
				Main.getProperty("character." + id + ".balloon_position_mode",
						Balloon.PositionMode.AUTO.toString())
		);
		addEventFilter(MouseEvent.MOUSE_PRESSED, this::startDrag);

		MouseEventNotificator mouseEventNotificator = new MouseEventNotificator(this, "character");
		mouseEventNotificator
				.setOnClickListener()
				.setOnScrollListener(event -> {
					Point2D characterPosition = getPosition();
					int charX = (int) characterPosition.getX();
					int charY = (int) characterPosition.getY();
					int x = event.getX();
					int y = event.getY();

					PixelReader imagePixels = getImage().getPixelReader();
					Color pixelColor = imagePixels.getColor(x - charX, y - charY);

					return !pixelColor.equals(Color.TRANSPARENT);
				});
	}
	
	Skin getSkin() {
		return skin;
	}
	
	void setSkin(Skin skin) {
		if (skin == null) {
			skin = Skin.load(DEFAULT_SKIN_NAME);
		}
		this.skin = skin;
		setImageName(imageName);
	}
	
	String getImageName() {
		return imageName;
	}
	
	void setImageName(String name) {
		imageName = ((name != null) && (name.length() > 0)) ? name : "normal";
		updateImage();
	}
	
	Image getImage() {
		return (skin != null) ? skin.getImage(imageName) : null;
	}
	
	@Override
	void setDefaultPosition() {
		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
		setPosition(new Point2D(screenBounds.getMaxX() - getWidth(),
				screenBounds.getMaxY() - getHeight()));
	}
	
	private void updateImage() {
		Image image = getImage();
		imageView.setImage(image);
		if (image != null) {
			imageView.setFitWidth(image.getWidth() * scaleFactor);
			imageView.setFitHeight(image.getHeight() * scaleFactor);
			resize(imageView.getFitWidth(), imageView.getFitHeight());
		}
	}

	void resizeSprite(float scaleFactor) {
		if (scaleFactor == 0) {
			return;
		}
		this.scaleFactor = Math.abs(scaleFactor);
		updateImage();
	}

	void resizeSprite(Integer width, Integer height) {
	    Double scaleFactor = null;
	    if (width != null) {
	        scaleFactor = width / imageView.getImage().getWidth();
        } else if (height != null) {
            scaleFactor = height / imageView.getImage().getHeight();
        } else {
	        return;
        }
        resizeSprite(scaleFactor.floatValue());
    }

	void resizeSpriteRelatively(float scaleFactorIncrement) {
		resizeSprite(scaleFactor + scaleFactorIncrement);
	}
	
	void setIdleImageName(String name) {
		idleImageName = name;
		setImageName(name);
	}
	
	Balloon.PositionMode getBalloonPositionMode() {
		return balloonPositionMode;
	}
	
	void setBalloonPositionMode(Balloon.PositionMode mode) {
		balloonPositionMode = mode;
		Main.setProperty("character." + id + ".balloon_position_mode", mode.toString());
	}
	
	void say(Map<String, Object> data) {
		MessageInfo messageInfo = null;
		if (data != null) {
			messageInfo = new MessageInfo(data);
			if ((messageInfo.priority <= 0) && (messageQueue.size() > 0)) {
				return;
			}
			messageQueue.add(messageInfo);
			if (messageQueue.peek() != messageInfo) {
				return;
			}
		} else {
			messageQueue.poll();
		}
		if (balloon != null) {
			balloon.close();
			balloon = null;
		}
		messageInfo = messageQueue.peek();
		if (messageInfo == null) {
			setImageName(idleImageName);
		} else {
			setImageName(messageInfo.characterImage);
			balloon = new Balloon(this, balloonPositionMode, messageInfo.text);
			balloon.setTimeout(messageInfo.timeout);
		}
		setLayerMode(layerMode);
	}
	
	LayerMode getLayerMode() {
		return layerMode;
	}
	
	void setLayerMode(LayerMode mode) {
		layerMode = mode;
		String newLayerName;
		if (mode.equals(LayerMode.ALWAYS_TOP)) {
			newLayerName = "top";
		} else if (mode.equals(LayerMode.TOP_IF_MESSAGE)) {
			newLayerName = (balloon != null) ? "top" : "normal";
		} else {
			newLayerName = "normal";
		}
		if (!layerName.equals(newLayerName)) {
			OverlayStage.getInstance(layerName).getRoot().getChildren().remove(this);
			layerName = newLayerName;
		}
		if (getParent() == null) {
			OverlayStage.getInstance(layerName).getRoot().getChildren().add(this);
		}
		if (balloon != null) {
			balloon.show(layerName);
		}
	}

	float getScaleFactor() {
	    return scaleFactor;
    }
	
	private static class MessageInfo implements Comparable<MessageInfo> {
		
		private final String text;
		private final String characterImage;
		private final int priority;
		private final int timeout;
		
		MessageInfo(Map<String, Object> data) {
			text = (String) data.getOrDefault("text", "");
			String characterImage = (String) data.getOrDefault("characterImage", null);
			if (characterImage != null) {
				characterImage = characterImage.toLowerCase();
			} else {
				characterImage = "normal";
			}
			this.characterImage = characterImage;
			priority = (Integer) data.getOrDefault("priority", DEFAULT_MESSAGE_PRIORITY);
			//timeout = (Integer) data.getOrDefault("timeout",
			//		Integer.parseInt(Main.getProperty("balloon.default_timeout", "15000")));
			timeout = (Integer) data.getOrDefault("timeout", Math.max(6000,
					text.length() * Integer.parseInt(Main.getProperty("balloon.default_timeout", "300"))));
		}
		
		@Override
		public int compareTo(MessageInfo messageInfo) {
			return -(priority - messageInfo.priority);
		}
		
	}
	
}
