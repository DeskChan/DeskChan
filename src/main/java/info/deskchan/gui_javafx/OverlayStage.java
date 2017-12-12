package info.deskchan.gui_javafx;

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
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.apache.commons.lang3.SystemUtils;

import java.util.*;

class OverlayStage extends Stage {

	public enum LayerMode {
		BROKEN,
		ALWAYS_NORMAL,
		TOP_IF_MESSAGE,
		ALWAYS_TOP,
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
			instances.put(LayerMode.ALWAYS_TOP, TopStage.class);
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
			instances.put(LayerMode.SEPARATE, SeparateStage.class);
		} catch(Exception e){
			Main.log("Cannot initialize SEPARATE stage");
		}
	}
	public static void updateStage(){
		String mode = Main.getProperties().getString("character.layer_mode");
		if(mode == null){
			if(SystemUtils.IS_OS_MAC) mode = "SEPARATE";
			else mode = "ALWAYS_TOP";
		}
		updateStage(mode);
	}
	public static void updateStage(String name){
		LayerMode mode;
		if (name == null){
			updateStage();
			return;
		}
		try {
			mode = LayerMode.valueOf(name);
		} catch (Exception e){
			Main.log("No such stage: "+name);
			return;
		}
		updateStage(mode);
	}
	private static void showConfirmation(String text){
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		try {
			((Stage) alert.getDialogPane().getScene().getWindow()).setAlwaysOnTop(true);
		} catch (Exception e){ }
		alert.setTitle(Main.getString("default_messagebox_name"));
		alert.setContentText(Main.getString(text));
		Optional<ButtonType> result = alert.showAndWait();
		if(result.get() != ButtonType.OK){
			return;
		}
	}
	public static void updateStage(LayerMode mode){
		if(mode == LayerMode.SEPARATE && !SystemUtils.IS_OS_MAC && instance != null)
			showConfirmation("info.separated-stage");
		else if(mode == LayerMode.ALWAYS_TOP && SystemUtils.IS_OS_MAC)
			showConfirmation("info.not-separated-stage");
		if(mode == currentMode) return;
		try {
			OverlayStage nextInstance = (OverlayStage) instances.get(mode).newInstance();
			if(instance != null) {
				instance.hideBalloons();
				instance.hideCharacter();
				instance.close();
			}
			instance = nextInstance;
			nextInstance.showStage();
			nextInstance.showCharacter();
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
	}

	static Rectangle2D getDesktopSize() {
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

	void showCharacter() {}
	void hideCharacter() {}
	void showBalloon(Balloon balloon) {}
	void hideBalloon(Balloon balloon) {}
	synchronized void hideBalloons() {}

	void relocate(Node node, double x, double y){
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
}
class NormalStage extends OverlayStage{
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
	void showCharacter(){
		if(!root.getChildren().contains(App.getInstance().getCharacter()))
			root.getChildren().add(App.getInstance().getCharacter());
		App.getInstance().getCharacter().loadPositionFromStorage();
		update();
	}
	@Override
	void hideCharacter(){
		root.getChildren().remove(App.getInstance().getCharacter());
	}
	@Override
	void showBalloon(Balloon balloon){
		balloon.setDefaultPosition();
		if(!root.getChildren().contains(balloon))
			root.getChildren().add(balloon);
		update();
	}
	@Override
	void hideBalloon(Balloon balloon){
		root.getChildren().remove(balloon);
	}
	synchronized void hideBalloons(){
		Iterator<Node> i = root.getChildren().iterator();
		while (i.hasNext()) {
			Node s = i.next();
			if(s instanceof Balloon)
				i.remove();
		}
	}
}

class TopStage extends NormalStage{
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
			stage.toFront();
			stage.setAlwaysOnTop();
		} catch (Exception e){
			Main.log("Cannot handle top stage change");
		}
	}
}
class FrontNormalStage extends NormalStage{
	FrontNormalStage(){
		super();
	}
	void showBalloon(Balloon balloon){
		super.showBalloon(balloon);
		toFront();
	}
}

class HideStage extends OverlayStage{
	HideStage(){
		super();
		close();
	}
	@Override
	public void showStage(){ }
}

class ShowIfMessageStage extends TopStage{
	@Override
	void showBalloon(Balloon balloon){
		super.showBalloon(balloon);
		show();
	}
	@Override
	void hideBalloon(Balloon balloon){
		Iterator<Node> i = root.getChildren().iterator();
		while (i.hasNext()) {
			Node s = i.next();
			if(s instanceof Balloon){
				if(s==balloon)
					i.remove();
				else return;
			}
		}
		hide();
	}
	@Override
	synchronized void hideBalloons(){
		super.hideBalloons();
		hide();
	}
	@Override
	public void showStage(){
		show();
		if(Balloon.getInstance()==null)
			hide();
	}
}
class SeparatedStage extends OverlayStage{
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
}
class SeparateStage extends OverlayStage{
	private HashMap<Node,SeparatedStage> children = new HashMap<>();
	SeparateStage(){
		super();
		close();
	}
	synchronized void add(Node node){
		if(!children.containsKey(node))
			children.put(node, new SeparatedStage(node));
	}
	synchronized void remove(Node node){
		SeparatedStage s = children.get(node);
		if(s==null) return;
		s.root.getChildren().remove(node);
		children.remove(node);
		s.hide();
		s.close();
		s.getScene().getWindow().hide();
	}
	@Override
	void showCharacter(){
		add(App.getInstance().getCharacter());
	}
	@Override
	void hideCharacter(){
		remove(App.getInstance().getCharacter());
	}
	@Override
	void showBalloon(Balloon balloon){
		add(balloon);
	}
	@Override
	void hideBalloon(Balloon balloon){
		remove(balloon);
	}
	@Override
	synchronized void hideBalloons(){
		javafx.application.Platform.runLater(() -> {
			Iterator<Node> i = children.keySet().iterator();
			while (i.hasNext()) {
				try {
					Node s = i.next();
					if (s instanceof Balloon) {
						children.remove(s);
					}
				} catch (ConcurrentModificationException e){
					Main.log(e);
					return;
				}
			}
		});
	}
	@Override
	void relocate(Node node, double x, double y){
		if(new Double(x).isNaN()) x = 0;
		if(new Double(y).isNaN()) y = 0;
		add(node);
		children.get(node).relocate(x, y);
	}
}