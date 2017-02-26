package com.eternal_search.deskchan.gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

class BalloonWidget extends JPanel implements MouseListener {
	
	private final MainWindow mainWindow;
	
	BalloonWidget(JComponent component, MainWindow mainWindow) {
		super();
		this.mainWindow = mainWindow;
		setBackground(new Color(0, 0, 0, 0));
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setMinimumSize(new Dimension(400, 200));
		setMaximumSize(new Dimension(400, 600));
		setPreferredSize(new Dimension(400, 300));
		add(component, BorderLayout.CENTER);
		addMouseListener(this);
	}
	
	private void close() {
		mainWindow.closeBalloon();
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
		String event = "gui-events:balloon-left-click";
		if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
			event = "gui-events:balloon-double-click";
		} else if (SwingUtilities.isRightMouseButton(e)) {
			event = "gui-events:balloon-right-click";
		} else if (SwingUtilities.isMiddleMouseButton(e)) {
			event = "gui-events:balloon-middle-click";
		}
		mainWindow.getPluginProxy().sendMessage(event, null);
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
