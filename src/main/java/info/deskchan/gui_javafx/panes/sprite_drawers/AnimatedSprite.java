package info.deskchan.gui_javafx.panes.sprite_drawers;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Parent;
import javafx.scene.effect.Effect;
import javafx.util.Duration;

import java.util.LinkedList;

public class AnimatedSprite<T extends Sprite> extends Parent implements EventHandler<ActionEvent> {

    private T mainSprite = null;
    private T swappingSprite = null;
    private LinkedList<AnimationData<T>> queue = new LinkedList<>();
    private Timeline timeline = new Timeline(new KeyFrame(Duration.millis(TIMELINE_DELAY), this));
    private static final int TIMELINE_DELAY = 20;
    private Effect effect;

    public AnimatedSprite(){
        timeline.setCycleCount(Timeline.INDEFINITE);
    }
    public AnimatedSprite(T origin){
        super();
        mainSprite = origin;
        getChildren().add(mainSprite);
    }

    public static class AnimationData<T> {
        public T next = null;
        public Point2D scaling = null;
        public Point3D moving = null;
        public Float rotation = null;
        public boolean smooth = false;
        public Float opacity = null;
        public int delay = 200;
        public AnimationData(){}
        public AnimationData(T sprite, boolean smooth){ next = sprite; this.smooth = smooth; }
    }

    public void dropAnimation(){
        queue.clear();
        currentAnimation = null;
        mainSprite.setOpacity(1);
        if (timeline.getStatus() == Animation.Status.RUNNING){
            timeline.stop();
        }
    }

    public void addAnimation(AnimationData<T> data){
        if (data == null) return;
        queue.add(data);
        if (mainSprite == null){
            setCurrentAnimation();
        }
        if (timeline.getStatus() != Animation.Status.RUNNING){
            timeline.play();
        }
    }

    private AnimationData<T> currentAnimation = null;

    @Override
    public void handle(javafx.event.ActionEvent actionEvent) {
        if (currentAnimation == null && !setCurrentAnimation()){
            timeline.stop();
            return;
        }

        currentAnimation.delay -= TIMELINE_DELAY;
        if (currentAnimation.smooth || currentAnimation.delay < 0){
            if (currentAnimation.opacity != null)
                mainSprite.setOpacity(mainSprite.getOpacity() + currentAnimation.opacity);
            if (swappingSprite != null){
                swappingSprite.setOpacity(swappingSprite.getOpacity() - currentAnimation.opacity);
            }
            if (currentAnimation.moving != null)
                mainSprite.setPosition(mainSprite.getPosition().add(new Point2D(currentAnimation.moving.getX(), currentAnimation.moving.getY())));
            if (currentAnimation.rotation != null)
                mainSprite.setRotate(mainSprite.getRotate() + currentAnimation.rotation);
            if (currentAnimation.scaling != null) {
                mainSprite.setScaleX(mainSprite.getScaleX() + currentAnimation.scaling.getX());
                mainSprite.setScaleX(mainSprite.getScaleX() + currentAnimation.scaling.getY());
            }
        }
        if (currentAnimation.delay < 0)
            currentAnimation = null;
    }

    private boolean setCurrentAnimation(){
        if (queue.isEmpty()){
            return false;
        }

        currentAnimation = queue.removeFirst();
        if (swappingSprite != null)
            getChildren().remove(swappingSprite);
        swappingSprite = null;
        if (currentAnimation.smooth){
            float mul = (float) TIMELINE_DELAY / currentAnimation.delay;
            if (currentAnimation.moving != null) currentAnimation.moving.multiply(mul);
            if (currentAnimation.scaling != null) currentAnimation.scaling.multiply(mul);
            if (currentAnimation.rotation != null) currentAnimation.rotation *= mul;
            if (currentAnimation.opacity != null) currentAnimation.opacity /= mul;
        }
        if (currentAnimation.next != null){
            swappingSprite = mainSprite;
            mainSprite = currentAnimation.next;
            getChildren().add(mainSprite);
            mainSprite.setEffect(effect);
            mainSprite.setOpacity(0);
            if (swappingSprite != null) {
                swappingSprite.setOpacity(1);
                mainSprite.setFitHeight(swappingSprite.getFitHeight());
                mainSprite.setFitWidth(swappingSprite.getFitWidth());
            }
            if (currentAnimation.smooth)
                currentAnimation.opacity = (float) TIMELINE_DELAY / currentAnimation.delay;
            else
                currentAnimation.opacity = 1F;
        }
        return true;
    }

    public double getOriginWidth(){
        return mainSprite != null ? mainSprite.getOriginWidth() : 0;
    }

    public double getOriginHeight(){
        return mainSprite != null ?   mainSprite.getOriginHeight() : 0;
    }

    public double getFitWidth(){
        return mainSprite.getFitWidth();
    }

    public double getFitHeight(){
        return mainSprite.getFitHeight();
    }

    public void setFitWidth(double width){
        mainSprite.setFitWidth(width);
    }

    public void setFitHeight(double height){
        mainSprite.setFitHeight(height);
    }

    public void applyEffect(Effect effect){
        this.effect = effect;
        mainSprite.setEffect(null);
        mainSprite.setEffect(effect);
    }

    public T getCurrentSprite(){ return mainSprite; }

}
