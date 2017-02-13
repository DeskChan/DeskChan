package com.eternal_search.DeskChan.Gui;

import javax.swing.*;
import java.awt.*;

public class BalloonWindow extends JFrame {
	
	private BalloonWidget widget;
	
	BalloonWindow(BalloonWidget widget) {
		super("DeskChan Balloon");
		this.widget = widget;
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setUndecorated(true);
		setAlwaysOnTop(true);
		setBackground(new Color(0, 0, 0, 0));
		pack();
		add(widget);
		setSize(widget.getPreferredSize());
	}
	
}
