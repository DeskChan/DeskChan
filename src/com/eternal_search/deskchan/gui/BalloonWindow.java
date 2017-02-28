package com.eternal_search.deskchan.gui;

import javax.swing.*;
import java.awt.*;

public class BalloonWindow extends JFrame {
	
	private BalloonWidget widget;
	
	BalloonWindow(BalloonWidget widget) {
		super("DeskChan Balloon");
		this.widget = widget;
		setIconImage(MainWindow.getApplicationIcon());
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setUndecorated(true);
		setAlwaysOnTop(true);
		setType(Type.POPUP);
		setFocusableWindowState(false);
		setBackground(new Color(0, 0, 0, 0));
		pack();
		add(widget);
		setSize(widget.getPreferredSize());
	}
	
}
