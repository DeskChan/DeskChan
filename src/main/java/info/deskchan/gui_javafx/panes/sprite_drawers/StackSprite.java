package info.deskchan.gui_javafx.panes.sprite_drawers;

import javafx.scene.Group;
import javafx.scene.Node;

import java.util.Collection;

public class StackSprite extends Sprite {

    private String path = null;

    private double originWidth, originHeight;

    public StackSprite(Collection<Node> sprites) {
        super(new Group(sprites), getTextStyle(null), getMarginFromFile(null));
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
        setScaleX(width / originWidth);
    }

    public void setFitHeight(double height){
        setScaleY(height / originWidth);
    }

}
