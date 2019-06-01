package online.cszt0.music;

import javax.swing.*;
import java.awt.*;

/**
 * @author 初始状态0
 * @date 2019/6/1 17:43
 */
public class ProgressTextLabel extends JComponent {
	private String text;
	private float progress;
	private LrcPaint gradientPaint = new LrcPaint();

	public void setText(String text) {
		this.text = text;
		measureText();
		invalidate();
		getParent().validate();
		repaint();
	}

	private void measureText() {
		if (text == null) {
			setPreferredSize(new Dimension(0, 0));
			return;
		}
		FontMetrics fontMetrics = getFontMetrics(getFont());
		int width = fontMetrics.stringWidth(text);
		int height = fontMetrics.getHeight();
		setPreferredSize(new Dimension(width, height));
	}

	public void setProgress(float progress) {
		if (text == null) {
			return;
		}
		FontMetrics fontMetrics = getFontMetrics(getFont());
		if (progress < 0) {
			this.progress = fontMetrics.stringWidth(text);
		} else {
			this.progress = fontMetrics.stringWidth(text) * progress;
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (text == null) {
			return;
		}
		((Graphics2D) g).setPaint(gradientPaint);
		gradientPaint.setLeft(getX());
		gradientPaint.setProgress(progress);
		g.drawString(text, 0, getFontMetrics(getFont()).getAscent());
	}
}
