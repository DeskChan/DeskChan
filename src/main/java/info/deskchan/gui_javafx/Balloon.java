package info.deskchan.gui_javafx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.util.Duration;

class Balloon extends MovablePane {

	private static Balloon instance;

	public static Balloon getInstance(){ return instance; }

	enum PositionMode {
		AUTO,
		RELATIVE,
		ABSOLUTE
	}
	
	private static final String BUBBLE_SVG_PATH = "m 32.339338,-904.55632 c -355.323298,0 -643.374998,210.31657 " +
			"-643.374998,469.78125 0,259.46468 288.0517,469.812505 643.374998,469.812505 123.404292,0 " +
			"238.667342,-25.3559002 336.593752,-69.3438 69.80799,78.7043 181.84985,84.1354 378.90625,5.3126 " +
			"-149.2328,-8.9191 -166.3627,-41.22 -200.6562,-124.031305 80.6876,-78.49713 128.5,-176.04496 " +
			"128.5,-281.75 0,-259.46468 -288.0205,-469.78125 -643.343802,-469.78125 z";
	
	private static Font defaultFont = null;
	
	private Character character = null;
	private SVGPath bubbleShape = new SVGPath();
	private Timeline timeoutTimeline = null;
	private final DropShadow bubbleShadow = new DropShadow();
	private final StackPane stackPane = new StackPane();
	private final Node content;
	private PositionMode positionMode = PositionMode.ABSOLUTE;
	private long lastClick = -1;

	private final MouseEventNotificator mouseEventNotificator = new MouseEventNotificator(this, "balloon");
	private float balloonOpacity = 1.0f;

	Balloon(String id, String text) {
		instance=this;

		setBalloonOpacity(Float.parseFloat(Main.getProperty("balloon.opacity", "1.0")));
            
		stackPane.setPrefWidth(400);
		stackPane.setMinHeight(200);
		
		bubbleShape.setContent(BUBBLE_SVG_PATH);
		bubbleShape.setFill(Color.WHITE);
		bubbleShape.setStroke(Color.BLACK);

		bubbleShape.setScaleX(0);
		bubbleShape.setScaleY(0);
                
		bubbleShadow.setRadius(5.0);
		bubbleShadow.setOffsetX(1.5);
		bubbleShadow.setOffsetY(2.5);
		bubbleShadow.setColor(Color.BLACK);
		
		Label label = new Label(text);
		label.setWrapText(true);
		if (defaultFont != null) {
			label.setFont(defaultFont);
		}
		content = label;
		StackPane contentPane = new StackPane();
		contentPane.getChildren().add(content);
		stackPane.getChildren().add(new Group(bubbleShape));
		stackPane.getChildren().add(contentPane);
		StackPane.setMargin(content, new Insets(40, 20, 40, 20));
		
		getChildren().add(stackPane);

		setOnMousePressed(event -> {
			lastClick = System.currentTimeMillis();
			if ((positionMode != PositionMode.AUTO) && event.getButton().equals(MouseButton.PRIMARY)) {
				startDrag(event);
				return;
			}
		});
		setOnMouseReleased(event -> {
			if(!isDragging() && event.getButton().equals(MouseButton.PRIMARY) && (System.currentTimeMillis()-lastClick)<200) {
				if (character != null) {
					character.say(null);
				} else {
					close();
				}
			}
		});

		setOnMouseEntered(event -> {
			if (character != null && timeoutTimeline != null) {
				timeoutTimeline.stop();
			}
		});
		setOnMouseExited(event -> {
			if (character != null && timeoutTimeline != null) {
				timeoutTimeline.play();
			}
		});

		mouseEventNotificator
				.setOnClickListener()
				.setOnMovedListener()
				// TODO: Figure out how to write more precise check.
				.setOnScrollListener(event -> true);
	}
	
