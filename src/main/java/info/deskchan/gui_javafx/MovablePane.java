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
	private long lastClick = -1;
	private EventHandler<MouseEvent> pressEventHandler = (e) -> {
		lastClick = System.currentTimeMillis();
	};
	private EventHandler<MouseEvent> dragEventHandler = (e) -> {
		Point2D newClickPos = new Point2D(e.getScreenX(), e.getScreenY());
		Point2D delta = newClickPos.subtract(clickPos);
		if (dragPos != null) {
			dragPos = dragPos.add(delta);
			setPosition(dragPos);
		}
		clickPos = newClickPos;

	};
	private EventHandler<MouseEvent> releaseEventHandler = (e) -> stopDrag();

	MovablePane(){
		setOnMousePressed(pressEventHandler);
	}
	protected boolean positionRelativeToDesktopSize = true;

	public boolean isLongClick(){
		return (System.currentTimeMillis()-lastClick)>500;
	}

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
		if (topLeft == null) {
			setDefaultPosition();
			return;
		}
		Bounds bounds = getLayoutBounds();
		Rectangle2D rect = new Rectangle2D(topLeft.getX(), topLeft.getY(),
				bounds.getWidth(), bounds.getHeight());
		for (Screen screen : Screen.getScreens()) {
			rect = snapRect(rect, screen.getBounds());
			rect = snapRect(rect, screen.getVisualBounds());
		}
		relocate(rect.getMinX(), rect.getMinY());
	}

	Point2D getPosition() {
		Bounds bounds = getLayoutBounds();
		Bounds local = localToScreen(bounds);
		if(local==null) local = bounds;
		Point2D position = new Point2D(local.getMinX(), local.getMinY());
		return position;
	}

	public void relocate(double x, double y) {
		if(OverlayStage.getInstance()!=null)
			OverlayStage.getInstance().relocate(this, x, y);
		else super.relocate(x,y);
	}
	void setDefaultPosition() {
		setPosition(new Point2D(0, 0));
	}
	
	void startDrag(MouseEvent event) {
		if (clickPos != null) {
			return;
		}
		clickPos = new Point2D(event.getScreenX(), event.getScreenY());
		dragPos = getPosition();
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
		storePositionToStorage();
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
		if (positionRelativeToDesktopSize) {
			Screen.getScreens().addListener((ListChangeListener<Screen>) change -> loadPositionFromStorage());
		}
	}
	
	private String getCurrentPositionStorageKey() {
		if (positionStorageID == null) {
			return null;
		}
		final StringBuilder key = new StringBuilder();
		final Rectangle2D desktopSize = OverlayStage.getDesktopSize();
		key.append(positionStorageID);
		if (positionRelativeToDesktopSize) {
			key.append('.');
			key.append(desktopSize.getMinX());
			key.append('_');
			key.append(desktopSize.getMinY());
			key.append('_');
			key.append(desktopSize.getWidth());
			key.append('_');
			key.append(desktopSize.getHeight());
		}
		return key.toString();
	}
	
	protected void loadPositionFromStorage() {
		try {
			final String key = getCurrentPositionStorageKey();
			if (key != null) {
				final String value = Main.getProperty(key, null);
				if (value != null) {
					String[] coords = value.split(";");
					if (coords.length == 2) {
						double x = Double.parseDouble(coords[0]);
						double y = Double.parseDouble(coords[1]);
						Rectangle2D desktop = OverlayStage.getDesktopSize();
						if (x + getHeight() > desktop.getMaxX() || x < -getHeight())
							x = desktop.getMaxX() - getHeight();
						if (y + getWidth() > desktop.getMaxY() || y < -getWidth()) y = desktop.getMaxY() - getWidth();
						setPosition(new Point2D(x, y));
						return;
					}
				}
			}
		} catch (Exception e){ Main.log(e); }
		setDefaultPosition();
	}

	protected void storePositionToStorage() {
		final String key = getCurrentPositionStorageKey();
		if (key != null) {
			Point2D pos = getPosition();
			Main.setProperty(key, pos.getX() + ";" + pos.getY());
		}
	}


}
