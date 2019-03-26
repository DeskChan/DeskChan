package info.deskchan.gui_javafx.panes.sprite_drawers;

import javafx.scene.Group;
import javafx.scene.Node;

import java.util.Collection;

public class StackSprite extends Sprite {

    private final Collection<Node> sprites;

    private double originWidth, originHeight;

    public StackSprite(Collection<Node> sprites) {
        super(new Group(sprites), getTextStyle(null), getMarginFromFile(null));
        this.sprites = sprites;
        originWidth = getFitWidth();
        originHeight = getFitHeight();
    }

    public double getOriginWidth(){
        return originWidth;
    }

    public double getOriginHeight(){
        return originHeight;
    }

    public double getFitWidth() {  return getBoundsInParent().getWidth();   }

    public double getFitHeight(){  return getBoundsInParent().getHeight();  }

    public void setFitWidth(double width)  {
        setScaleX(width / originWidth);
    }

    public void setFitHeight(double height){
        setScaleY(height / originHeight);
    }

}
