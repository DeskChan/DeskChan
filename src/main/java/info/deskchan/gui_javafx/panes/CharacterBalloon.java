package info.deskchan.gui_javafx.panes;

import info.deskchan.gui_javafx.LocalFont;
import info.deskchan.gui_javafx.Main;
import info.deskchan.gui_javafx.MouseEventNotificator;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class CharacterBalloon extends Balloon {

	private static CharacterBalloon instance;

	public static CharacterBalloon getInstance(){ return instance; }

	public enum PositionMode {
		AUTO,
		RELATIVE,
		RELATIVE_FOR_EACH,
		ABSOLUTE
	}

	public enum DirectionMode {
		NO_DIRECTION,
		ALWAYS_INVERTED,
		STANDARD_DIRECTION,
		INVERTED_DIRECTION
	}

	protected static BalloonDrawer drawer;

	public static void updateDrawer(){
		drawer = getDrawer("balloon.path-character");
	}

	protected Character character = null;
	protected Timeline timeoutTimeline = null;
	protected final Text content;
	protected PositionMode positionMode = PositionMode.ABSOLUTE;
	protected DirectionMode directionMode = DirectionMode.STANDARD_DIRECTION;
	protected long lastClick = -1;

	protected final MouseEventNotificator mouseEventNotificator = new MouseEventNotificator(this, "balloon");

	protected SymbolsAdder symbolsAdder;

	CharacterBalloon(Character character, String text) {
		super();
		instance = this;

		positionMode = CharacterBalloon.PositionMode.valueOf(
				Main.getProperties().getString("balloon_position_mode", positionMode.toString())
		);
		directionMode = CharacterBalloon.DirectionMode.valueOf(
				Main.getProperties().getString("balloon_direction_mode", directionMode.toString())
		);

		Text label = new Text("");
		label.setFont(defaultFont);
		if (defaultFont != null) {
			label.setFont(defaultFont);
		} else {
			label.setFont(LocalFont.defaultFont);
		}

		Integer animation_delay = Main.getProperties().getInteger("balloon.text-animation-delay", 50);
		if (animation_delay > 0)
			symbolsAdder = new SymbolsAdder(text, animation_delay);
		else
			label.setText(text);

		content = label;
		bubblePane = drawer.createBalloon(content);

		getChildren().add(bubblePane);

		label.setWrappingWidth(bubblePane.getContentWidth());

		setBalloonScaleFactor(Main.getProperties().getFloat("balloon.scale_factor", 100));
		setBalloonOpacity(Main.getProperties().getFloat("balloon.opacity", 100));

		setOnMousePressed(event -> {
			lastClick = System.currentTimeMillis();
			if ((positionMode != PositionMode.AUTO) && event.getButton().equals(MouseButton.PRIMARY)) {
				startDrag(event);
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
				.setOnScrollListener(event -> true);

		this.character = character;

		if (positionMode != PositionMode.ABSOLUTE) {
			positionRelativeToDesktopSize = false;
		}
	}

	@Override
	public void setDefaultPosition() {
		if (character != null) {
			impl_updateBalloonLayoutX();
			impl_updateBalloonLayoutY();
		} else {
			super.setDefaultPosition();
		}
	}

	@Override
	public void loadPositionFromStorage(){
		if (positionMode == CharacterBalloon.PositionMode.RELATIVE || positionMode == CharacterBalloon.PositionMode.RELATIVE_FOR_EACH) {
			assert character != null;
			Point2D position = character.getSkin().getPreferredBalloonPosition(
					positionMode == CharacterBalloon.PositionMode.RELATIVE ? character.getSkin().getName() : character.getImageName()
			);
			setPosition(character.getPosition().add(position));
		} else if (positionMode == CharacterBalloon.PositionMode.ABSOLUTE) {
			super.loadPositionFromStorage();
		} else setDefaultPosition();
	}

	@Override
	protected void storePositionToStorage() {
		if (positionMode == CharacterBalloon.PositionMode.RELATIVE || positionMode == CharacterBalloon.PositionMode.RELATIVE_FOR_EACH) {
			assert character != null;
			character.getSkin().overridePreferredBalloonPosition(
					positionMode == CharacterBalloon.PositionMode.RELATIVE ? character.getSkin().getName() : character.getImageName(),
					getPosition().subtract(character.getPosition())
			);
		} else if (positionMode == CharacterBalloon.PositionMode.ABSOLUTE) {
			super.storePositionToStorage();
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
					try {
						getInstance().impl_updateBalloonLayoutX();
					} catch (Exception e){ }
				});
			}
		}
	};
	public static ChangeListener<java.lang.Number> updateBalloonLayoutY = new ChangeListener<Number>() {
		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			if(getInstance()!=null) {
				javafx.application.Platform.runLater(() -> {
					try {
						getInstance().impl_updateBalloonLayoutY();
					} catch (Exception e){ }
				});
			}
		}
	};

	private void impl_updateBalloonLayoutX() {
		if (positionMode == PositionMode.ABSOLUTE || character == null || character.getParent() == null) return;
		if (positionMode == PositionMode.RELATIVE || positionMode == PositionMode.RELATIVE_FOR_EACH){
			try{
				loadPositionFromStorage();
				return;
			} catch (Exception e){ }
		}
		double x = character.localToScreen(character.getBoundsInLocal()).getMinX();
		double width = prefWidth(-1);

		boolean rightAlign = width - x < 0;
		x += rightAlign ? (-width) : character.getWidth();
		relocate(x, getPosition().getY());

		bubblePane.invert(rightAlign, directionMode);

	}

	private void impl_updateBalloonLayoutY() {
		if(positionMode == PositionMode.ABSOLUTE || character == null || character.getParent() == null) return;
		if (positionMode == PositionMode.RELATIVE || positionMode == PositionMode.RELATIVE_FOR_EACH){
			try{
				loadPositionFromStorage();
				return;
			} catch (Exception e){ }
		}
		double y = character.localToScreen(character.getBoundsInLocal()).getMinY();
		relocate(getPosition().getX(), y);
	}

	public void setFont(Font font){
		content.setFont(font);
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

	public static void setDefaultPositionMode(String positionMode){
		PositionMode mode;
		try {
			mode = PositionMode.valueOf(positionMode.toUpperCase());
		} catch (Exception e){
			mode = PositionMode.AUTO;
		}
		Main.getProperties().put("balloon_position_mode", mode);
	}

	public static void setDefaultDirectionMode(String directionMode){
		DirectionMode mode;
		try {
			mode = DirectionMode.valueOf(directionMode.toUpperCase());
		} catch (Exception e){
			mode = DirectionMode.STANDARD_DIRECTION;
		}
		Main.getProperties().put("balloon_direction_mode", mode);
	}

	void show() {
		super.show();
		setPositionStorageID(character.getId() + ".balloon");
	}


	class SymbolsAdder implements EventHandler<javafx.event.ActionEvent> {

		private static final int delayLimit = 40;
		private final Timeline timeline;
		private final String text;
		private final int addCount;

		SymbolsAdder(String text, Integer delay) {
			this.text = text;
			if (delay < delayLimit){
				addCount = delayLimit / delay;
				delay = delayLimit;
			} else
				addCount = 1;

			timeline = new Timeline(new KeyFrame(Duration.millis(delay), this));
			timeline.setCycleCount(Timeline.INDEFINITE);
			timeline.play();
		}

		private boolean flag = false;
		@Override
		public void handle(javafx.event.ActionEvent actionEvent) {
			int counter = addCount;
			while (counter > 0) {
				if(text.length() <= content.getText().length()){
					timeline.stop();
					return;
				}
				char c = text.charAt(content.getText().length());
				if (c == '.') {
					if (!flag) {
						timeline.setCycleCount(timeline.getCycleCount() + 1);
						flag = true;
						return;
					}
					flag = false;
				}
				content.setText(content.getText() + c);
				counter--;
			}
		}
	}
}
