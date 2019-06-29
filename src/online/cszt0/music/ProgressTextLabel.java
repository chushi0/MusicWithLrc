package online.cszt0.music;

import javax.swing.*;
import java.awt.*;

/**
 * @author 初始状态0
 * @date 2019/6/1 17:43
 */
@SuppressWarnings("WeakerAccess")
public class ProgressTextLabel extends JComponent {
	private String text;
	private float progress;
	private LrcPaint gradientPaint = new LrcPaint(0xff0000, 0xffffff);

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
		setPreferredSize(new Dimension(Math.min(1920, width), height));
	}

	public void setReachedColor(int reachedColor) {
		gradientPaint.setReachedColor(reachedColor);
	}

	public void setUnreachedColor(int unreachedColor) {
		gradientPaint.setUnreachedColor(unreachedColor);
	}

	public void setProgress(float progress, int start, int end) {
		if (text == null) {
			return;
		}
		FontMetrics fontMetrics = getFontMetrics(getFont());
		if (progress < 0) {
			this.progress = fontMetrics.stringWidth(text);
		} else {
			this.progress = fontMetrics.stringWidth(text.substring(0, start)) +
					fontMetrics.stringWidth(text.substring(start, end)) * progress;
		}
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (text == null) {
			return;
		}
		FontMetrics fontMetrics = getFontMetrics(getFont());
		int width = fontMetrics.stringWidth(text);
		int x;
		if (width < 1920) {
			x = 0;
		} else {
			x = (int) (1920 / 2 - progress);
			if (x > 0) {
				x = 0;
			}
			int min = 1920 - width;
			if (x < min) {
				x = min;
			}
		}
		((Graphics2D) g).setPaint(gradientPaint);
		gradientPaint.setLeft(getX() + x);
		gradientPaint.setProgress(progress);
		g.drawString(text, x, getFontMetrics(getFont()).getAscent());
	}
}
