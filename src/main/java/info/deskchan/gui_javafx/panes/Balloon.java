package info.deskchan.gui_javafx.panes;

import info.deskchan.gui_javafx.App;
import info.deskchan.gui_javafx.LocalFont;
import info.deskchan.gui_javafx.Main;
import info.deskchan.gui_javafx.OverlayStage;
import info.deskchan.gui_javafx.panes.sprite_drawers.SVGSprite;
import info.deskchan.gui_javafx.panes.sprite_drawers.Sprite;
import javafx.geometry.Insets;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

public class Balloon extends MovablePane {

    private static List<Balloon> openedBalloons = new LinkedList<>();

    protected static Sprite getBalloonSprite(String pathKey){
        Sprite sprite;
        String path = Main.getPluginProxy().getProperties().getString(pathKey);
        if (path == null || !new File(path).exists()){
            path = Main.getPluginProxy().getAssetsDirPath().resolve("balloons").resolve("bubble.svg").toString();
            if (!new File(path).exists()){
                return createDefaultBubble();
            }
        }

        try {
            sprite = Sprite.getSpriteFromFile(new File(path));
        } catch (Exception e) {
            Main.log(new Exception("Cannot set file " + path + "as balloon skin, unsupported format", e));
            sprite = createDefaultBubble();
        }
        return sprite;
    }

    protected static Font defaultFont = null;
    protected final DropShadow bubbleShadow = new DropShadow();
    protected float balloonOpacity = 100;
    protected Sprite bubblePane;

    protected Balloon() {
        openedBalloons.add(this);

        bubbleShadow.setRadius(10.0);
        bubbleShadow.setOffsetX(1.5);
        bubbleShadow.setOffsetY(2.5);
        bubbleShadow.setColor(Color.color(0, 0, 0, Main.getProperties().getFloat("balloon.shadow-opacity", 1.0f)));

        getStylesheets().add(App.getStylesheet());
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


    public static void setOpacity(float opacity) {
        opacity /= 100;
        opacity = Math.max(opacity, 0);
        opacity = Math.min(opacity, 1);
        Main.getProperties().put("balloon.opacity", opacity * 100);
        for (Balloon instance : openedBalloons)
            instance.setBalloonOpacity(opacity);
    }

    protected void setBalloonOpacity(float opacity) {
        if (opacity > 0.99) {
            bubblePane.setEffect(bubbleShadow);
        } else {
            bubblePane.setEffect(null);
        }
        bubblePane.setOpacity(balloonOpacity);
    }

    public static void setShadowOpacity(float opacity) {
        opacity /= 100;
        opacity = Math.max(opacity, 0);
        opacity = Math.min(opacity, 1);
        Main.getProperties().put("balloon.shadow-opacity", opacity);
        for (Balloon instance : openedBalloons)
            instance.setBalloonShadowOpacity(opacity);
    }

    protected void setBalloonShadowOpacity(float opacity) {
        bubbleShadow.setColor(Color.color(0, 0, 0, opacity));
    }

    public static void setScaleFactor(float scale) {
        Main.getProperties().put("balloon.scale_factor", scale);
        for (Balloon instance : openedBalloons)
            instance.setBalloonScaleFactor(scale);
    }

    protected void setBalloonScaleFactor(float scale) {
        scale = Math.max(scale, 0) / 100;
        bubblePane.setScaleX(scale);
        bubblePane.setScaleY(scale);
    }

    @Override
    public void show() {
        OverlayStage.getInstance().showSprite(this);
        toFront();
        requestFocus();
    }

    private static final String DEFAULT_FONT = "PT Sans, 16.0";

    public static void setDefaultFont(String font) {
        if (font == null)
            font = DEFAULT_FONT;
        setDefaultFont(LocalFont.fromString(font));
    }

    static void setDefaultFont(Font font) {
        if (font == null)
            font = LocalFont.fromString(DEFAULT_FONT);
        defaultFont = font;
        Main.getProperties().put("balloon.font", LocalFont.toString(font));

        for (Balloon instance : openedBalloons)
            instance.setFont(font);
    }

    public void setFont(Font font){}

    public static Sprite createDefaultBubble(){
        Main.log("Balloon file not found, using default");

        String BUBBLE_SVG_PATH = "m 134 0 " +
                "c -74.618 0 -135.108 44.166 -135.108 99 " +
                "c 0 54.487 60.491 98.661 135.108 98.661 " +
                "c 25.915 5 120.12 -5.325 120.685 -14.562 " +
                "c 14.659 16.528 38.188 17.669 79.57 1.116 " +
                "c -31.339 -1.873 -34.936 -8.656 -42.138 -26.046 " +
                "c 16.944 -16.484 26.985 -36.969 26.985 -59.167 " +
                "c 0 -54.487 -60.484 -98.654 -135.102 -98.654 z";
        SVGPath[] bubbleShapes = new SVGPath[1];
        bubbleShapes[0] = new SVGPath();

        bubbleShapes[0].setContent(BUBBLE_SVG_PATH);
        bubbleShapes[0].setFill(Color.WHITE);
        bubbleShapes[0].setStroke(Color.BLACK);
        Insets margin = new Insets(20, 40, 20, 20);
        String textStyle = Sprite.getTextStyle(null);

        return new SVGSprite(bubbleShapes, textStyle, margin, null);
    }

}
