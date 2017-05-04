package info.deskchan.gui_javafx;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

import java.util.HashMap;
import java.util.Map;

class MouseEventNotificator {
    private String sender;

    MouseEventNotificator(String sender) {
        this.sender = sender;
    }

    void notifyMouseEvent(MouseEvent event) {
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
        Map<String, Object> m = new HashMap<>();
        m.put("deltaX", event.getDeltaX());
        m.put("deltaY", event.getDeltaY());

        String eventMessage = "gui-events:" + sender + "-scroll";
        Main.getInstance().getPluginProxy().sendMessage(eventMessage, m);
    }
}
