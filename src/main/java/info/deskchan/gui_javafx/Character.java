package info.deskchan.gui_javafx;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;

import java.util.Map;
import java.util.PriorityQueue;

class Character extends Pane {
	
	private static final String DEFAULT_SKIN_NAME = "illia";
	
	enum LayerMode {
		ALWAYS_NORMAL,
		TOP_IF_MESSAGE,
		ALWAYS_TOP
	}
	
	private static final int SNAP_DISTANCE = 10;
	private static final int DEFAULT_MESSAGE_PRIORITY = 1000;
	
	private ImageView imageView = new ImageView();
	private Skin skin = null;
	private String imageName = "normal";
	private Point2D clickPos = null;
	private Point2D dragPos = null;
	private String idleImageName = "normal";
	private PriorityQueue<MessageInfo> messageQueue = new PriorityQueue<>();
	private Balloon balloon = null;
	private String layerName = "top";
	private LayerMode layerMode = LayerMode.ALWAYS_TOP;
	
	Character(Skin skin) {
		getChildren().add(imageView);
		setSkin(skin);
		initEventFilters();
		setDefaultPosition();
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
	
	private Rectangle2D snapRect(Rectangle2D bounds, Rectangle2D screenBounds) {
		double x1 = bounds.getMinX(), y1 = bounds.getMinY();
		double x2 = bounds.getMaxX(), y2 = bounds.getMaxY();
		if (Math.abs(x1 - screenBounds.getMinX()) < SNAP_DISTANCE) {
			x1 = screenBounds.getMinX();
		}
		if (Math.abs(y1 - screenBounds.getMinY()) < SNAP_DISTANCE) {
			y1 = screenBounds.getMinY();
		}
		if (Math.abs(x2 - screenBounds.getMaxX()) < SNAP_DISTANCE) {
			x1 = screenBounds.getMaxX() - bounds.getWidth();
		}
		if (Math.abs(y2 - screenBounds.getMaxY()) < SNAP_DISTANCE) {
			y1 = screenBounds.getMaxY() - bounds.getHeight();
		}
		return new Rectangle2D(x1, y1, bounds.getWidth(), bounds.getHeight());
	}
	
	void setPosition(Point2D topLeft) {
		Bounds bounds = getLayoutBounds();
		Rectangle2D rect = new Rectangle2D(topLeft.getX(), topLeft.getY(),
				bounds.getWidth(), bounds.getHeight());
		for (Screen screen : Screen.getScreens()) {
			rect = snapRect(rect, screen.getBounds());
			rect = snapRect(rect, screen.getVisualBounds());
		}
		relocate(rect.getMinX(), rect.getMinY());
	}
	
	void setDefaultPosition() {
		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
		setPosition(new Point2D(screenBounds.getMaxX() - getWidth(),
				screenBounds.getMaxY() - getHeight()));
	}
	
	private void updateImage() {
		Image image = getImage();
		imageView.setImage(image);
		if (image != null) {
			resize(image.getWidth(), image.getHeight());
		}
	}
	
	private void initEventFilters() {
		addEventFilter(MouseEvent.MOUSE_PRESSED, (event) -> {
			clickPos = new Point2D(event.getSceneX(), event.getSceneY());
			dragPos = new Point2D(getLayoutX(), getLayoutY());
		});
		addEventFilter(MouseEvent.MOUSE_RELEASED, (event) -> {
			dragPos = null;
			clickPos = null;
		});
		addEventFilter(MouseEvent.MOUSE_DRAGGED, (event) -> {
			Point2D newClickPos = new Point2D(event.getSceneX(), event.getSceneY());
			Point2D delta = newClickPos.subtract(clickPos);
			if (dragPos != null) {
				dragPos = dragPos.add(delta);
				setPosition(dragPos);
			}
			clickPos = newClickPos;
		});
	}
	
	void setIdleImageName(String name) {
		idleImageName = name;
		setImageName(name);
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
			balloon = new Balloon(this, messageInfo.text);
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
