package com.eternal_search.DeskChan.Gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

class BalloonWidget extends JPanel implements MouseListener {
	
	private MainWindow mainWindow;
	
	BalloonWidget(JComponent component, MainWindow mainWindow) {
		super();
		this.mainWindow = mainWindow;
		setBackground(new Color(0, 0, 0, 0));
		setPreferredSize(new Dimension(400, 300));
		setLayout(new BorderLayout());
		add(component, BorderLayout.CENTER);
		addMouseListener(this);
	}
	
	private void close() {
		mainWindow.showBalloon((String) null);
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (g instanceof Graphics2D) {
			Graphics2D g2d = (Graphics2D)g;
			g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
		}
		g.setColor(Color.WHITE);
		g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
		g.setColor(Color.BLACK);
		g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		close();
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
	}
	
}
