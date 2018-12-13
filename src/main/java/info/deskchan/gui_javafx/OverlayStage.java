package info.deskchan.gui_javafx;

import info.deskchan.gui_javafx.panes.Balloon;
import info.deskchan.gui_javafx.panes.CharacterBalloon;
import info.deskchan.gui_javafx.panes.MovablePane;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Pair;
import org.apache.commons.lang3.SystemUtils;

import java.util.*;

import static info.deskchan.gui_javafx.OverlayStage.LayerMode.ALWAYS_TOP;
import static info.deskchan.gui_javafx.OverlayStage.LayerMode.SEPARATE;

public abstract class OverlayStage extends Stage {

	public enum LayerMode {
		BROKEN,
		ALWAYS_NORMAL,
		TOP_IF_MESSAGE,
		ALWAYS_TOP,
		ONLY_BALLOON,
		HIDE,
		SHOW_IF_MESSAGE,
		SEPARATE
	}

	protected final OverlayStage stage;

	private static Map<LayerMode, Class> instances = new HashMap<>();
	protected static OverlayStage instance = null;
	private static LayerMode currentMode;

	protected Group root = new Group();
	protected Scene scene = new Scene(root);

	public static void initialize(){
		try {
			instances.put(ALWAYS_TOP, TopStage.class);
		} catch(Exception e){
			Main.log("Cannot initialize ALWAYS_TOP stage");
		}
		try {
			instances.put(LayerMode.ALWAYS_NORMAL, NormalStage.class);
		} catch(Exception e){
			Main.log("Cannot initialize ALWAYS_NORMAL stage");
		}
		try {
			instances.put(LayerMode.TOP_IF_MESSAGE, FrontNormalStage.class);
		} catch(Exception e){
			Main.log("Cannot initialize TOP_IF_MESSAGE stage");
		}
		try {
			instances.put(LayerMode.HIDE, HideStage.class);
		} catch(Exception e){
			Main.log("Cannot initialize HIDE stage");
		}
		try {
			instances.put(LayerMode.SHOW_IF_MESSAGE, ShowIfMessageStage.class);
		} catch(Exception e){
			Main.log("Cannot initialize SHOW_IF_MESSAGE stage");
		}
		try {
			instances.put(SEPARATE, SeparateStage.class);
		} catch(Exception e){
			Main.log("Cannot initialize SEPARATE stage");
		}
		try {
			instances.put(LayerMode.ONLY_BALLOON, OnlyBalloonStage.class);
		} catch(Exception e){
			Main.log("Cannot initialize SEPARATE stage");
		}
	}

	public static void updateStage(){
		LayerMode mode;

		if(SystemUtils.IS_OS_MAC) mode = SEPARATE;
		else mode = ALWAYS_TOP;

		updateStage(Main.getProperties().getOneOf("character.layer_mode", LayerMode.values(), mode));
	}

	private static boolean showConfirmation(String text){
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		try {
			((Stage) alert.getDialogPane().getScene().getWindow()).setAlwaysOnTop(true);
		} catch (Exception e){ }
		alert.setTitle(Main.getString("default_messagebox_name"));
		alert.setContentText(Main.getString(text));
		Optional<ButtonType> result = alert.showAndWait();
		return (result.get() != ButtonType.OK);
	}

	public static void updateStage(LayerMode mode){
		if(mode == SEPARATE && !SystemUtils.IS_OS_MAC && instance != null)
			if (showConfirmation("info.separated-stage")) return;
		else if(mode == ALWAYS_TOP && SystemUtils.IS_OS_MAC)
			if (showConfirmation("info.separated-stage")) return;

		if(mode == currentMode) return;
		try {
			OverlayStage nextInstance = (OverlayStage) instances.get(mode).newInstance();
			List<Pair<MovablePane, Point2D>> nodesOnScreen = new LinkedList<>();
			if(instance != null) {
				for(Node node : instance.root.getChildren()){
					try {
						MovablePane pane = (MovablePane) node;
						nodesOnScreen.add(new Pair<>(pane, pane.getPosition()));
					} catch (Exception e){ }
				}
				instance.hideAllSprites();
				instance.close();
			}
			instance = nextInstance;
			instance.getScene().getStylesheets().add(App.getStylesheet());
			instance.focusedProperty().addListener((observable, oldValue, newValue) -> {
				if (!newValue){
					ContextMenu contextMenu = Menu.getInstance().getContextMenu();
					contextMenu.hide();
				}
			});
			nextInstance.showStage();
			nextInstance.showSprite(App.getInstance().character);
			for (Pair<MovablePane, Point2D> pane : nodesOnScreen){
				nextInstance.showSprite(pane.getKey());
			}
		} catch (Exception e){
			Main.log(e);
			currentMode = LayerMode.BROKEN;
			return;
		}
		currentMode = mode;
		instance.toFront();
		if (mode != LayerMode.HIDE)
			Main.getProperties().put("character.layer_mode", mode);
	}

