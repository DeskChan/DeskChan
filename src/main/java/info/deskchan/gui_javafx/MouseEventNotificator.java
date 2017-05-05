package info.deskchan.gui_javafx;

import javafx.geometry.Point2D;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import org.jnativehook.mouse.NativeMouseWheelEvent;

import java.util.HashMap;
import java.util.Map;

class MouseEventNotificator {
    private String sender;

    MouseEventNotificator(String sender) {
        this.sender = sender;
    }

    void notifyMouseEvent(MouseEvent event) {
        if (!event.isStillSincePress()) {
            return;
        }

        Map<String, Object> m = new HashMap<>();
        m.put("x", event.getScreenX());
        m.put("y", event.getScreenY());

        StringBuilder eventMessage = new StringBuilder("gui-events:").append(sender).append("-");
        if (event.getButton() == MouseButton.PRIMARY) {
            if (event.getClickCount() == 2) {
                eventMessage.append("double");
            } else {
                eventMessage.append("left");
            }
        } else if (event.getButton() == MouseButton.SECONDARY) {
            eventMessage.append("right");
        } else if (event.getButton() == MouseButton.MIDDLE) {
            eventMessage.append("middle");
        } else {
            return;
        }
        eventMessage.append("-click");

        Main.getInstance().getPluginProxy().sendMessage(eventMessage.toString(), m);
    }

    void notifyScrollEvent(ScrollEvent event) {
        int delta = (event.getDeltaY() > 0) ? 1 : -1;
        impl_notifyScrollEvent(delta);
    }

    void notifyScrollEvent(NativeMouseWheelEvent event) {
        Character character = App.getInstance().getCharacter();
        Point2D characterPosition = character.getPosition();
        double charX1 = characterPosition.getX();
        double charX2 = charX1 + character.getWidth();
        double charY1 = characterPosition.getY();
        double charY2 = charY1 + character.getHeight();
        double x = event.getX();
        double y = event.getY();

        if (x > charX1 && x < charX2 && y > charY1 && y < charY2) {
            PixelReader imagePixels = character.getImage().getPixelReader();
            Color pixelColor = imagePixels.getColor((int) (x - charX1), (int) (y - charY1));
            if (!pixelColor.equals(Color.TRANSPARENT)) {
                impl_notifyScrollEvent(event.getWheelRotation());
            }
        }
    }

    private void impl_notifyScrollEvent(int delta) {
        Map<String, Object> m = new HashMap<>();
        m.put("delta", delta);

        String eventMessage = "gui-events:" + sender + "-scroll";
        Main.getInstance().getPluginProxy().sendMessage(eventMessage, m);
    }
}
