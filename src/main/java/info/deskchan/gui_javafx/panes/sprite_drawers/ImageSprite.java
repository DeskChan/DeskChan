package info.deskchan.gui_javafx.panes.sprite_drawers;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.w3c.dom.Document;

import java.nio.file.Paths;

public class ImageSprite extends Sprite{

    public static boolean canRead(String path){
        try {
            Image image = new Image("file:///" + path);
            return image.getHeight() > 0 && image.getWidth() > 0;
        } catch (Exception e){
            return false;
        }
    }

    public static Sprite create(String path) throws Exception {
        Image image = new Image(Paths.get(path).toUri().toString());

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

        return new ImageSprite(view, textStyle, margin);
    }

    public ImageSprite(Node sprite, String contentStyle, Insets margin) {
        super(sprite, contentStyle, margin);
    }

    public ImageSprite(Image sprite) {
        super(new ImageView(sprite), null, null);
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
}
