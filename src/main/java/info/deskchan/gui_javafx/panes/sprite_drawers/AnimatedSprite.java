package info.deskchan.gui_javafx.panes.sprite_drawers;

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

    public static class AnimationData {
        public Sprite next = null;
        public Float scalingX = null;
        public Float scalingY = null;
        public float movingX = 0F;
        public float movingY = 0F;
        public float movingZ = 0F;
        public Float rotation = null;
        public boolean smooth = false;
        public Float opacity = null;
        public long delay = 200;
        protected float changeSpriteOpacity = 0;
        public AnimationData(){}
        public AnimationData(Map<String, Object> dat){
            MessageDataMap data = new MessageDataMap(dat);
            movingX = data.getFloat("movingX", 0);
            movingY = data.getFloat("movingY", 0);

            scalingX = data.getFloat("scaleX");
            scalingY = data.getFloat("scaleY");

            rotation = data.getFloat("rotation");

            smooth = data.getBoolean("smooth", smooth);
            delay = data.getLong("delay", delay);
            opacity = data.getFloat("opacity");

            if (data.containsKey("next")) {
                try {
                    next = Sprite.getSpriteFromFile(data.getFile("next"));
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

        currentAnimation.delay -= TIMELINE_DELAY;
        if (currentAnimation.smooth || currentAnimation.delay < 0){
            mainSprite.setOpacity(mainSprite.getOpacity() + currentAnimation.changeSpriteOpacity * 1.5);
            if (swappingSprite != null){
                swappingSprite.setOpacity(swappingSprite.getOpacity() - currentAnimation.changeSpriteOpacity);
            }
            if (currentAnimation.opacity != null)
                setOpacity(getOpacity() + currentAnimation.opacity);
            setLayoutX(getLayoutX() + currentAnimation.movingX);
            setLayoutY(getLayoutY() + currentAnimation.movingY);
            if (currentAnimation.rotation != null)
                setRotate(getRotate() + currentAnimation.rotation);
            if (currentAnimation.scalingX != null)
                setScaleX(getScaleX() + currentAnimation.scalingX);
            if (currentAnimation.scalingY != null)
                setScaleY(getScaleY() + currentAnimation.scalingY);

        }
        if (currentAnimation.delay < 0) {
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
        if (currentAnimation.smooth){
            float mul = (float) TIMELINE_DELAY / currentAnimation.delay;
            currentAnimation.movingX *= mul;
            currentAnimation.movingY *= mul;
            currentAnimation.movingZ *= mul;
            if (currentAnimation.scalingX != null) currentAnimation.scalingX *= mul;
            if (currentAnimation.scalingY != null) currentAnimation.scalingY *= mul;
            if (currentAnimation.rotation != null) currentAnimation.rotation *= mul;
            if (currentAnimation.opacity != null) currentAnimation.opacity *= mul;
        }
        if (currentAnimation.next != null){
            swappingSprite = mainSprite;
            mainSprite = currentAnimation.next;
            getChildren().add(mainSprite);
            mainSprite.setOpacity(0);

            if (swappingSprite != null) {
                swappingSprite.setOpacity(1);
                mainSprite.setFitHeight(swappingSprite.getFitHeight());
                mainSprite.setFitWidth(swappingSprite.getFitWidth());
            }
            if (currentAnimation.smooth)
                currentAnimation.changeSpriteOpacity = (float) TIMELINE_DELAY / currentAnimation.delay;
            else
                currentAnimation.changeSpriteOpacity = 1F;
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

    public Sprite getCurrentSprite(){ return mainSprite; }

    public List<Runnable> onSpriteHandlers = new LinkedList<>();

    private void notify(List<Runnable> handlers){
        for (Runnable handler : handlers)
            handler.run();
    }
}
