package online.cszt0.music;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * @author 初始状态0
 * @date 2019/6/1 18:04
 */
public class LrcPaint implements Paint {

	private static DirectColorModel directColorModel = new DirectColorModel(24, 0xff0000, 0x00ff00, 0x0000ff);
	private float progress;
	private float left;
	private final IPaintContext context = new IPaintContext();

	public void setProgress(float progress) {
		this.progress = progress;
	}

	public void setLeft(float left) {
		this.left = left;
	}

	@Override
	public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) {
		return context;
	}

	@Override
	public int getTransparency() {
		return OPAQUE;
	}

	private class IPaintContext implements PaintContext {

		@Override
		public void dispose() {
		}

		@Override
		public ColorModel getColorModel() {
			return directColorModel;
		}

		@Override
		public Raster getRaster(int x, int y, int w, int h) {
			WritableRaster raster = directColorModel.createCompatibleWritableRaster(w, h);
			final int[] blue = {0, 0, 0xff};
			final int[] white = {0xff, 0xff, 0xff};
			for (int i = 0; i < w; i++) {
				for (int j = 0; j < h; j++) {
					if (x - left + i < progress) {
						raster.setPixel(i, j, blue);
					} else {
						raster.setPixel(i, j, white);
					}
				}
			}
			return raster;
		}
	}
}