	public static LayerMode getCurrentStage(){
		return currentMode;
	}

	public static OverlayStage getInstance(){
		return instance;
	}

	public static Set<LayerMode> getStages(){
		return instances.keySet();
	}

	OverlayStage() {
		stage = this;
		setTitle(App.NAME);
		getIcons().add(new Image(App.ICON_URL.toString()));
		initStyle(StageStyle.TRANSPARENT);
		scene.setFill(Color.TRANSPARENT);
		setScene(scene);

		// All nodes position automatically changed when window position change
		// So nodes will save their absolute position on screen
		xProperty().addListener(onPositionChange);
		yProperty().addListener(onPositionChange);
	}

	public static Rectangle2D getDesktopSize() {
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

	protected void resizeToDesktopSize() {
		Rectangle2D rect = getDesktopSize();
		setX(rect.getMinX());
		setY(rect.getMinY());
		setWidth(rect.getWidth());
		setHeight(rect.getHeight());
	}

	public abstract void showSprite(MovablePane pane);
	public abstract void hideSprite(MovablePane pane);
	public abstract void hideAllSprites();

	public void relocate(Node node, double x, double y){
		node.setLayoutX(x - this.getX());
		node.setLayoutY(y - this.getY());
	}

	public void showStage(){
		show();
	}

	public void setAlwaysOnTop(){
		try {
			setAlwaysOnTop(true);
		} catch(Throwable e){
			Main.log("Sorry, top stage is not available on your system. Maybe it's because you didn't update Java.");
		}
	}

	protected javafx.beans.value.ChangeListener<Number> onPositionChange = new javafx.beans.value.ChangeListener<Number>() {
		@Override
		public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
			for (Node node : root.getChildren())
				relocate(node, node.getLayoutX() - scene.getX(), node.getLayoutY() - scene.getY());
		}
	};

	public static void sendToFront(){
		instance.toFront();
		instance.requestFocus();
	}

	public boolean isCharacterVisible(){
		return true;
	}
}
class NormalStage extends OverlayStage {
	EventHandler<WindowEvent> handler = new EventHandler<WindowEvent>() {
		@Override
		public void handle(WindowEvent event) {
			update();
		}
	};
	protected synchronized void update(){
		HackJavaFX.setCreateTransparentPopup(stage);
	}
	NormalStage(){
		super();
		setOnShowing(handler);
		setOnHiding(handler);
		resizeToDesktopSize();
		Screen.getScreens().addListener((ListChangeListener<Screen>) change -> {
			resizeToDesktopSize();
			Rectangle2D rect = getDesktopSize();
			Main.log("Desktop rect is " + rect.getMinX() + "," + rect.getMinY() +
					"," + rect.getMaxX() + "," + rect.getMaxY());
		});
	}

	@Override
	public void showSprite(MovablePane pane){
		if(root.getChildren().contains(pane)) return;

		root.getChildren().add(pane);
		pane.loadPositionFromStorage();
		update();
	}

	@Override
	public void hideSprite(MovablePane pane){
		root.getChildren().remove(pane);
	}

	@Override
	public synchronized void hideAllSprites(){
		root.getChildren().clear();
	}
}

class TopStage extends NormalStage {
	TopStage(){
		super();
		toFront();
		setOnShowing(handler);
		setOnHiding(handler);
	}
	@Override
	protected synchronized void update(){
		try {
			HackJavaFX.setCreateTransparentPopup(stage);
			HackJavaFX.setWindowFocusable(stage, false);
			toFront();
			requestFocus();
			setAlwaysOnTop();
		} catch (Exception e){
			Main.log("Cannot handle top stage change");
		}
	}
}

class OnlyBalloonStage extends TopStage {
	OnlyBalloonStage(){
		super();
		CharacterBalloon.setDefaultPositionMode(CharacterBalloon.PositionMode.ABSOLUTE);
	}
	@Override
	public void showSprite(MovablePane sprite){
		if (sprite instanceof Balloon)
			super.showSprite(sprite);
	}
	public boolean isCharacterVisible(){
		return false;
	}
}

class FrontNormalStage extends NormalStage {
	FrontNormalStage(){
		super();
	}
	@Override
	public void showSprite(MovablePane sprite){
		super.showSprite(sprite);
		if (sprite instanceof Balloon) {
			toFront();
			requestFocus();
		}
	}
}

