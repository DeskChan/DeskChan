package info.deskchan.gui_javafx.panes.sprite_drawers;

import info.deskchan.MessageData.GUI.SetSprite;
import info.deskchan.core.MessageDataMap;
import info.deskchan.gui_javafx.Main;
import info.deskchan.gui_javafx.panes.MovablePane;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AnimatedSprite extends MovablePane implements EventHandler<ActionEvent> {

    private Sprite mainSprite = null;
    private Sprite swappingSprite = null;
    private LinkedList<AnimationData> queue = new LinkedList<>();
    private Timeline timeline;
    private static final int TIMELINE_DELAY = Main.getProperties().getInteger("sprites-animation-delay", 20);

    public AnimatedSprite(){
        timeline = new Timeline(new KeyFrame(Duration.millis(TIMELINE_DELAY), this));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }
    public AnimatedSprite(Sprite origin){
        this();
        mainSprite = origin;
        getChildren().add(mainSprite);
    }

    public static class AnimationData extends SetSprite.Animation {

        public Sprite nextSprite = null;
        protected float changeSpriteOpacity = 0;

        public AnimationData(){
            super();
        }

        public AnimationData(Map<String, Object> dat){
            super();
            MessageDataMap data = new MessageDataMap(dat);
            if (dat.containsKey("movingX")) setMovingX(data.getFloat("movingX"));
            if (dat.containsKey("movingY")) setMovingY(data.getFloat("movingY"));

            if (dat.containsKey("scaleX")) setScalingX(data.getFloat("scaleX"));
            if (dat.containsKey("scaleY")) setScalingY(data.getFloat("scaleY"));

            if (dat.containsKey("rotation")) setRotation(data.getFloat("rotation"));

            if (dat.containsKey("smooth")) setSmooth(data.getBoolean("smooth"));
            if (dat.containsKey("delay")) setDelay(data.getLong("delay"));
            if (dat.containsKey("opacity")) setOpacity(data.getFloat("opacity"));

            if (data.containsKey("next")) {
                try {
                    nextSprite = Sprite.getSpriteFromFile(data.getFile("next"));
                } catch (Exception e) {
                    Main.log(e);
                }
            }
        }
    }

    public void dropAnimation(){
        queue.clear();
        currentAnimation = null;
        if (mainSprite != null)
            mainSprite.setOpacity(1);
        if (swappingSprite != null)
            getChildren().remove(swappingSprite);
        swappingSprite = null;

        timeline.stop();
        setScaleX(1);
        setScaleY(1);
        setLayoutX(0);
        setLayoutY(0);
        setRotate(0);
        setOpacity(1);
    }

    public void addAnimation(AnimationData data){
        if (data == null) return;
        queue.add(data);
        if (mainSprite == null){
            setCurrentAnimation();
        }

        if (timeline.getStatus() != Animation.Status.RUNNING){
            timeline.play();
        }
    }

    private AnimationData currentAnimation = null;

    @Override
    public void handle(javafx.event.ActionEvent actionEvent) {
        if (currentAnimation == null && !setCurrentAnimation()){
            timeline.stop();
            return;
        }

        currentAnimation.setDelay(currentAnimation.getDelay() - TIMELINE_DELAY);
        if (currentAnimation.getSmooth() || currentAnimation.getDelay() < 0){
            mainSprite.setOpacity(mainSprite.getOpacity() + currentAnimation.changeSpriteOpacity * 1.5);
            if (swappingSprite != null){
                swappingSprite.setOpacity(swappingSprite.getOpacity() - currentAnimation.changeSpriteOpacity);
            }
            if (currentAnimation.getOpacity() != null)
                setOpacity(getOpacity() + currentAnimation.getOpacity());
            setLayoutX(getLayoutX() + currentAnimation.getMovingX());
            setLayoutY(getLayoutY() + currentAnimation.getMovingY());
            setRotate(getRotate() + currentAnimation.getRotation());
            setScaleX(getScaleX() + currentAnimation.getScalingX());
            setScaleY(getScaleY() + currentAnimation.getScalingY());

        }
        if (currentAnimation.getDelay() < 0) {
            currentAnimation = null;
            notify(onSpriteHandlers);
        }
    }

    private boolean setCurrentAnimation(){
        if (queue.isEmpty()){
            return false;
        }

        currentAnimation = queue.removeFirst();
        if (swappingSprite != null)
            getChildren().remove(swappingSprite);
        swappingSprite = null;
        if (currentAnimation.getSmooth()){
            float mul = (float) TIMELINE_DELAY / currentAnimation.getDelay();
            currentAnimation.setMovingX(currentAnimation.getMovingX() * mul);
            currentAnimation.setMovingY(currentAnimation.getMovingY() * mul);
            //currentAnimation.setMovingZ(currentAnimation.getMovingZ() * mul);
            currentAnimation.setScalingX(currentAnimation.getScalingX() * mul);
            currentAnimation.setScalingY(currentAnimation.getScalingY() * mul);
            currentAnimation.setRotation(currentAnimation.getRotation() * mul);
            currentAnimation.setOpacity(currentAnimation.getOpacity() * mul);
        }
        if (currentAnimation.nextSprite != null){
            swappingSprite = mainSprite;
            mainSprite = currentAnimation.nextSprite;
            getChildren().add(mainSprite);
            mainSprite.setOpacity(0);

            if (swappingSprite != null) {
                swappingSprite.setOpacity(1);
                mainSprite.setFitHeight(swappingSprite.getFitHeight());
                mainSprite.setFitWidth(swappingSprite.getFitWidth());
            }
            if (currentAnimation.getSmooth())
                currentAnimation.changeSpriteOpacity = (float) TIMELINE_DELAY / currentAnimation.getDelay();
            else
                currentAnimation.changeSpriteOpacity = 1F;
        }
        return true;
    }

    public double getOriginWidth(){
        return mainSprite != null ? mainSprite.getOriginWidth() : 0;
    }

    public double getOriginHeight(){
        return mainSprite != null ? mainSprite.getOriginHeight() : 0;
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

    public Sprite getCurrentSprite(){ return mainSprite; }

    public List<Runnable> onSpriteHandlers = new LinkedList<>();

    private void notify(List<Runnable> handlers){
        for (Runnable handler : handlers)
            handler.run();
    }
}
