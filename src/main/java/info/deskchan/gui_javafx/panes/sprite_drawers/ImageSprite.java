package info.deskchan.gui_javafx.panes.sprite_drawers;

import info.deskchan.gui_javafx.Main;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.w3c.dom.Document;

import java.io.File;
import java.io.FileInputStream;

public class ImageSprite extends Sprite {

    private String path = null;

    public static boolean canRead(File path){
        try {
            Image image = new Image(new FileInputStream(path));
            return image.getHeight() > 0 && image.getWidth() > 0;
        } catch (Exception e){
            Main.log(e);
            return false;
        }
    }

    public static ImageSprite create(File path) throws Exception {
        Image image = new Image(new FileInputStream(path));

        Insets margin;
        String textStyle;
        try {
            Document document = getDocument(path + ".config");
            margin = getMarginFromFile(document);
            textStyle = getTextStyle(document);
        } catch (Exception e){
            margin = getMarginFromFile(null);
            textStyle = getTextStyle(null);
        }

        ImageView view = new ImageView();
        view.setImage(image);

        return new ImageSprite(view, textStyle, margin, path);
    }

    private ImageSprite(Node sprite, String contentStyle, Insets margin, File path) {
        super(sprite, contentStyle, margin);
        this.path = path != null ? path.toString() : null;
    }

    public double getOriginWidth(){
        return toImageView().getImage() != null ? toImageView().getImage().getWidth() : 0;
    }

    public double getOriginHeight(){
        return toImageView().getImage() != null ? toImageView().getImage().getHeight() : 0;
    }

    public double getFitWidth() {  return toImageView().getFitWidth();   }

    public double getFitHeight(){  return toImageView().getFitHeight();  }

    public void setFitWidth(double width)  {  toImageView().setFitWidth(width);    }

    public void setFitHeight(double height){  toImageView().setFitHeight(height);  }

    public ImageView toImageView(){ return (ImageView) sprite; }

    public String getSpritePath(){ return path; }

}
