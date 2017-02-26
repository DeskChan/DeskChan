package com.eternal_search.deskchan.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;

public class InputWidget extends JPanel implements KeyListener {
	
	private final MainWindow mainWindow;
	private JLabel messageLabel = null;
	private JTextField textField = new JTextField();
	
	InputWidget(MainWindow mainWindow) {
		super(new BorderLayout());
		this.mainWindow = mainWindow;
		add(textField, BorderLayout.PAGE_END);
		setBackground(Color.WHITE);
		textField.addKeyListener(this);
	}
	
	String getText() {
		return textField.getText();
	}
	
	void setMessage(String message) {
		if (message == null) {
			if (messageLabel != null) {
				remove(messageLabel);
				messageLabel = null;
			}
			return;
		}
		message = "<html><center>" + message + "</center></html>";
		if (messageLabel == null) {
			messageLabel = new JLabel(message);
			messageLabel.setHorizontalAlignment(JLabel.CENTER);
			messageLabel.setFont(mainWindow.balloonTextFont);
			add(messageLabel);
		} else {
			messageLabel.setText(message);
		}
	}
	
	@Override
	public void keyTyped(KeyEvent keyEvent) {
		if (keyEvent.getKeyChar() == '\n') {
			mainWindow.showBalloon(null, new HashMap<>());
		}
	}
	
	@Override
	public void keyPressed(KeyEvent keyEvent) {
	}
	
	@Override
	public void keyReleased(KeyEvent keyEvent) {
	}
}
