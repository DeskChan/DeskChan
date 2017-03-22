package info.deskchan.gui_javafx;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;

class Balloon extends StackPane {
	
	private static final String BUBBLE_SVG_PATH = "m 32.339338,-904.55632 c -355.323298,0 -643.374998,210.31657 " +
			"-643.374998,469.78125 0,259.46468 288.0517,469.812505 643.374998,469.812505 123.404292,0 " +
			"238.667342,-25.3559002 336.593752,-69.3438 69.80799,78.7043 181.84985,84.1354 378.90625,5.3126 " +
			"-149.2328,-8.9191 -166.3627,-41.22 -200.6562,-124.031305 80.6876,-78.49713 128.5,-176.04496 " +
			"128.5,-281.75 0,-259.46468 -288.0205,-469.78125 -643.343802,-469.78125 z";
	
	private static Font defaultFont = null;
	
	private final Character character;
	private String layer;
	private SVGPath bubbleShape = new SVGPath();
	
	Balloon(Character character, String text) {
		this.character = character;
		setPrefWidth(400);
		setMinHeight(200);
		bubbleShape.setContent(BUBBLE_SVG_PATH);
		bubbleShape.setFill(Color.WHITE);
		bubbleShape.setStroke(Color.BLACK);
		bubbleShape.setScaleX(0);
		bubbleShape.setScaleY(0);
		Label label = new Label(text);
		label.setWrapText(true);
		if (defaultFont != null) {
			label.setFont(defaultFont);
		}
		StackPane labelPane = new StackPane();
		labelPane.getChildren().add(label);
		StackPane.setMargin(label, new Insets(40, 20, 40, 20));
		getChildren().add(new Group(bubbleShape));
		getChildren().add(labelPane);
		if (character != null) {
			ChangeListener<java.lang.Number> updateBalloonLayoutX = (property, oldValue, value) -> {
				double x = character.getLayoutX() - getWidth();
				setLayoutX((x >= 0) ? x : character.getLayoutX() + character.getWidth());
				bubbleShape.getParent().setScaleX((x >= 0) ? 1 : -1);
			};
			character.layoutXProperty().addListener(updateBalloonLayoutX);
			widthProperty().addListener(updateBalloonLayoutX);
			layoutYProperty().bind(character.layoutYProperty());
		}
		setOnMouseClicked(event -> {
			if (character != null) {
				character.say(null);
			} else {
				close();
			}
		});
	}
	
	Character getCharacter() {
		return character;
	}
	
	void show(String layer) {
		this.layer = layer;
		OverlayStage.getInstance(layer).getRoot().getChildren().add(this);
	}
	
	void close() {
		OverlayStage.getInstance(layer).getRoot().getChildren().remove(this);
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
