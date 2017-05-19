package info.deskchan.gui_javafx;

import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseButton;
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
		setScaleFactor(Float.parseFloat(Main.getProperty("skin.scale_factor", "1.0")));
		getChildren().add(imageView);
		setSkin(skin);
		setPositionStorageID("character." + id);
		balloonPositionMode = Balloon.PositionMode.valueOf(
				Main.getProperty("character." + id + ".balloon_position_mode",
						Balloon.PositionMode.AUTO.toString())
		);
		addEventFilter(MouseEvent.MOUSE_PRESSED, this::startDrag);
		addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
			boolean enabled = Main.getProperty("character.enable_context_menu", "0").equals("1");
			ContextMenu contextMenu = App.getInstance().getContextMenu();
			if (enabled && event.getButton() == MouseButton.SECONDARY && event.isStillSincePress()) {
				// If we don't hide the menu manually, we get it with incorrect width.
				contextMenu.hide();
				contextMenu.show(this, event.getScreenX(), event.getScreenY());
			} else if (contextMenu.isShowing()) {
				contextMenu.hide();
			}
		});

		MouseEventNotificator mouseEventNotificator = new MouseEventNotificator(this, "character");
		mouseEventNotificator
				.setOnClickListener()
				.setOnScrollListener(event -> {
					Point2D characterPosition = getPosition();
					double charX = characterPosition.getX();
					double charY = characterPosition.getY();
					double x = event.getX();
					double y = event.getY();
					double x0 = x - charX;
					double y0 = y - charY;

					// just in case
					if (scaleFactor == 0) {
						Main.log("The scale factor of the image is equal to zero!");
						return false;
					}
					x0 /= scaleFactor;
					y0 /= scaleFactor;

					PixelReader imagePixels = imageView.getImage().getPixelReader();
					Color pixelColor = imagePixels.getColor((int) x0, (int) y0);

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
	
	private Image getImage() {
		return (skin != null) ? skin.getImage(imageName) : null;
	}
	
	@Override
	void setDefaultPosition() {
		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
		setPosition(new Point2D(screenBounds.getMaxX() - getWidth(),
				screenBounds.getMaxY() - getHeight()));
	}
	
	private void updateImage(boolean reloadImage) {
	    if (reloadImage) {
            imageView.setImage(getImage());
        }

        Image image = imageView.getImage();
        double oldWidth = imageView.getFitWidth();
        double oldHeight = imageView.getFitHeight();
        double newWidth = image.getWidth() * scaleFactor;
        double newHeight = image.getHeight() * scaleFactor;

        imageView.setFitWidth(newWidth);
        imageView.setFitHeight(newHeight);
        resize(imageView.getFitWidth(), imageView.getFitHeight());

        Point2D oldPosition = getPosition();
        double deltaX = -(newWidth - oldWidth) / 2;
        double deltaY = -(newHeight - oldHeight) / 2;
        Point2D newPosition = new Point2D(oldPosition.getX() + deltaX, oldPosition.getY() + deltaY);
        setPosition(newPosition);
	}

	private void updateImage() {
	    updateImage(true);
    }

    /**
     * Scales the image to a given scale factor.
     * If it's greater than 1.0f, the image will be bigger.
     * Use a number in the range of (0.0; 1.0) to make the image smaller.
     * @param scaleFactor a positive float-point number
     */
	void resizeSprite(float scaleFactor) {
		if (scaleFactor == 0) {
			return;
		}
		setScaleFactor(scaleFactor);
		updateImage(false);
	}

    /**
     * Scales the image to either given width or height keeping its aspect ratio.
     * The width takes precedence over the height.
     * @param width nullable
     * @param height nullable
     */
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

    /**
     * Scales the image relatively. Unlike the usual resizeSprite(), this method
     * gets the old value of the scale factor and adds an increment to it.
     * Use a positive value to zoom in the image, or a negative one to zoom it out.
     * @param scaleFactorIncrement a positive or negative number
     */
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

    void setScaleFactor(float scaleFactor) {
		if (scaleFactor == 0) {
			this.scaleFactor = 1.0f;
		} else {
			this.scaleFactor = Math.round(Math.abs(scaleFactor) * 100.0f) / 100.0f;
		}
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
