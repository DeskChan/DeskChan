package com.eternal_search.deskchan.gui;

import com.eternal_search.deskchan.core.PluginProxy;
import com.eternal_search.deskchan.core.Utils;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class MainWindow extends JFrame {

	private static final int BALLOON_DEFAULT_TIMEOUT = 10000;

	private PluginProxy pluginProxy = null;
	private Path dataDirPath = null;
	private final CharacterWidget characterWidget = new CharacterWidget(this);
	private BalloonWidget balloonWidget = null;
	private BalloonWindow balloonWindow = null;
	OptionsDialog optionsDialog = null;
	private Timer balloonTimer = null;

	final Action quitAction = new AbstractAction("Quit") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			setVisible(false);
			dispose();
		}
	};
	final Action optionsAction = new AbstractAction("Options...") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			optionsDialog.updateOptions();
			Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			optionsDialog.setLocation(
					(screenBounds.width - optionsDialog.getWidth()) / 2 + screenBounds.x,
					(screenBounds.height - optionsDialog.getHeight()) / 2 + screenBounds.y
			);
			optionsDialog.setVisible(true);
		}
	};
	final List<PluginAction> extraActions = new ArrayList<>();
	
	void initialize(PluginProxy pluginProxy) {
		this.pluginProxy = pluginProxy;
		pluginProxy.sendMessage("core:get-plugin-data-dir", null, (sender_, data_) -> {
			dataDirPath = Paths.get(((Map) data_).get("path").toString());
			setTitle("DeskChan");
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			setUndecorated(true);
			setAlwaysOnTop(true);
			setType(Type.POPUP);
			setFocusableWindowState(false);
			setLayout(null);
			setBackground(new Color(0, 0, 0, 0));
			pack();
			characterWidget.loadBuiltinSkin("variant1");
			setDefaultLocation();
			setContentPane(characterWidget);
			optionsDialog = new OptionsDialog(this);
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent windowEvent) {
					pluginProxy.sendMessage("core:quit", null);
				}
			});
			pluginProxy.addMessageListener("gui:say", (sender, tag, data) -> {
				runOnEventThread(() -> {
					Map m = (Map) data;
					showBalloon(m.get("text").toString(), (int) m.getOrDefault("timeout", BALLOON_DEFAULT_TIMEOUT));
				});
			});
			pluginProxy.addMessageListener("gui:register-extra-action", (sender, tag, data) -> {
				runOnEventThread(() -> {
					Map m = (Map) data;
					String msgTag = m.get("msgTag").toString();
					Object msgData = m.getOrDefault("msgData", null);
					PluginAction action = new PluginAction(m.get("name").toString(), sender) {
						@Override
						public void actionPerformed(ActionEvent actionEvent) {
							pluginProxy.sendMessage(msgTag, msgData);
						}
					};
					extraActions.add(action);
				});
			});
			pluginProxy.addMessageListener("gui:change-skin", (sender, tag, data) -> {
				runOnEventThread(() -> {
					characterWidget.loadSkin(Paths.get(data.toString()));
					setDefaultLocation();
				});
			});
			pluginProxy.addMessageListener("gui:set-image", (sender, tag, data) -> {
				runOnEventThread(() -> {
					characterWidget.setImage(data.toString());
				});
			});
			pluginProxy.addMessageListener("core-events:plugin-unload", (sender, tag, data) -> {
				runOnEventThread(() -> {
					extraActions.removeIf(action -> action.getPlugin().equals(data));
				});
			});
			pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>() {{
				put("srcTag", "DeskChan:say"); put("dstTag", "gui:say"); put("priority", 100);
			}});
			pluginProxy.sendMessage("core:register-alternative", new HashMap<String, Object>() {{
				put("srcTag", "DeskChan:register-simple-action"); put("dstTag", "gui:register-extra-action");
				put("priority", 100);
			}});
		});
		balloonTimer = new Timer(BALLOON_DEFAULT_TIMEOUT, e -> {
			if (balloonWidget != null) {
				showBalloon((JComponent) null);
			}
		});
		balloonTimer.setRepeats(false);
	}
	
	void setDefaultLocation() {
		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		setLocation(
				(int)screenBounds.getMaxX() - getWidth(),
				(int)screenBounds.getMaxY() - getHeight()
		);
		updateSizes();
	}
	
	void updateSizes() {
		Dimension characterSize = characterWidget.getPreferredSize();
		Rectangle characterBounds = new Rectangle(new Point(0, 0), characterSize);
		Dimension frameSize = new Dimension(characterSize);
		if (balloonWidget != null) {
			Dimension balloonSize = balloonWidget.getPreferredSize();
			Rectangle balloonBounds = new Rectangle(
					new Point(getX() - balloonSize.width, getY()),
					balloonSize
			);
			if (balloonBounds.getX() < 0) {
				balloonBounds.x = getX() + frameSize.width;
			}
			balloonWindow.setBounds(balloonBounds);
		}
		Rectangle frameBounds = new Rectangle(getLocation(), frameSize);
		if (!characterBounds.equals(characterWidget.getBounds())) {
			characterWidget.setBounds(characterBounds);
		}
		if (!frameBounds.equals(getBounds())) {
			setBounds(frameBounds);
		}
	}
	
	private void showBalloon(JComponent component, int timeout) {
		if (balloonWidget != null) {
			if (balloonWindow != null) {
				balloonWindow.dispose();
			} else {
				remove(balloonWidget);
			}
			balloonWindow = null;
			balloonWidget = null;
		}
		if (component != null) {
			balloonWidget = new BalloonWidget(component, this);
			balloonWindow = new BalloonWindow(balloonWidget);
		}
		updateSizes();
		if (balloonWindow != null) {
			balloonWindow.setVisible(true);
		}
		if (balloonWidget != null) {
			if (balloonTimer.isRunning()) {
				balloonTimer.stop();
			}
			if (timeout > 0) {
				balloonTimer.setInitialDelay(timeout);
				balloonTimer.start();
			}
		}
	}
	
	private void showBalloon(JComponent component) {
		showBalloon(component, BALLOON_DEFAULT_TIMEOUT);
	}
	
	private void showBalloon(String text, int timeout) {
		if (text != null) {
			JLabel label = new JLabel("<html><center>" + text + "</center></html>");
			label.setHorizontalAlignment(JLabel.CENTER);
			label.setFont(label.getFont().deriveFont(15.0f));
			showBalloon(label, timeout);
		} else {
			showBalloon((JComponent) null);
		}
	}
	
	void showBalloon(String text) {
		showBalloon(text, BALLOON_DEFAULT_TIMEOUT);
	}
	
	@Override
	public void dispose() {
		if (balloonTimer.isRunning()) {
			balloonTimer.stop();
		}
		if (optionsDialog != null) {
			optionsDialog.dispose();
		}
		if (balloonWindow != null) {
			balloonWindow.dispose();
		}
		super.dispose();
	}
	
	void setPosition(Point pos) {
		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		Rectangle frameBounds = new Rectangle(pos, getSize());
		if (frameBounds.getMaxX() > screenBounds.getMaxX()) {
			frameBounds.x = (int) screenBounds.getMaxX() - frameBounds.width;
		}
		if (frameBounds.getMaxY() > screenBounds.getMaxY()) {
			frameBounds.y = (int)screenBounds.getMaxY() - frameBounds.height;
		}
		frameBounds.x = Math.max(screenBounds.x, frameBounds.x);
		frameBounds.y = Math.max(screenBounds.y, frameBounds.y);
		setLocation(frameBounds.x, frameBounds.y);
		if (balloonWindow != null) {
			updateSizes();
		}
	}
	
	CharacterWidget getCharacterWidget() {
		return characterWidget;
	}
	
	PluginProxy getPluginProxy() {
		return pluginProxy;
	}
	
	Path getDataDirPath() {
		return dataDirPath;
	}
	
	void showThrowable(Throwable e) {
		showThrowable(this, e);
	}
	
	static void showThrowable(JFrame frame, Throwable e) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		e.printStackTrace(printWriter);
		String stackTraceStr = stringWriter.toString();
		showLongMessage(frame, stackTraceStr, e.toString(), JOptionPane.ERROR_MESSAGE);
	}
	
	private static void showLongMessage(JFrame frame, String message, String title, int type) {
		JOptionPane.showMessageDialog(frame, new LongMessagePanel(message), title, type);
	}
	
	static void runOnEventThread(Runnable runnable) {
		if (SwingUtilities.isEventDispatchThread()) {
			runnable.run();
		} else {
			SwingUtilities.invokeLater(runnable);
		}
	}
	
	private static abstract class PluginAction extends AbstractAction {
		
		private String plugin;
		
		PluginAction(String text, String plugin) {
			super(text);
			this.plugin = plugin;
		}
		
		String getPlugin() {
			return plugin;
		}
		
	}
	
	private static class LongMessagePanel extends JPanel {
		
		private final BorderLayout borderLayout = new BorderLayout();
		private final JScrollPane scrollPane = new JScrollPane();
		private final JTextArea textArea = new JTextArea();
		
		private LongMessagePanel(String message) {
			this.setLayout(borderLayout);
			textArea.setEnabled(true);
			textArea.setEditable(false);
			textArea.setLineWrap(true);
			textArea.setText(message);
			textArea.setSize(textArea.getPreferredSize());
			scrollPane.getViewport().add(textArea, null);
			scrollPane.setPreferredSize(new Dimension(400, 250));
			scrollPane.getViewport().setViewPosition(new Point(0, 0));
			add(scrollPane, BorderLayout.CENTER);
		}
		
	}
	
}
