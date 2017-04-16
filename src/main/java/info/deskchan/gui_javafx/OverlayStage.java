package info.deskchan.gui_javafx;

import javafx.collections.ListChangeListener;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.HashMap;
import java.util.Map;

class OverlayStage extends Stage {
	
	private static Map<String, OverlayStage> instances = new HashMap<>();
	private Group root = new Group();
	private Scene scene = new Scene(root);
	
	OverlayStage(String name) {
		setTitle(App.NAME);
		getIcons().add(new Image(App.ICON_URL.toString()));
		initStyle(StageStyle.TRANSPARENT);
		setOnCloseRequest(event -> Main.getInstance().quit());
		scene.setFill(Color.TRANSPARENT);
		setScene(scene);
		resizeToDesktopSize();
		Screen.getScreens().addListener((ListChangeListener<Screen>) change -> resizeToDesktopSize());
		instances.put(name, this);
	}
	
	static OverlayStage getInstance(String name) {
		return instances.getOrDefault(name, null);
	}
	
	Group getRoot() {
		return root;
	}
	
	private static Rectangle2D getDesktopSize() {
		Rectangle2D rect = Screen.getPrimary().getBounds();
		double minX = rect.getMinX(), minY = rect.getMinY();
		double maxX = rect.getMaxX(), maxY = rect.getMaxY();
		for (Screen screen : Screen.getScreens()) {
			Rectangle2D screenRect = screen.getBounds();
			if (minX > screenRect.getMinX()) {
				minX = screenRect.getMinX();
			}
			if (minY > screenRect.getMinY()) {
				minY = screenRect.getMinY();
			}
			if (maxX < screenRect.getMaxX()) {
				maxX = screenRect.getMaxX();
			}
			if (maxY < screenRect.getMaxY()) {
				maxY = screenRect.getMaxY();
			}
		}
		return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
	}
	
	private void resizeToDesktopSize() {
		Rectangle2D rect = getDesktopSize();
		Main.log("Desktop rect is " + rect.getMinX() + "," + rect.getMinY() +
				"," + rect.getMaxX() + "," + rect.getMaxY());
		setX(rect.getMinX());
		setY(rect.getMinY());
		setWidth(rect.getWidth());
		setHeight(rect.getHeight());
	}
	
	@Override
	protected void impl_visibleChanging(boolean value) {
		HackJavaFX.setCreateTransparentPopup(this);
		super.impl_visibleChanging(value);
		HackJavaFX.setWindowFocusable(this, false);
	}
	
}
