package com.eternal_search.DeskChan.Gui;

import com.eternal_search.DeskChan.Core.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class MainWindow extends JFrame {

	private CharacterWidget characterWidget = new CharacterWidget(this);
	private BalloonWidget balloonWidget = null;
	private BalloonWindow balloonWindow = null;
	OptionsDialog optionsDialog = null;
	Action quitAction = new AbstractAction("Quit") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			setVisible(false);
			dispose();
		}
	};
	Action optionsAction = new AbstractAction("Options...") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			if (optionsDialog == null) {
				optionsDialog = new OptionsDialog(MainWindow.this);
				Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
				optionsDialog.setLocation(
						(screenBounds.width - optionsDialog.getWidth()) / 2 + screenBounds.x,
						(screenBounds.height - optionsDialog.getHeight()) / 2 + screenBounds.y
				);
				optionsDialog.setVisible(true);
			}
		}
	};
	Action testAction = new AbstractAction("Test") {
		@Override
		public void actionPerformed(ActionEvent actionEvent) {
			JLabel label = new JLabel("Hello world!");
			label.setHorizontalAlignment(JLabel.CENTER);
			showBalloon(label);
		}
	};
	
	private MainWindow() {
		super("DeskChan");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setUndecorated(true);
		setAlwaysOnTop(true);
		setFocusableWindowState(false);
		setLayout(null);
		setBackground(new Color(0, 0, 0, 0));
		pack();
		characterWidget.loadImage(Utils.getResourcePath("characters/sprite0001.png"));
		setDefaultLocation();
		setContentPane(characterWidget);
	}
	
	void setDefaultLocation() {
		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		setLocation(
				(int)screenBounds.getMaxX() - getWidth(),
				(int)screenBounds.getMaxY() - getHeight()
		);
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
	
	void showBalloon(JComponent component) {
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
	}
	
	@Override
	public void dispose() {
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
	
	public static void createAndShowGUI() {
		javax.swing.SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e) {
				e.printStackTrace();
			}
			MainWindow window = new MainWindow();
			window.setVisible(true);
		});
	}

}
