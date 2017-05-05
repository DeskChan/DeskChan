package info.deskchan.gui_javafx;

import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import org.apache.commons.lang3.SystemUtils;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.mouse.NativeMouseWheelEvent;
import org.jnativehook.mouse.NativeMouseWheelListener;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

class MouseEventNotificator {
    private Node sender;
    private String senderName;

    private NativeMouseWheelListener mouseWheelListener;

    static {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
    }

    MouseEventNotificator(Node sender, String senderName) {
        this.sender = sender;
        this.senderName = senderName;
    }

    void notifyClickEvent(MouseEvent event) {
        if (!event.isStillSincePress()) {
            return;
        }

        Map<String, Object> m = new HashMap<>();
        m.put("x", event.getScreenX());
        m.put("y", event.getScreenY());

        StringBuilder eventMessage = new StringBuilder("gui-events:").append(senderName).append("-");
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

    void notifyScrollEvent(NativeMouseWheelEvent event, Function<NativeMouseWheelEvent, Boolean> intersectionTestFunc) {
        double senderX = sender.getLayoutX();
        double senderY = sender.getLayoutY();
        double x = event.getX();
        double y = event.getY();

        Main.log(String.format("X: %f, Y: %f, senderX: %f, senderY: %f.", x, y, senderX, senderY));
        if (sender.contains(x - senderX, y - senderY)) {
            if (intersectionTestFunc.apply(event)) {
                impl_notifyScrollEvent(event.getWheelRotation());
            }
        }
    }

    private void impl_notifyScrollEvent(int delta) {
        Map<String, Object> m = new HashMap<>();
        m.put("delta", delta);

        String eventMessage = "gui-events:" + senderName + "-scroll";
        Main.getInstance().getPluginProxy().sendMessage(eventMessage, m);
    }

    MouseEventNotificator setOnClickListener() {
        sender.addEventFilter(MouseEvent.MOUSE_CLICKED, this::notifyClickEvent);
        return this;
    }

    MouseEventNotificator setOnScrollListener(Function<NativeMouseWheelEvent, Boolean> intersectionTestFunc) {
        if (SystemUtils.IS_OS_WINDOWS) {
            if (!GlobalScreen.isNativeHookRegistered()) {
                try {
                    GlobalScreen.registerNativeHook();
                } catch (NativeHookException | UnsatisfiedLinkError e) {
                    e.printStackTrace();
                    Main.log("Failed to initialize the native hooking. Rolling back to using JavaFX events...");
                    sender.addEventFilter(ScrollEvent.SCROLL, this::notifyScrollEvent);
                    return this;
                }
            }
            mouseWheelListener = event -> this.notifyScrollEvent(event, intersectionTestFunc);
            GlobalScreen.addNativeMouseWheelListener(mouseWheelListener);
        } else {
            sender.addEventFilter(ScrollEvent.SCROLL, this::notifyScrollEvent);
        }

        return this;
    }

    void cleanListeners() {
        // All methods have their own internal checks for the case when a filter is not set and equals null.
        sender.removeEventFilter(MouseEvent.MOUSE_CLICKED, this::notifyClickEvent);
        sender.removeEventFilter(ScrollEvent.SCROLL, this::notifyScrollEvent);
        GlobalScreen.removeNativeMouseWheelListener(mouseWheelListener);
    }
}
