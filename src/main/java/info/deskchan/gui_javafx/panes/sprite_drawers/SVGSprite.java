package info.deskchan.gui_javafx.panes.sprite_drawers;

import info.deskchan.gui_javafx.Main;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.shape.SVGPath;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.ArrayList;

public class SVGSprite extends Sprite {

    private String path = null;

    public static boolean canRead(File path){
        return path.getName().endsWith(".svg");
    }

    public static SVGSprite create(File path) throws Exception {

        Document document = getDocument(path.toString());

        Insets margin = getMarginFromFile(document);

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
            } catch (Exception e) {
                Main.log(e);
            }
        }
        SVGPath[] shapesPaths = shapes.toArray(new SVGPath[shapes.size()]);

        String textStyle = getTextStyle(document);

        return new SVGSprite(shapesPaths, textStyle, margin, path);
    }

    private SVGPath[] svgParts;
    private double originWidth, originHeight;

    public SVGSprite(SVGPath[] shapes, String contentStyle, Insets margin, File path) {
        super(new Group(shapes), contentStyle, margin);
        svgParts = shapes;
        originWidth = getFitWidth();
        originHeight = getFitHeight();
        this.path = path != null ? path.toString() : null;
    }

    public double getOriginWidth(){
        return originWidth;
    }

    public double getOriginHeight(){
        return originHeight;
    }

    public double getFitWidth() {  return sprite.getLayoutBounds().getWidth();   }

    public double getFitHeight(){  return sprite.getLayoutBounds().getHeight();  }

    public void setFitWidth(double width)  {
        for (SVGPath path : svgParts)
            path.setScaleX(width / originWidth);
    }

    public void setFitHeight(double height){
        for (SVGPath path : svgParts)
            path.setScaleY(height / originWidth);
    }

    public String getSpritePath(){ return path; }
}
