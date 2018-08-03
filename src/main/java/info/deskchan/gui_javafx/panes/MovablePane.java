package info.deskchan.gui_javafx.panes;

import info.deskchan.gui_javafx.Main;
import info.deskchan.gui_javafx.OverlayStage;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;

import java.util.Map;

public class MovablePane extends Pane {
	
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

	protected MovablePane(){
		setOnMousePressed(pressEventHandler);
	}
	protected boolean positionRelativeToDesktopSize = true;

	public boolean isLongClick(){
		return (System.currentTimeMillis()-lastClick)>500;
	}

	private static Rectangle2D anchorToEdges(Rectangle2D bounds, Rectangle2D screenBounds) {
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

	public void setPosition(Point2D topLeft) {
		if (topLeft == null) {
			setDefaultPosition();
			return;
		}
		Bounds bounds = getLayoutBounds();
		Rectangle2D rect = new Rectangle2D(topLeft.getX(), topLeft.getY(),
				bounds.getWidth(), bounds.getHeight());

		// Anchoring to screen edges if pane is too hide to edge
		for (Screen screen : Screen.getScreens()) {
			rect = anchorToEdges(rect, screen.getBounds());
			rect = anchorToEdges(rect, screen.getVisualBounds());
		}
		relocate(rect.getMinX(), rect.getMinY());
	}

	public Point2D getPosition() {
		Bounds bounds = getLayoutBounds();
		try {
			Bounds local = localToScreen(bounds);
			if (local != null) bounds = local;
		} catch(Exception e){ }
		Point2D position = new Point2D(bounds.getMinX(), bounds.getMinY());
		return position;
	}

	public void relocate(Map<String, Number> data){
		Point2D pos = getPosition();
		Rectangle2D screen = OverlayStage.getDesktopSize();
		for (Map.Entry<String, Number> entry : data.entrySet()){
			pos = relocateByKey(pos, screen, entry.getKey(), entry.getValue().intValue());
		}
		setPosition(pos);
		storePositionToStorage();
	}

	public void relocate(String key, int value){
		Point2D pos = relocateByKey(getPosition(), OverlayStage.getDesktopSize(), key, value);
		setPosition(pos);
		storePositionToStorage();
	}

	private Point2D relocateByKey(Point2D pos, Rectangle2D screen, String key, int value){
		switch (key){
			case "top": return new Point2D(pos.getX(), value);
			case "left": return new Point2D(value, pos.getY());
			case "right": return new Point2D(screen.getMaxX() - getWidth() - value, pos.getY());
			case "bottom": return new Point2D(pos.getX(), screen.getMaxY() - getHeight() - value);
			case "verticalPercent": return new Point2D(pos.getX(), (screen.getMaxY() - getHeight()) * value / 100);
			case "horizontalPercent": return new Point2D((screen.getMaxX() - getWidth()) * value / 100, pos.getY());
		}
		return pos;
	}

	public void relocate(double x, double y) {
		if(OverlayStage.getInstance() != null && getParent() != null)
			OverlayStage.getInstance().relocate(this, x, y);
		else {
			super.relocate(x,y);
		}
	}
	public void setDefaultPosition() {
		setPosition(new Point2D(0, 0));
	}

	public void show() {
		OverlayStage.getInstance().showSprite(this);
	}

	public void hide() {
		OverlayStage.getInstance().hideSprite(this);
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
		try {
			loadPositionFromStorage();
		} catch (Exception e){
			setDefaultPosition();
		}
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
	
	public void loadPositionFromStorage() {
		try {
			final String key = getCurrentPositionStorageKey();
			if (key != null) {
				final String value = Main.getProperties().getString(key);
				if (value != null) {
					String[] coords = value.split(";");
					if (coords.length == 2) {
						double x = Double.parseDouble(coords[0]);
						double y = Double.parseDouble(coords[1]);
						Rectangle2D desktop = OverlayStage.getDesktopSize();
						if(desktop.getWidth() == 0 || desktop.getHeight() == 0) return;

						// fix if character position is outside the screen
						if (x + getHeight() > desktop.getMaxX() || x < -getHeight())
							x = desktop.getMaxX() - getHeight();
						if (y + getWidth() > desktop.getMaxY() || y < -getWidth())
							y = desktop.getMaxY() - getWidth();

						setPosition(new Point2D(x, y));
						return;
					}
				}
			}
		} catch (Exception e){ Main.log(e); }
		setDefaultPosition();
	}

	public void storePositionToStorage() {
		final String key = getCurrentPositionStorageKey();
		if (key != null) {
			Point2D pos = getPosition();
			Main.getProperties().put(key, pos.getX() + ";" + pos.getY());
		}
	}


}
