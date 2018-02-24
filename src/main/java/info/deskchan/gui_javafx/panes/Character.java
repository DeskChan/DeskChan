package info.deskchan.gui_javafx.panes;

import info.deskchan.gui_javafx.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.util.Duration;

import java.util.*;

public class Character extends MovablePane {

	private static final String DEFAULT_SKIN_NAME = "illia.image_set";

	private static final int DEFAULT_MESSAGE_PRIORITY = 1000;

	private final String id;
	private AnimatedImageView imageView = new AnimatedImageView();
	private Skin skin = null;
	private String imageName = "normal";
	private String idleImageName = "normal";
	private final DropShadow imageShadow = new DropShadow();
	private PriorityQueue<MessageInfo> messageQueue = new PriorityQueue<>();
	private CharacterBalloon balloon = null;
	private float scaleFactor = 1.0f;
	private float skinOpacity = 1.0f;
	private Color skinColor = null;

	public Character(String id, Skin skin) {
		this.id = id;

		imageShadow.setRadius(10.0);
		imageShadow.setOffsetX(1.5);
		imageShadow.setOffsetY(2.5);

		setScaleFactor(Main.getProperties().getFloat("skin.scale_factor", 100));
		setSkinOpacity(Main.getProperties().getFloat("skin.opacity", 100));
		getChildren().add(imageView);
		setSkin(skin);
		setPositionStorageID("character." + id);

		imageView.setMouseTransparent(true);

		addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
			startDrag(event);
			Map<String, Object> m = new HashMap<>();
			m.put("screenX", event.getScreenX());
			m.put("screenY", event.getScreenY());
			Main.getPluginProxy().sendMessage("gui-events:character-start-drag",m);
		});
		addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
			Map<String, Object> m = new HashMap<>();
			m.put("screenX", event.getScreenX());
			m.put("screenY", event.getScreenY());
			Main.getPluginProxy().sendMessage("gui-events:character-stop-drag",m);
		});
		addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
			boolean enabled = Main.getProperties().getBoolean("character.enable_context_menu", true);
			ContextMenu contextMenu = TrayMenu.getContextMenu();
			// We need to hide menu manually in both cases to avoid showing the menu with incorrect width.
			contextMenu.hide();
			if (enabled && event.getButton() == MouseButton.SECONDARY && event.isStillSincePress()) {
				contextMenu.show(this, event.getScreenX(), event.getScreenY());
			}
		});

		MouseEventNotificator mouseEventNotificator = new MouseEventNotificator(this, "character");
		mouseEventNotificator
				.setOnClickListener()
				.setOnMovedListener()
				.setOnScrollListener(event -> {
					Point2D characterPosition = getPosition();
					double charX = characterPosition.getX();
					double charY = characterPosition.getY();
					double x = event.getX();
					double y = event.getY();
					double x0 = x - charX;
					double y0 = y - charY;

					// just in case
					if (scaleFactor == 0) {
						Main.log("The scale factor of the image is equal to zero!");
						return false;
					}
					x0 /= scaleFactor;
					y0 /= scaleFactor;

					PixelReader imagePixels = imageView.getImage().getPixelReader();
					if (imageView.getImage() != null && x0 < imageView.getImage().getWidth() && y0 < imageView.getImage().getHeight()){
						try {
							Color pixelColor = imagePixels.getColor((int) x0, (int) y0);
							return !pixelColor.equals(Color.TRANSPARENT);
						} catch (Exception e){
							return true;
						}
					}
					return false;
				});

		layoutXProperty().addListener(CharacterBalloon.updateBalloonLayoutX);
		layoutYProperty().addListener(CharacterBalloon.updateBalloonLayoutY);
	}

	public Skin getSkin() {
		return skin;
	}

	public void setSkin(Skin skin) {
		if (skin == null) {
			skin = Skin.load(DEFAULT_SKIN_NAME);
			if (skin == null){
				App.showNotification(Main.getString("error"), Main.getString("error.no-image"));
				Main.getInstance().quit();
				return;
			}
		}
		this.skin = skin;

		setImageName(imageName);
	}

	String getImageName() {
		return imageName;
	}

	public void setImageName(String name) {
		imageName = ((name != null) && (name.length() > 0)) ? name : "normal";
		updateImage();
	}

	private Image getImage() {
		return (skin != null) ? skin.getImage(imageName) : null;
	}

	@Override
	public void setDefaultPosition() {
		Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
		setPosition(new Point2D(screenBounds.getMaxX() - getWidth(),
				screenBounds.getMaxY() - getHeight()));
	}

	private void updateImage(boolean reloadImage) {
	    if (reloadImage) {
            imageView.setImage(getImage());
        }
        Image image = imageView.getImage();
	    if(image == null){
	    	setSkin(null);
	    	return;
		}
		double oldWidth = imageView.getFitWidth();
		double oldHeight = imageView.getFitHeight();
		double newWidth = imageView.getWidth() * scaleFactor;
		double newHeight = imageView.getHeight() * scaleFactor;

		imageView.setFitWidth(newWidth);
		imageView.setFitHeight(newHeight);
		resize(newWidth, newHeight);

		Lighting lighting = null;
		if (skinColor != null && !skinColor.equals(new Color(1,1,1,1))) {
			lighting = new Lighting();
			lighting.setDiffuseConstant(1.0);
			lighting.setSpecularConstant(0.0);
			lighting.setSpecularExponent(0.0);
			lighting.setSurfaceScale(0.0);
			lighting.setLight(new Light.Distant(45, 45, skinColor));
		}
		imageView.setEffect(lighting);

        Point2D oldPosition = getPosition();
        double deltaX = -(newWidth - oldWidth) / 2;
        double deltaY = -(newHeight - oldHeight) / 2;
        Point2D newPosition = new Point2D(oldPosition.getX() + deltaX, oldPosition.getY() + deltaY);
        setPosition(newPosition);
        setShadowOpacity(Main.getProperties().getFloat("skin.shadow-opacity", 1.0f));
	}

	private void updateImage() {
	    updateImage(true);
    }

    /**
     * Scales the image to a given scale factor.
     * If it's greater than 1.0f, the image will be bigger.
     * Use a number in the range of (0.0; 1.0) to make the image smaller.
     * @param scaleFactor a positive float-point number
     */
	public void resizeSkin(float scaleFactor) {
		if (scaleFactor == 0) {
			return;
		}
		setScaleFactor(scaleFactor);
		updateImage(false);
	}

    /**
     * Scales the image to either given width or height keeping its aspect ratio.
     * The width takes precedence over the height.
     * @param width nullable
     * @param height nullable
     */
	public void resizeSkin(Integer width, Integer height) {
	    Double scaleFactor = null;
	    if (width != null) {
	        scaleFactor = width / imageView.getImage().getWidth();
        } else if (height != null) {
            scaleFactor = height / imageView.getImage().getHeight();
        } else {
	        return;
        }
        resizeSkin(scaleFactor.floatValue());
    }

    /**
     * Scales the image relatively. Unlike the usual resizeSprite(), this method
     * gets an old value of the scale factor and adds an increment to it.
     * Use a positive value to zoom in the image, or a negative one to zoom it out.
     * @param scaleFactorIncrement a positive or negative float-point number
     */
	public void resizeSkinRelatively(float scaleFactorIncrement) {
		resizeSkin(scaleFactor + scaleFactorIncrement);
	}

	/**
	 * Changes the absolute value of the opacity of the image.
	 * @param opacity a value in percents (x100)
	 */
	public void changeOpacity(float opacity) {
		opacity /= 100;

		if (opacity < 0 || opacity > 0.99) {
			skinOpacity = 1.0f;
			imageView.setEffect(imageShadow);
		} else {
			skinOpacity = opacity;
			imageView.setEffect(null);
		}
		imageView.setOpacity(skinOpacity);
		updateImage(false);
	}

	/**
	 * Changes the value of the opacity of the image relatively.
	 * Unlike the usual changeOpacity(), this method gets an old value of the scale factor and adds an increment to it.
	 * @param opacityIncrement a positive or negative float-point number
	 */
	public void changeOpacityRelatively(float opacityIncrement) {
		changeOpacity(skinOpacity + opacityIncrement);
	}

	public void setColorFilter(Color color) {
		if(!new Color(1,1,1,1).equals(color))
			skinColor = color;
		else skinColor=null;
		updateImage(false);
	}

	public void setColorFilter(double red, double green, double blue, double opacity) {
		setColorFilter(new Color(red, green, blue, opacity));
	}

	void setColorFilter(double red, double green, double blue) {
		setColorFilter(new Color(red, green, blue, 1.0));
	}

	void setIdleImageName(String name) {
		idleImageName = name;
		setImageName(name);
	}

	public void say(Object data) {
		if(OverlayStage.getCurrentStage() == OverlayStage.LayerMode.HIDE) return;
		MessageInfo messageInfo = null;
		if (data != null) {
			messageInfo = new MessageInfo(data);
			if ((messageInfo.priority < 0) && (messageQueue.size() > 0)) {
				return;
			}
			messageQueue.add(messageInfo);
			Iterator<MessageInfo> i = messageQueue.iterator();
			while (i.hasNext()) {
				MessageInfo s = i.next();
				if(s != messageInfo && s.skippable && s.priority <= messageInfo.priority) i.remove();
			}
			if (messageQueue.peek() != messageInfo) {
				return;
			}
		} else {
			if(messageQueue.peek() != null && messageQueue.peek().itsTimeToStop()){
				messageQueue.poll();
			}
		}
		if (balloon != null) {
			balloon.close();
			balloon = null;
		}
		messageInfo = messageQueue.peek();
		if (messageInfo == null || messageInfo.counter >= messageInfo.text.length) {
			setImageName(idleImageName);
		} else {
			if(messageInfo.characterImage != null)
				setImageName(messageInfo.characterImage);
			balloon = new CharacterBalloon(this, messageInfo.text[messageInfo.counter]);
            messageInfo.counter++;
			balloon.setTimeout(messageInfo.timeout);
			balloon.show();
		}
	}

	public float getScaleFactor() {
	    return scaleFactor * 100;
    }

	/** Set scaling, in percents (x100). **/
    private void setScaleFactor(float scaleFactor) {
		if (scaleFactor == 0) {
			this.scaleFactor = 1.0f;
		} else {
			this.scaleFactor = Math.round(Math.abs(scaleFactor)) / 100.0f;
		}
	}

	float getSkinOpacity() {
		return skinOpacity * 100;
	}

	/** Set opacity, in percents (x100). **/
	public void setSkinOpacity(float opacity) {
		opacity /= 100;
		if (opacity == 0 || opacity > 0.99) {
			skinOpacity = 1.0f;
			imageView.setEffect(imageShadow);
		} else {
			skinOpacity = opacity;
			imageView.setEffect(null);
		}
		imageView.setOpacity(skinOpacity);
	}

	/** Set shadow opacity, in percents (x100). **/
	public void setShadowOpacity(float opacity) {
		opacity /= 100;
		if (opacity > 0.99)
			opacity = 1.0f;

		imageShadow.setColor(Color.color(0, 0, 0, opacity));
		imageView.setEffect(imageShadow);
	}

	private static class MessageInfo implements Comparable<MessageInfo> {
		private final String[] text;
		private final String characterImage;
		private final int priority;
		private final int timeout;
		private final boolean skippable;
		private int counter = 0;

		private static int max_length = 100;

		MessageInfo(Object data) {
			String text2;
			int _timeout;
			boolean partible = true;

			if(data instanceof Map){
				Map<String,Object> mapData = (Map<String,Object>) data;
				String characterImage = (String) mapData.getOrDefault("characterImage", null);
				if (characterImage != null)
					characterImage = characterImage.toLowerCase();

				Object ob = mapData.getOrDefault("skippable", true);
				if(ob instanceof String)
					skippable = Boolean.parseBoolean((String) ob);
				else skippable = (Boolean) ob;

				this.characterImage = characterImage;

				ob = mapData.getOrDefault("priority", DEFAULT_MESSAGE_PRIORITY);
				if(ob instanceof Number)
					 priority = ((Number) ob).intValue();
				else priority = Integer.parseInt((String) ob);

				ob = mapData.getOrDefault("timeout", Main.getProperties().getInteger("balloon.default_timeout", 200) );
				if(ob instanceof Number)
					 _timeout = ((Number) ob).intValue();
				else _timeout = Integer.parseInt((String) ob);

				if(mapData.containsKey("text"))
					text2 = (String) mapData.get("text");
				else if(mapData.containsKey("msgData"))
					text2 = (String) mapData.get("msgData");
				else text2 = "";

				ob = mapData.getOrDefault("partible", true);
				if(ob instanceof String)
					partible = Boolean.parseBoolean((String) ob);
				else partible = (Boolean) ob;
			} else {
				if(data instanceof String)
					text2 = (String) data;
				else text2 = data.toString();
				skippable = true;
				characterImage = null;
				priority = DEFAULT_MESSAGE_PRIORITY;
				_timeout = Main.getProperties().getInteger("balloon.default_timeout", 200);
			}

			if(partible) {
				ArrayList<String> list = new ArrayList<>();
				boolean inQuotes = false;
				ParsingState state = ParsingState.SENTENCE;
				int start = 0, end;
				for (int i = 0; i < text2.length(); i++) {
					switch (text2.charAt(i)) {
						case '.': case '?': case '!':
							if (state == ParsingState.PRE_SENTENCE || inQuotes) {
								continue;
							}
							state = ParsingState.END_OF_SENTENCE;
							break;
						case ' ':
							if (state == ParsingState.SENTENCE && !inQuotes) {
								continue;
							}
							state = ParsingState.PRE_SENTENCE;
							break;
						case '"': case '\'':
							if (state != ParsingState.SENTENCE) {
								state = ParsingState.SENTENCE;
							}
							inQuotes = !inQuotes;
							break;
						default:
							if (state == ParsingState.SENTENCE || inQuotes) {
								continue;
							}
							end = i;
							while (text2.charAt(end) == ' ') end--;
							state = ParsingState.SENTENCE;
							list.add(text2.substring(start, end));
							start = i;
					}
				}
				list.add(text2.substring(start));
				for (int i = 0; i < list.size(); i++) {
					if (list.get(i).length() > max_length) {
						if (list.get(i).contains(",")) {
							String[] spl = list.get(i).split(",\\s*");
							StringBuilder left = new StringBuilder();
							StringBuilder right = new StringBuilder();
							left.append(spl[0]).append(",");
							int j = 1;
							while (left.length() + spl[j].length() < max_length) {
								left.append(" ").append(spl[j]).append(",");
								j++;
							}
							left.append("...");
							right.append("...");
							for (; j < spl.length; j++) {
								right.append(" ").append(spl[j]).append(j < spl.length - 1 ? "," : "");
							}
							list.set(i, left.toString());
							list.add(i + 1, right.toString());
							continue;
						} else {
							int l = list.get(i).length() / 2;
							while (list.get(i).charAt(l) != ' ') {
								l--;
							}
							list.add(i + 1, "... " + list.get(i).substring(l + 1));
							list.set(i, list.get(i).substring(0, l - 1) + "...");
							i--;
							continue;
						}
					}
					if (i + 1 == list.size() || list.get(i).length() + list.get(i + 1).length() > max_length) {
						continue;
					}
					list.set(i, list.get(i) + " " + list.get(i + 1));
					list.remove(i + 1);
					i--;
				}
				text = list.toArray(new String[list.size()]);
			} else {
				text = new String[1];
				text[0] = text2;
			}
			timeout = Math.max( 3000 , text2.length() / text.length * _timeout );
		}

        boolean itsTimeToStop(){
            return counter >= text.length;
        }

		@Override
		public int compareTo(MessageInfo messageInfo) {
			return -(priority - messageInfo.priority);
		}

		@Override
		public String toString(){
        	String t = "";
        	for (String item : text) t += item;
        	return "[" + characterImage + "]: " + t + " / " + priority;
		}
		private enum ParsingState {
			SENTENCE,
			END_OF_SENTENCE,
			PRE_SENTENCE
		}
	}

	class AnimatedImageView extends Parent implements EventHandler<javafx.event.ActionEvent> {

		private ImageView mainImage = new ImageView();
		private ImageView secondImage = new ImageView();
		private Timeline timeline;

		AnimatedImageView(){
			getChildren().add(mainImage);
			getChildren().add(secondImage);
			secondImage.setOpacity(0);
		}

		public void setImage(Image image){
			swap();
			mainImage.setImage(image);
			mainImage.setOpacity(0);
			secondImage.setOpacity(1);
			timeline = new Timeline(new KeyFrame(Duration.millis(20), this));
			timeline.setCycleCount(Timeline.INDEFINITE);
			timeline.play();
		}

		@Override
		public void handle(javafx.event.ActionEvent actionEvent) {
			double opacity = mainImage.getOpacity() + 0.1;
			mainImage.setOpacity(opacity);
			secondImage.setOpacity(1 - opacity);
			if (opacity >= 1){
				secondImage.setImage(null);
				timeline.stop();
			}
		}

		private void swap(){
			ImageView t = mainImage;
			mainImage = secondImage;
			secondImage = t;
		}

		public Image getImage(){ return mainImage.getImage(); }

		public double getWidth(){
			return Math.max(
				  mainImage.getImage() != null ?   mainImage.getImage().getWidth() : 0,
				secondImage.getImage() != null ? secondImage.getImage().getWidth() : 0
			);
		}

		public double getHeight(){
			return Math.max(
				  mainImage.getImage() != null ?   mainImage.getImage().getHeight() : 0,
				secondImage.getImage() != null ? secondImage.getImage().getHeight() : 0
			);
		}

		public double getFitWidth(){
			return Math.max(mainImage.getFitWidth(), secondImage.getFitWidth());
		}

		public double getFitHeight(){
			return Math.max(mainImage.getFitHeight(), secondImage.getFitHeight());
		}

		public void setFitWidth(double width){
			mainImage.setFitWidth(width);
			secondImage.setFitWidth(width);
		}

		public void setFitHeight(double height){
			mainImage.setFitHeight(height);
			secondImage.setFitHeight(height);
		}

	}

}
