package info.deskchan.gui_javafx.panes;

import info.deskchan.gui_javafx.LocalFont;
import info.deskchan.gui_javafx.Main;
import info.deskchan.gui_javafx.OverlayStage;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Balloon extends MovablePane {

    private static List<Balloon> openedBalloons = new LinkedList<>();

    protected static BalloonDrawer getDrawer(String pathKey){
        BalloonDrawer drawer;
        String path = Main.getPluginProxy().getProperties().getString(pathKey);
        if (path == null || !new File(path).exists()){
            path = Main.getPluginProxy().getAssetsDirPath().resolve("bubble.svg").toString();
            if (!new File(path).exists()){
                return new SVGBalloonDrawer(null);
            }
        }

        if (SVGBalloonDrawer.canRead(path)){
            drawer = new SVGBalloonDrawer(path);
        } else if (ImageBalloonDrawer.canRead(path)){
            drawer = new ImageBalloonDrawer(path);
        } else {
            Main.log(new Exception("Cannot set file " + path + "as balloon skin, unsupported format"));
            drawer = new SVGBalloonDrawer(path);
        }
        return drawer;
    }

    protected static Font defaultFont = null;
    protected final DropShadow bubbleShadow = new DropShadow();
    protected float balloonOpacity = 100;
    protected BubblePane bubblePane;

    Balloon() {
        openedBalloons.add(this);

        bubbleShadow.setRadius(10.0);
        bubbleShadow.setOffsetX(1.5);
        bubbleShadow.setOffsetY(2.5);
        bubbleShadow.setColor(Color.color(0, 0, 0, Main.getProperties().getFloat("balloon.shadow-opacity", 1.0f)));
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

    void show() {
        OverlayStage.getInstance().showBalloon(this);
    }

    void hide() {
        OverlayStage.getInstance().hideBalloon(this);
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

    protected static class BubblePane extends StackPane {
        private Node bubble;
        private Node content;
        private Insets margin;

        BubblePane(Node bubbles, Node content, Insets margin){
            super();
            this.bubble = bubbles;
            this.content = content;
            this.margin = margin;

            StackPane contentPane = new StackPane();
            contentPane.getChildren().add(content);
            contentPane.prefHeightProperty().bind(prefHeightProperty());

            StackPane.setMargin(content, margin);
            getChildren().addAll(bubbles, contentPane);
        }

        public double getContentWidth(){
            return bubble.getLayoutBounds().getWidth() - margin.getRight() - margin.getLeft();
        }

        public double getBubbleWidth(){
            return bubble.getLayoutBounds().getWidth();
        }

        public double getBubbleHeight(){
            return bubble.getLayoutBounds().getHeight();
        }

        public void invert(boolean turn, CharacterBalloon.DirectionMode mode){
            switch (mode){
                case NO_DIRECTION:       return;
                case STANDARD_DIRECTION: break;
                case ALWAYS_INVERTED:    turn = true;  break;
                case INVERTED_DIRECTION: turn = !turn; break;
            }
            bubble.setScaleX(turn ? -1 : 1);

            Insets newMargin = new Insets(margin.getTop(), (!turn) ? margin.getRight() : margin.getLeft(),
                    margin.getBottom(), (turn) ? margin.getRight() : margin.getLeft());

            StackPane.setMargin(content, newMargin);
        }

        public void setOpacity(float opacity){
            bubble.setOpacity(opacity);
        }
    }

    static abstract class BalloonDrawer {

        protected Insets margin;
        protected String textStyle;

        abstract BalloonDrawer copy();

        abstract BubblePane createBalloon(Node content);

        protected static Document getDocument(String path){
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                return builder.parse(path);
            } catch (Exception e) {
                Main.log(e);
                return null;
            }
        }

        protected static Insets getMargin(Document document){
            Insets standard = new Insets(30, 30, 30, 30);
            if (document == null)
                return standard;

            try {
                XPathFactory xpf = XPathFactory.newInstance();
                XPath xpath = xpf.newXPath();
                XPathExpression expression = xpath.compile("//margin");

                NamedNodeMap marginTags = ((NodeList) expression.evaluate(document, XPathConstants.NODESET)).item(0).getAttributes();
                return new Insets(
                        Double.parseDouble(marginTags.getNamedItem("top").getTextContent()),
                        Double.parseDouble(marginTags.getNamedItem("right").getTextContent()),
                        Double.parseDouble(marginTags.getNamedItem("bottom").getTextContent()),
                        Double.parseDouble(marginTags.getNamedItem("left").getTextContent())
                );
            } catch (Exception e) {
                return standard;
            }
        }

        protected static String getTextStyle(Document document){
            String standard = "-fx-alignment: center; -fx-text-alignment: center; -fx-content-display: center;";
            if (document == null)
                return standard;

            try {
                XPathFactory xpf = XPathFactory.newInstance();
                XPath xpath = xpf.newXPath();
                XPathExpression expression = xpath.compile("//text");

                NamedNodeMap colorTag = ((NodeList) expression.evaluate(document, XPathConstants.NODESET)).item(0).getAttributes();
                return convertStyle(colorTag.getNamedItem("style").getTextContent());
            } catch (Exception e) {
                return standard;
            }
        }

        protected static String convertStyle(String style){
            String[] styleLines = style.split(";");
            StringBuilder result = new StringBuilder();
            for (int j = 0; j < styleLines.length; j++) {
                styleLines[j] = styleLines[j].trim();
                if (styleLines[j].length() == 0) continue;
                result.append("-fx-");
                result.append(styleLines[j].trim());
                result.append("; ");
            }
            return result.toString();
        }
    }

    static class SVGBalloonDrawer extends BalloonDrawer {

        private SVGPath[] bubbleShapes;

        private SVGBalloonDrawer(){ }

        public SVGBalloonDrawer(String path){
            Document document = getDocument(path);
            try {
                margin = getMargin(document);

                XPathFactory xpf = XPathFactory.newInstance();
                XPath xpath = xpf.newXPath();
                XPathExpression expression = xpath.compile("//path");

                NodeList svgPaths = (NodeList) expression.evaluate(document, XPathConstants.NODESET);
                ArrayList<SVGPath> shapes = new ArrayList<>();
                for(int i=0; i<svgPaths.getLength(); i++) {
                    try {
                        SVGPath shape = new SVGPath();
                        NamedNodeMap map = svgPaths.item(i).getAttributes();
                        shape.setContent(map.getNamedItem("d").getTextContent());
                        if(map.getNamedItem("style") != null) {
                            shape.setStyle(convertStyle(map.getNamedItem("style").getTextContent()));
                        } else {
                            shape.setStyle("-fx-fill: white; -fx-stroke-width: 2;");
                        }
                        shapes.add(shape);
                    } catch (Exception e){
                        Main.log(e);
                    }
                }
                bubbleShapes = shapes.toArray(new SVGPath[shapes.size()]);

                textStyle = getTextStyle(document);
            } catch (Exception e){
                Main.log("Balloon file not found, using default");

                String BUBBLE_SVG_PATH = "m 32.339338,-904.55632 c -355.323298,0 -643.374998,210.31657 " +
                        "-643.374998,469.78125 0,259.46468 288.0517,469.812505 643.374998,469.812505 123.404292,0 " +
                        "238.667342,-25.3559002 336.593752,-69.3438 69.80799,78.7043 181.84985,84.1354 378.90625,5.3126 " +
                        "-149.2328,-8.9191 -166.3627,-41.22 -200.6562,-124.031305 80.6876,-78.49713 128.5,-176.04496 " +
                        "128.5,-281.75 0,-259.46468 -288.0205,-469.78125 -643.343802,-469.78125 z";
                bubbleShapes = new SVGPath[1];
                bubbleShapes[0] = new SVGPath();

                bubbleShapes[0].setContent(BUBBLE_SVG_PATH);
                bubbleShapes[0].setFill(Color.WHITE);
                bubbleShapes[0].setStroke(Color.BLACK);

                bubbleShapes[0].setScaleX(0.3);
                bubbleShapes[0].setScaleY(0.23);

                margin = new Insets(40, 40, 20, 20);
            }
        }

        public BubblePane createBalloon(Node content){
            content.setStyle(textStyle);

            Group bubblesGroup = new Group(bubbleShapes);

            return new BubblePane(bubblesGroup, content, margin);
        }

        public BalloonDrawer copy(){
            SVGBalloonDrawer drawer = new SVGBalloonDrawer();
            drawer.bubbleShapes = bubbleShapes;
            drawer.margin = margin;
            return drawer;
        }

        public static boolean canRead(String path){
            return path.endsWith(".svg");
        }
    }

    static class ImageBalloonDrawer extends BalloonDrawer {
        public static boolean canRead(String path){
            try {
                Image image = new Image("file:///" + path);
                return image.getHeight() > 0 && image.getWidth() > 0;
            } catch (Exception e){
                return false;
            }
        }

        Image image;
        private ImageBalloonDrawer(){ }

        public ImageBalloonDrawer(String path){
            try {
                image = new Image(Paths.get(path).toUri().toString());
            } catch (Exception e){
                Main.log(e);
                return;
            }

            try {
                Document document = getDocument(path + ".config");
                margin = getMargin(document);
                textStyle = getTextStyle(document);
            } catch (Exception e){
                margin = getMargin(null);
                textStyle = getTextStyle(null);
            }
        }

        public BubblePane createBalloon(Node content){
            content.setStyle(textStyle);

            ImageView view = new ImageView();
            view.setImage(image);

            return new BubblePane(view, content, margin);
        }

        public BalloonDrawer copy(){
            ImageBalloonDrawer drawer = new ImageBalloonDrawer();
            drawer.image = image;
            drawer.margin = margin;
            return drawer;
        }

    }
}
