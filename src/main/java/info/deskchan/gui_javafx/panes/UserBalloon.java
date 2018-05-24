package info.deskchan.gui_javafx.panes;

import info.deskchan.gui_javafx.App;
import info.deskchan.gui_javafx.LocalFont;
import info.deskchan.gui_javafx.Main;
import info.deskchan.gui_javafx.OverlayStage;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.HashMap;

public class UserBalloon extends Balloon {

    private static UserBalloon instance;

    public static UserBalloon getInstance(){ return instance; }

    protected final TextInputControl content;

    protected static BalloonDrawer drawer;

    public static void updateDrawer(){
        drawer = getDrawer("balloon.path-user");
    }

    BalloonDialog dialog;

    public UserBalloon() {
        super();
        instance = this;

        //TextArea label = new TextArea("");
        TextField label = new TextField("");
        label.setFont(defaultFont);
        if (defaultFont != null) {
            label.setFont(defaultFont);
        } else {
            label.setFont(LocalFont.defaultFont);
        }

        BorderPane pane = new BorderPane();
        pane.setCenter(label);
        pane.setMaxHeight(100);
        content = label;

        Button sendButton = new Button(Main.getString("send"));
        Button closeButton = new Button(Main.getString("close"));
        HBox buttons = new HBox(sendButton, closeButton);
        buttons.setAlignment(Pos.CENTER);
        buttons.setSpacing(10);
        pane.setBottom(buttons);

        bubblePane = drawer.createBalloon(pane);
        getChildren().add(bubblePane);

        label.setPrefWidth(bubblePane.getContentWidth());

        setBalloonScaleFactor(Main.getProperties().getFloat("balloon.scale_factor", 100));
        setBalloonOpacity(Main.getProperties().getFloat("balloon.opacity", 100));

        label.setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ENTER) sendPhrase();
        });

        sendButton.setOnAction(event -> {
            sendPhrase();
        });

        closeButton.setOnAction(event -> {
            close();
        });

        setOnMousePressed(event -> {
            startDrag(event);
        });

        dialog = new BalloonDialog(this);
    }

    protected void sendPhrase(){
        final String text = content.getText();
        new Thread(() -> {
            Main.getPluginProxy().sendMessage("DeskChan:user-said", new HashMap<String, Object>() {{
                put("value", text);
            }});
        }).start();
        Platform.runLater(this::close);
    }

    @Override
    public void relocate(double x, double y){
        dialog.relocate(x, y);
    }

    @Override
    public void setDefaultPosition() {
        setPosition(
           new Point2D(
                   (OverlayStage.getDesktopSize().getWidth() - bubblePane.getBubbleWidth()) / 2,
                   (OverlayStage.getDesktopSize().getHeight() - bubblePane.getBubbleHeight()) / 2
           )
        );
    }

    public static void show(String text){
        Platform.runLater(() -> {
            if (instance == null)
                new UserBalloon();

            instance.content.setText(text);
            instance.show();
        });
    }

    void close() {
        getChildren().removeAll();
        hide();
        instance = null;
    }

    void show() { dialog.show(); }

    void hide() {
        dialog.close();
    }

    static class BalloonDialog {
        Stage dialog;
        BalloonDialog(Balloon balloon){
            dialog = new Stage();
            dialog.getIcons().add(new Image(App.ICON_URL.toString()));
            dialog.initStyle(StageStyle.TRANSPARENT);
            dialog.initOwner(null);

            Scene scene = new Scene(balloon);
            balloon.setStyle("-fx-background-color: transparent;");
            scene.setFill(Color.TRANSPARENT);
            balloon.setMinWidth(balloon.bubblePane.getBubbleWidth());
            balloon.setMinHeight(balloon.bubblePane.getBubbleHeight());

            dialog.setScene(scene);
        }

        public void relocate(double x, double y){
            dialog.setX(x);
            dialog.setY(y);
        }

        void show() {
            dialog.show();
            dialog.requestFocus();
            dialog.toFront();
        }

        void close() {
            dialog.close();
        }
    }
}
