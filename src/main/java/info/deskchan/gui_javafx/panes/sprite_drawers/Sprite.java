package info.deskchan.gui_javafx.panes.sprite_drawers;

import info.deskchan.gui_javafx.panes.CharacterBalloon;
import info.deskchan.gui_javafx.panes.MovablePane;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.effect.Effect;
import javafx.scene.layout.StackPane;
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
import java.io.FileNotFoundException;
import java.io.InvalidObjectException;
import java.nio.file.InvalidPathException;

public abstract class Sprite extends MovablePane {

    protected Node sprite;
    protected Insets margin;
    protected Effect effect;

    protected Node spriteContent = null;
    protected String contentStyle;

    Sprite(Node sprite, String contentStyle, Insets margin){
        super();
        this.sprite = sprite;
        this.margin = margin;
        this.contentStyle = contentStyle;

        getChildren().add(this.sprite);
    }

    public void setSpriteContent(Node content){
        if (spriteContent != null){
            getChildren().remove(this.spriteContent.getParent());
        }
        spriteContent = content;
        if (spriteContent != null) {
            StackPane contentPane = new StackPane();
            contentPane.getChildren().add(spriteContent);
            spriteContent.setStyle(contentStyle);
            applyMargin();
            getChildren().add(contentPane);
        }
    }

    public static Sprite getSpriteFromFile(File path) throws Exception {
        if (!path.exists()) throw new FileNotFoundException("Not found: "+path);
        Sprite sprite;
        if (SVGSprite.canRead(path)) {
            sprite = SVGSprite.create(path);
        } else if (ImageSprite.canRead(path)) {
            sprite = ImageSprite.create(path);
        } else if (path.toString().contains(" ") || path.toString().contains("#")){
            throw new InvalidPathException(path.toString(), "Path of file contains invalid symbols such as spaces or sharps");
        } else {
            throw new InvalidObjectException("Cannot load sprite from file "+ path + ", unknown type of file");
        }

        return sprite;
    }

    public double getContentWidth(){
        return getOriginWidth() - margin.getRight() - margin.getLeft();
    }

    public abstract double getOriginWidth();

    public abstract double getOriginHeight();

    public String getSpritePath(){ return null; }

    public void invert(boolean turn, CharacterBalloon.DirectionMode mode){
        switch (mode){
            case NO_DIRECTION:       return;
            case STANDARD_DIRECTION: break;
            case ALWAYS_INVERTED:    turn = true;  break;
            case INVERTED_DIRECTION: turn = !turn; break;
        }
        sprite.setScaleX(turn ? -1 : 1);

        applyMargin();
    }

    private void applyMargin(){
        if (spriteContent == null)
            return;

        boolean turn = sprite.getScaleX() < 0;
        Insets newMargin = new Insets(margin.getTop(), (!turn) ? margin.getRight() : margin.getLeft(),
                margin.getBottom(), (turn) ? margin.getRight() : margin.getLeft());

        StackPane parent = (StackPane) spriteContent.getParent();
        parent.setPrefHeight(getFitHeight());
        parent.setMaxWidth(getFitWidth());
        StackPane.setMargin(spriteContent, newMargin);
    }

    public abstract double getFitWidth();

    public abstract double getFitHeight();

    public abstract void setFitWidth(double width);

    public abstract void setFitHeight(double height);

    public void applyEffect(Effect effect){
        this.effect = effect;
        sprite.setEffect(null);
        sprite.setEffect(effect);
    }

    protected static Document getDocument(String path){
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new File(path).toURI().toURL().toString());
        } catch (Exception e) {
            return null;
        }
    }

    protected static Insets getMarginFromFile(Document document){
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

    public static String getTextStyle(Document document){
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
