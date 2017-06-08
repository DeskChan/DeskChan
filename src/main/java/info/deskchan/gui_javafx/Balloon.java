package info.deskchan.gui_javafx;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.util.Duration;

class Balloon extends MovablePane {
	
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
	private String layer;
	private SVGPath bubbleShape = new SVGPath();
	private Timeline timeoutTimeline = null;
	private final DropShadow bubbleShadow = new DropShadow();
	private final StackPane stackPane = new StackPane();
	private final Node content;
	private PositionMode positionMode = PositionMode.ABSOLUTE;

	private final MouseEventNotificator mouseEventNotificator = new MouseEventNotificator(this, "balloon");
	
	Balloon(String id, String text) {
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
		stackPane.setEffect(bubbleShadow);
		
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
			if ((positionMode != PositionMode.AUTO) && event.isSecondaryButtonDown()) {
				startDrag(event);
				return;
			}
			if (character != null) {
				character.say(null);
			} else {
				close();
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
		this.character = character;
		this.positionMode = positionMode;
		if (positionMode != PositionMode.ABSOLUTE) {
			positionRelativeToDesktopSize = false;
			positionAnchor = character;
			if (positionMode == PositionMode.AUTO) {
				ChangeListener<java.lang.Number> updateBalloonLayoutX = (property, oldValue, value) -> {
					double x = character.getLayoutX() - getWidth();
					setLayoutX((x >= 0) ? x : character.getLayoutX() + character.getWidth());
					bubbleShape.getParent().setScaleX((x >= 0) ? 1 : -1);
					StackPane.setMargin(content, new Insets(40, (x >= 0) ? 40 : 20,
							40, (x >= 0) ? 20 : 40));
				};
				character.layoutXProperty().addListener(updateBalloonLayoutX);
				widthProperty().addListener(updateBalloonLayoutX);
				layoutYProperty().bind(character.layoutYProperty());
			} else {
				character.layoutXProperty().addListener((property, oldValue, value) -> {
					loadPositionFromStorage();
				});
				character.layoutYProperty().addListener((property, oldValue, value) -> {
					loadPositionFromStorage();
				});
			}
		}
		setPositionStorageID(character.getId() + ".balloon");
	}
	
	@Override
	protected void setDefaultPosition() {
		if (character != null) {
			double x = character.getLayoutX() - getWidth();
			setLayoutX((x >= 0) ? x : character.getLayoutX() + character.getWidth());
			bubbleShape.getParent().setScaleX((x >= 0) ? 1 : -1);
			StackPane.setMargin(content, new Insets(40, (x >= 0) ? 40 : 20,
					40, (x >= 0) ? 20 : 40));
			setLayoutY(character.getLayoutY());
		} else {
			super.setDefaultPosition();
		}
	}
	
	@Override
	protected void loadPositionFromStorage() {
		if (positionMode == PositionMode.RELATIVE) {
			assert character != null;
			setPosition(character.getSkin().getPreferredBalloonPosition(character.getImageName()));
		} else if (positionMode == PositionMode.ABSOLUTE) {
			super.loadPositionFromStorage();
		}
	}
	
	@Override
	protected void storePositionToStorage() {
		if (positionMode == PositionMode.RELATIVE) {
			assert character != null;
			character.getSkin().overridePreferredBalloonPosition(character.getImageName(),
					getPosition());
		} else if (positionMode == PositionMode.ABSOLUTE) {
			super.storePositionToStorage();
		}
	}
	
	Character getCharacter() {
		return character;
	}
	
	void show(String layer) {
		if (this.layer != null) {
			hide();
		}
		this.layer = layer;
		OverlayStage.getInstance(layer).getRoot().getChildren().add(this);
	}
	
	void hide() {
		if (layer != null) {
			OverlayStage.getInstance(layer).getRoot().getChildren().remove(this);
			layer = null;
		}
	}
	
	void close() {
		setTimeout(0);
		hide();
		mouseEventNotificator.cleanListeners();
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
