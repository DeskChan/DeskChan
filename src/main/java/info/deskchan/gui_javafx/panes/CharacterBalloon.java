package info.deskchan.gui_javafx.panes;

import info.deskchan.gui_javafx.LocalFont;
import info.deskchan.gui_javafx.Main;
import info.deskchan.gui_javafx.MouseEventNotificator;
import info.deskchan.gui_javafx.panes.sprite_drawers.Sprite;
import javafx.animation.Animation;
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

	private static CharacterBalloon instance = null;

	public static CharacterBalloon getInstance(){
		if (instance == null)
			instance = new CharacterBalloon();
		return instance;
	}

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

	protected static Sprite sprite;

	public static void updateDrawer(){
		sprite = getBalloonSprite("balloon.path-character");
		instance = null;
	}

	protected Character character = null;
	protected Timeline timeoutTimeline = null;
	protected final Text content;
	protected PositionMode positionMode = PositionMode.AUTO;
	protected DirectionMode directionMode = DirectionMode.STANDARD_DIRECTION;
	protected long lastClick = -1;

	protected final MouseEventNotificator mouseEventNotificator = new MouseEventNotificator(this, "balloon");

	protected SymbolsAdder symbolsAdder;

	protected CharacterBalloon() {
		super();
		instance = this;
		setId("character-balloon");

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

		content = label;
		bubblePane = sprite;
		sprite.setSpriteContent(content);
		getChildren().add(bubblePane);

		label.setWrappingWidth(bubblePane.getContentWidth());

		setBalloonScaleFactor(Main.getProperties().getFloat("balloon.scale_factor", 100));
		setBalloonOpacity(Main.getProperties().getFloat("balloon.opacity", 100));

		setOnMousePressed(event -> {
			lastClick = System.currentTimeMillis();
			if (event.isSecondaryButtonDown()){
				UserBalloon.show(null);
				return;
			}
			if ((positionMode != PositionMode.AUTO) && event.getButton().equals(MouseButton.PRIMARY)) {
				startDrag(event);
			}
		});
		setOnMouseReleased(event -> {
			if(!isDragging() && event.getButton().equals(MouseButton.PRIMARY) && (System.currentTimeMillis()-lastClick)<200) {
				if (symbolsAdder.isDone()) {
					if (character != null) {
						character.say(null);
					} else {
						hide();
					}
				} else {
					symbolsAdder.stop();
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

		if (positionMode != PositionMode.ABSOLUTE) {
			positionRelativeToDesktopSize = false;
		}
	}

	protected void setup(Character character, String text){
		if (symbolsAdder != null)
			symbolsAdder.stop();
		Integer animation_delay = Main.getProperties().getInteger("balloon.text-animation-delay", 50);
		if (animation_delay > 0)
			symbolsAdder = new SymbolsAdder(text, animation_delay);
		else
			content.setText(text);
		this.character = character;
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
			setPosition(position != null ? character.getPosition().add(position) : character.getPosition());
		} else if (positionMode == CharacterBalloon.PositionMode.ABSOLUTE) {
			super.loadPositionFromStorage();
		} else setDefaultPosition();
	}

	@Override
	public void storePositionToStorage() {
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
			javafx.application.Platform.runLater(() -> {
				try {
					getInstance().impl_updateBalloonLayoutX();
				} catch (Exception e){ }
			});
		}
	};
	public static ChangeListener<java.lang.Number> updateBalloonLayoutY = new ChangeListener<Number>() {
		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			javafx.application.Platform.runLater(() -> {
				try {
					getInstance().impl_updateBalloonLayoutY();
				} catch (Exception e){ }
			});
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

		boolean rightAlign = x - width < 0;
		x += rightAlign ? character.getWidth() : (-width);
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
		System.out.println(font);
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

	public static void setDefaultPositionMode(PositionMode positionMode){
		Main.getProperties().put("balloon_position_mode", positionMode.toString());
		if (instance != null)
			instance.positionMode = positionMode;
	}

	public void setPositionMode(PositionMode mode){
		positionMode = mode;
	}

	public static void setDefaultDirectionMode(DirectionMode directionMode){
		Main.getProperties().put("balloon_direction_mode", directionMode);
		if (instance != null)
			instance.directionMode = directionMode;
	}

	@Override
	public void show() {
		super.show();
		setPositionStorageID(character.getId() + ".balloon");
	}

	class SymbolsAdder implements EventHandler<javafx.event.ActionEvent> {

		private final Timeline timeline;
		private final String text;
		private final int addCount;

		SymbolsAdder(String text, Integer delay) {
			int delayLimit = Main.getProperties().getInteger("sprites-animation-delay", 50);

			this.text = text;
			if (delay < delayLimit){
				addCount = delayLimit / delay;
				delay = delayLimit;
			} else
				addCount = 1;

			content.setText("");
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

		public void stop(){
			timeline.stop();
			content.setText(text);
		}

		public boolean isDone(){ return timeline.getStatus() == Animation.Status.STOPPED; }
	}
}