	Balloon(Character character, PositionMode positionMode, String text) {

		this(character.getId() + ".balloon", text);

		instance=this;

		this.character = character;
		this.positionMode = positionMode;
		if (positionMode != PositionMode.ABSOLUTE) {
			positionRelativeToDesktopSize = false;
		}
	}
	private EventHandler<MouseEvent> pressEventHandler = (e) -> {
		lastClick = System.currentTimeMillis();
	};
	public static ChangeListener<java.lang.Number> updateBalloonLayoutX = new ChangeListener<Number>() {
		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			if(getInstance()!=null) {
				javafx.application.Platform.runLater(() -> {
					getInstance().impl_updateBalloonLayoutX();
				});
			}
		}
	};
	public static ChangeListener<java.lang.Number> updateBalloonLayoutY = new ChangeListener<Number>() {
		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			if(getInstance()!=null) {
				javafx.application.Platform.runLater(() -> {
					getInstance().impl_updateBalloonLayoutY();
				});
			}
		}
	};
	private void impl_updateBalloonLayoutX() {
		if(positionMode == PositionMode.ABSOLUTE) return;
		if(positionMode == PositionMode.RELATIVE){
			loadPositionFromStorage();
			return;
		}
		double width = prefWidth(-1);
		double x = character.localToScreen(character.getBoundsInLocal()).getMinX();
		boolean rightAlign = x-width>0;
		x += rightAlign ? (-width) : character.getWidth();
		relocate(x, getPosition().getY());

		bubbleShape.getParent().setScaleX((rightAlign) ? 1 : -1);
		StackPane.setMargin(content, new Insets(40, (rightAlign) ? 40 : 20,
				40, (rightAlign) ? 20 : 40));
	}
	private void impl_updateBalloonLayoutY() {
		if(positionMode == PositionMode.ABSOLUTE) return;
		if (positionMode == PositionMode.RELATIVE) {
			loadPositionFromStorage();
			return;
		}
		double y = character.localToScreen(character.getBoundsInLocal()).getMinY();
		relocate(getPosition().getX(), y);
	}
    /**
	 * Changes the absolute value of the opacity of the image.
	 * @param opacity a value in the range of (0.0; 1.0]
	 */
	private void changeOpacity(float opacity) {
		if (opacity == 0 || opacity > 1.0) {
			return;
		}
		setBalloonOpacity(opacity);
	}

	/**
	 * Changes the value of the opacity of the image relatively.
	 * Unlike the usual changeOpacity(), this method gets an old value of the scale factor and adds an increment to it.
	 * @param opacityIncrement a positive or negative float-point number
	 */
	void changeOpacityRelatively(float opacityIncrement) {
		changeOpacity(balloonOpacity + opacityIncrement);
	}
                
	float getSkinOpacity() {
        return balloonOpacity;
	}
        
	public void setBalloonOpacity(float opacity) {
		if (opacity == 0 || opacity > 0.99) {
			balloonOpacity = 1.0f;
			stackPane.setEffect(bubbleShadow);
		} else {
			balloonOpacity = Math.round(Math.abs(opacity) * 20.0f) / 20.0f;
			stackPane.setEffect(null);
		}
		bubbleShape.setOpacity(balloonOpacity);
	}
        
	@Override
	protected void setDefaultPosition() {
		if (character != null) {
			impl_updateBalloonLayoutX();
			impl_updateBalloonLayoutY();
		} else {
			super.setDefaultPosition();
		}
	}
	
	@Override
	protected void loadPositionFromStorage() {
		if (positionMode == PositionMode.RELATIVE) {
			assert character != null;
			setPosition(character.getPosition().add(character.getSkin().getPreferredBalloonPosition(character.getImageName())));
		} else if (positionMode == PositionMode.ABSOLUTE) {
			super.loadPositionFromStorage();
		} else setDefaultPosition();
	}
	
	@Override
	protected void storePositionToStorage() {
		if (positionMode == PositionMode.RELATIVE) {
			assert character != null;
			character.getSkin().overridePreferredBalloonPosition(character.getImageName(),
					getPosition().subtract(character.getPosition()));
		} else if (positionMode == PositionMode.ABSOLUTE) {
			super.storePositionToStorage();
		}
	}
	
	Character getCharacter() {
		return character;
	}
	
	void show(String layer) {
		OverlayStage.getInstance().showBalloon(this);
		setPositionStorageID(character.getId() + ".balloon");
	}
	
	void hide() {
		OverlayStage.getInstance().hideBalloon(this);
	}
	
	void close() {
		setTimeout(0);
		getChildren().removeAll();
		hide();
		mouseEventNotificator.cleanListeners();
		instance = null;
	}
	
	void setTimeout(int timeout) {
		if (timeoutTimeline != null) {
			timeoutTimeline.stop();
			timeoutTimeline = null;
		}
		if (timeout > 0) {
			timeoutTimeline = new Timeline(new KeyFrame(
					Duration.millis(timeout),
					event -> character.say(null)
			));
			timeoutTimeline.play();
		}
	}
	
	@Override
	public void layoutChildren() {
		super.layoutChildren();
		if ((bubbleShape.getScaleX() == 0) && (bubbleShape.getScaleY() == 0)) {
			bubbleShape.setScaleX(getWidth() / bubbleShape.getBoundsInLocal().getWidth());
			bubbleShape.setScaleY(getHeight() / bubbleShape.getBoundsInLocal().getHeight());
		}
	}
	
	static Font getDefaultFont() {
		return defaultFont;
	}
	
	static void setDefaultFont(Font font) {
		defaultFont = font;
	}
	
}
