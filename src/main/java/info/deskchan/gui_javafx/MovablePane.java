package info.deskchan.gui_javafx;

import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;

class MovablePane extends Pane {
	
	private static final int SNAP_DISTANCE = 10;

	private String positionStorageID = null;
	private Point2D clickPos = null;
	private Point2D dragPos = null;
	private EventHandler<MouseEvent> dragEventHandler = (e) -> {
		Point2D newClickPos = new Point2D(e.getSceneX(), e.getSceneY());
		Point2D delta = newClickPos.subtract(clickPos);
		if (dragPos != null) {
			dragPos = dragPos.add(delta);
			setPosition(dragPos);
		}
		clickPos = newClickPos;
	};
	private EventHandler<MouseEvent> releaseEventHandler = (e) -> stopDrag();
	
	private static Rectangle2D snapRect(Rectangle2D bounds, Rectangle2D screenBounds) {
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
		setPosition(new Point2D(0, 0));
	}
	
	void startDrag(MouseEvent event) {
		if (clickPos != null) {
			return;
		}
		clickPos = new Point2D(event.getSceneX(), event.getSceneY());
		dragPos = new Point2D(getLayoutX(), getLayoutY());
		addEventFilter(MouseEvent.MOUSE_RELEASED, releaseEventHandler);
		addEventFilter(MouseEvent.MOUSE_DRAGGED, dragEventHandler);
	}
	
	void stopDrag() {
		if (clickPos == null) {
			return;
		}
		removeEventFilter(MouseEvent.MOUSE_RELEASED, releaseEventHandler);
		removeEventFilter(MouseEvent.MOUSE_DRAGGED, dragEventHandler);
		dragPos = null;
		clickPos = null;
		if (positionStorageID != null) {
			storePositionToStorage();
		}
	}
	
	boolean isDragging() {
		return dragPos != null;
	}
	
	String getPositionStorageID() {
		return positionStorageID;
	}
	
	void setPositionStorageID(String id) {
		assert positionStorageID == null;
		positionStorageID = id;
		loadPositionFromStorage();
		Screen.getScreens().addListener((ListChangeListener<Screen>) change -> loadPositionFromStorage());
	}
	
	private String getCurrentPositionStorageKey() {
		final StringBuilder key = new StringBuilder();
		final Rectangle2D desktopSize = OverlayStage.getDesktopSize();
		key.append(positionStorageID);
		key.append('.');
		key.append(desktopSize.getMinX());
		key.append('_');
		key.append(desktopSize.getMinY());
		key.append('_');
		key.append(desktopSize.getWidth());
		key.append('_');
		key.append(desktopSize.getHeight());
		return key.toString();
	}
	
	private void loadPositionFromStorage() {
		final String key = getCurrentPositionStorageKey();
		final String value = Main.getProperty(key, null);
		if (value != null) {
			String[] coords = value.split(";");
			if (coords.length == 2) {
				double x = Double.parseDouble(coords[0]);
				double y = Double.parseDouble(coords[1]);
				setPosition(new Point2D(x, y));
				return;
			}
		}
		setDefaultPosition();
	}
	
	private void storePositionToStorage() {
		final String key = getCurrentPositionStorageKey();
		Main.setProperty(key, getLayoutX() + ";" + getLayoutY());
	}
	
}
