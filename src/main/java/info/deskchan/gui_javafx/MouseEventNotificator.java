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

/**
 * This class provides some convenient means for Nodes to help them to handle mouse events.
 * For now, it processes different types of clicks and scrolling by the mouse wheel.
 */
class MouseEventNotificator {
    private Node sender;
    private String senderName;

    private NativeMouseWheelListener mouseWheelListener;

    // Gets rid of all garbage messages from the GlobalScreen of JNativeHook.
    static {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
    }

    /**
     * @param sender an event target Node
     * @param senderName a name that will be used as a component of `gui-events:*` messages
     */
    MouseEventNotificator(Node sender, String senderName) {
        this.sender = sender;
        this.senderName = senderName;
    }

    /**
     * Use this method to process a click event and send a special GUI event by the means of the Message System.
     * @param event MouseEvent. A click event
     */
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

    /**
     * Use this method to process a scroll event and send a special GUI event by the means of the Message System.
     * @param event ScrollEvent
     */
    void notifyScrollEvent(ScrollEvent event) {
        int delta = (event.getDeltaY() > 0) ? 1 : -1;
        impl_notifyScrollEvent(delta);
    }

    /**
     * This is a special method for Windows. Since Windows allows scrolling only for active windows (not windows
     * beneath the cursor, like Linux does), we need to use a special library to call WinAPI Hooks via JNI.
     * It fixes the problem but have another: events propagates globally for a whole screen. I limit the handling
     * area to the rectangle around a node. But in most cases it's not enough.
     * You must pass a lambda function (or an instance of Function explicitly if you want for whatever reason) as
     * the second parameter. It takes an event object and must return true if the check is passed successfully and
     * the event should be propagate further, and false otherwise.
     * @param event NativeMouseWheelEvent. A scroll event
     * @param intersectionTestFunc a function that takes an event object and must return boolean
     */
    void notifyScrollEvent(NativeMouseWheelEvent event, Function<NativeMouseWheelEvent, Boolean> intersectionTestFunc) {
        double senderX = sender.getLayoutX();
        double senderY = sender.getLayoutY();
        double x = event.getX();
        double y = event.getY();

        if (sender.contains(x - senderX, y - senderY)) {
            if (intersectionTestFunc.apply(event)) {
                impl_notifyScrollEvent(event.getWheelRotation());
            }
        }
    }

    /**
     * Encapsulates common code of the `notifyScrollEvent` methods and represents an implementation of sending
     * `gui-events:%element%-scroll` messages.
     * @param delta 1 for scrolling down or -1 for scrolling up
     */
    private void impl_notifyScrollEvent(int delta) {
        Map<String, Object> m = new HashMap<>();
        m.put("delta", delta);

        String eventMessage = "gui-events:" + senderName + "-scroll";
        Main.getInstance().getPluginProxy().sendMessage(eventMessage, m);
    }

    /**
     * Enables handling of click events for the node.
     * @return itself to let you use a chain of calls
     */
    MouseEventNotificator setOnClickListener() {
        sender.addEventFilter(MouseEvent.MOUSE_CLICKED, this::notifyClickEvent);
        return this;
    }

    /**
     * Enables handling of scroll and mouse wheel events for the node.
     * This type of events has a peculiarity on Windows. See the javadoc of notifyScrollEvents for more information.
     * @see #notifyScrollEvent
     * @param intersectionTestFunc a function that takes an event object and must return boolean
     * @return itself to let you use a chain of calls
     */
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
            mouseWheelListener = event -> notifyScrollEvent(event, intersectionTestFunc);
            GlobalScreen.addNativeMouseWheelListener(mouseWheelListener);
        } else {
            sender.addEventFilter(ScrollEvent.SCROLL, this::notifyScrollEvent);
        }

        return this;
    }

    /**
     * Removes all event listeners that were set earlier.
     */
    void cleanListeners() {
        // All methods have their own internal checks for the case when a filter is not set and equals null.
        sender.removeEventFilter(MouseEvent.MOUSE_CLICKED, this::notifyClickEvent);
        sender.removeEventFilter(ScrollEvent.SCROLL, this::notifyScrollEvent);
        GlobalScreen.removeNativeMouseWheelListener(mouseWheelListener);
    }
}