class HideStage extends NormalStage {
	HideStage(){
		super();
		close();
	}
	@Override
	public void showStage(){ }
	public boolean isCharacterVisible(){
		return false;
	}
}

class ShowIfMessageStage extends TopStage {
	@Override
	public void showSprite(MovablePane sprite){
		super.showSprite(sprite);
		if (sprite instanceof Balloon) {
			show();
		}
	}
	@Override
	public void hideSprite(MovablePane sprite){
		super.hideSprite(sprite);
		for(Node node : root.getChildren()){
			if (node instanceof Balloon) return;
		}
		hide();
	}
	@Override
	public void showStage(){
		show();
		for(Node node : root.getChildren()){
			if (node instanceof Balloon) return;
		}
		hide();
	}
	public boolean isCharacterVisible(){
		return isShowing();
	}
}

class SeparatedStage extends OverlayStage {
	final Node node;
	EventHandler<MouseEvent> startDragHandler = new EventHandler<MouseEvent>() {
		@Override
		public synchronized void handle(MouseEvent event) {
			javafx.application.Platform.runLater(() -> {
				if(dragging || !event.getButton().equals(MouseButton.PRIMARY)) return;
				dragging = true;
				node.setLayoutX(stage.getX());
				node.setLayoutY(stage.getY());
				resizeToDesktopSize();
			});
		}
	};
	EventHandler<MouseEvent> stopDragHandler = new EventHandler<MouseEvent>() {
		@Override
		public synchronized void handle(MouseEvent event) {
			javafx.application.Platform.runLater(() -> {
				if(!dragging || !event.getButton().equals(MouseButton.PRIMARY)) return;
				if(!root.getChildren().contains(node)){
					close();
					return;
				}
				Bounds bounds = node.getLayoutBounds();
				stage.setHeight(bounds.getHeight()+5);
				stage.setWidth(bounds.getWidth()+5);
				dragging = false;
				relocate(node.getLayoutX(),node.getLayoutY());
			});
		}
	};
	EventHandler<WindowEvent> showHandler = new EventHandler<WindowEvent>() {
		@Override
		public void handle(WindowEvent event) {
			HackJavaFX.setCreateTransparentPopup(stage);
			HackJavaFX.setWindowFocusable(stage, false);
		}
	};
	Point2D position;
	private boolean dragging = false;
	SeparatedStage(Node node){
		super();
		position = new Point2D(node.getLayoutX(),node.getLayoutY());
		root.getChildren().add(node);
		//scene.setFill(Color.WHITE);
		this.node = node;
		setOnShowing(showHandler);
		setOnHiding(showHandler);
		root.setOnMouseDragged(startDragHandler);
		root.setOnMouseExited(stopDragHandler);
		root.setOnMouseReleased(stopDragHandler);
		setAlwaysOnTop();
		try {
			node.setLayoutX(0);
			node.setLayoutY(0);
		} catch (Exception e){ }
		show();
		stage.setX(position.getX());
		stage.setY(position.getY());
		Bounds bounds = node.getLayoutBounds();
		stage.setHeight(bounds.getHeight()+5);
		stage.setWidth(bounds.getWidth()+5);

	}
	void relocate(double x, double y){
		if(dragging){
			super.relocate(node, x, y);
		} else {
			setX(x);
			setY(y);
			node.setLayoutX(0);
			node.setLayoutY(0);
		}
	}

	public void showSprite(MovablePane pane){ throw new RuntimeException("This method should not be called"); }
	public void hideSprite(MovablePane pane){ throw new RuntimeException("This method should not be called"); }
	public void hideAllSprites(){ throw new RuntimeException("This method should not be called"); }
}

class SeparateStage extends OverlayStage {
	private Map<MovablePane, SeparatedStage> children = new HashMap<>();
	SeparateStage(){
		super();
		close();
	}
	@Override
	public void showSprite(MovablePane pane){
		if(!children.containsKey(pane))
			children.put(pane, new SeparatedStage(pane));
	}
	@Override
	public void hideSprite(MovablePane pane){
		SeparatedStage s = children.get(pane);
		if(s == null) return;
		s.root.getChildren().remove(pane);
		children.remove(pane);
		s.hide();
		s.close();
		s.getScene().getWindow().hide();
	}
	@Override
	public void hideAllSprites(){
		throw new RuntimeException("This method should not be called");
	}

	public void relocate(MovablePane pane, double x, double y){
		if(new Double(x).isNaN()) x = 0;
		if(new Double(y).isNaN()) y = 0;
		showSprite(pane);
		children.get(pane).relocate(x, y);
	}
}