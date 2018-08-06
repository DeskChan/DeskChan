package info.deskchan.gui_javafx.panes;

import info.deskchan.gui_javafx.Main;
import info.deskchan.gui_javafx.MouseEventNotificator;
import info.deskchan.gui_javafx.panes.sprite_drawers.AnimatedSprite;
import info.deskchan.gui_javafx.panes.sprite_drawers.Sprite;
import javafx.geometry.Point2D;

import java.io.File;
import java.util.*;

public class ControllableSprite extends MovablePane {

    public enum SpriteActionType {
        CREATE,
        SHOW,
        HIDE,
        DELETE,
        ANIMATE,
        DROP_ANIMATION
    }

    private static Map<String, ControllableSprite> registeredSprites = new HashMap<>();

    final String id;
    final String owner;
    final AnimatedSprite sprite;

    public ControllableSprite(String owner, String id, File imageFile) {
        this.owner = owner;
        this.id = id;
        AnimatedSprite s;
        try {
            s = new AnimatedSprite(Sprite.getSpriteFromFile(imageFile));
        } catch (Exception e) {
            Main.log(e);
            sprite = null;
            return;
        }
        sprite = s;
        ControllableSprite last = registeredSprites.put(getFullName(), this);
        if (last != null)
            last.destroy();
        getChildren().add(sprite);

        MouseEventNotificator mouseEventNotificator = new MouseEventNotificator(this, getFullName());
        mouseEventNotificator.setOnClickListener().setOnMovedListener();


    }

    String getFullName() {
        return owner + "-" + id;
    }

    public void addAnimation(AnimatedSprite.AnimationData data){
        sprite.addAnimation(data);
    }

    public void dropAnimation(){ sprite.dropAnimation(); }

    public void destroy() {
        hide();
        registeredSprites.remove(getFullName());
    }

    public void setDraggable(boolean draggable){
        if (draggable) {
            setOnMousePressed(event -> {
                startDrag(event);
            });
        } else {
            setOnMousePressed(null);
        }
    }

    public void setDefaultPosition(){
        setPosition(new Point2D(getLayoutX(), getLayoutY()));
    }

    public static Collection<ControllableSprite> getSprites() {
        return registeredSprites.values();
    }

    public static Collection<ControllableSprite> getSprites(String owner) {
        List<ControllableSprite> result = new LinkedList<>();
        for (ControllableSprite sprite : registeredSprites.values())
            if (sprite.owner.equals(owner)) result.add(sprite);

        return result;
    }

    public static ControllableSprite getSprite(String owner, String name) {
        return registeredSprites.get(owner+"-"+name);
    }

}
