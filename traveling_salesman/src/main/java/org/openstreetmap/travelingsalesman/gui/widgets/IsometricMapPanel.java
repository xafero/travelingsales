/**
 * This file is part of Traveling-Salesman by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  Traveling-Salesman is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Traveling-Salesman is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Traveling-Salesman.  If not, see <http://www.gnu.org/licenses/>.
 *
 ***********************************
 * Editing this file:
 *  -For consistent code-quality this file should be checked with the
 *   checkstyle-ruleset enclosed in this project.
 *  -After the design of this file has settled it should get it's own
 *   JUnit-Test that shall be executed regularly. It is best to write
 *   the test-case BEFORE writing this class and to run it on every build
 *   as a regression-test.
 */
package org.openstreetmap.travelingsalesman.gui.widgets;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;

import org.openstreetmap.osm.data.coordinates.EastNorth;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider.IGPSListener;

/**
 * This class changes MapPanel to display it's map isometric instead of plain
 * planar and allways GPS-centered.
 * 
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class IsometricMapPanel extends MapPanel implements IGPSListener {

	/**
	 * generated.
	 */
	private static final long serialVersionUID = 398582042427212653L;

	/**
	 * ${@inheritDoc}.
	 */
	@Override
	public EastNorth getEastNorth(final int aX, final int aY) {
		int x = getUndistortedX(aX, aY);
		EastNorth turned = super.getEastNorth(x, aY);
		double theta = -1.0 * Math.toRadians(getCurrentCourse());
		EastNorth unturned = new EastNorth(turned.east() * Math.cos(theta)
				- turned.north() * Math.sin(theta), turned.east()
				* Math.sin(theta) + turned.north() * Math.cos(theta));
		return unturned;
	}

	/**
	 * ${@inheritDoc}.
	 */
	@Override
	public Point getPoint(final EastNorth aPoint) {
		// TODO test this
		double theta = Math.toRadians(getCurrentCourse());
		EastNorth rotatedPoint = new EastNorth(aPoint.east() * Math.cos(theta)
				- aPoint.north() * Math.sin(theta), aPoint.east()
				* Math.sin(theta) + aPoint.north() * Math.cos(theta));
		Point point = super.getPoint(rotatedPoint);
		point.x = getDistortedX(point.x, point.y);
		return point;
	}

	/**
	 * Create a new MapPanel that displays isometrically distorted. Set
	 * AutoCenterMode to be on at all times and AutoRotation+ScreenCache to stay
	 * off.
	 */
	public IsometricMapPanel() {
		super();
		super.setAutoCenterMode(true);
		super.setAutoRotationMode(false);
		super.setUseOffScreenCache(false);
	}

	/**
	 * @param newAutoCenterMode
	 *            the autoCenterMode to set
	 */
	public void setAutoCenterMode(final boolean newAutoCenterMode) {
		// ignored
	}

	/**
	 * @param newAutoRotationMode
	 *            the autoRotationMode to set
	 */
	public void setAutoRotationMode(final boolean newAutoRotationMode) {
		// ignored
	}

	/**
	 * @param aUseOffScreenCache
	 *            The useOffScreenCache to set.
	 * @see #useOffScreenCache
	 */
	public void setUseOffScreenCache(final boolean aUseOffScreenCache) {
		// ignored
	}

	/**
	 * Get the perspectively distorted X -coordinate.
	 * 
	 * @param x
	 *            the original x (horizontal, 0 being the left side)
	 * @param y
	 *            the original y (vertical, 0 being the horizon)
	 * @return the new X-value (>= the original x)
	 */
	private int getDistortedX(final int x, final int y) {
		int height2 = getHeight();
		int centerX = getWidth() / 2;
		return centerX + (x - centerX) * (1 + (4 * y) / height2);
	}

	/**
	 * Reverse the perspectively distorted X -coordinate.
	 * 
	 * @param x
	 *            the distorted x (horizontal, 0 being the left side)
	 * @param y
	 *            the distorted y (vertical, 0 being the horizon)
	 * @return the new X-value (<= the original x)
	 */
	private int getUndistortedX(final int x, final int y) {
		int height2 = getHeight();
		int centerX = getWidth() / 2;
		return ((x - centerX) / (1 + (4 * y) / height2)) + centerX;
	}

	/**
	 * Paint this map.
	 * 
	 * @param g
	 *            the graphics we paint on.
	 */
	@Override
	public void paint(final Graphics g) {
		Graphics2D g2d = (Graphics2D) g;
		AffineTransform oldTransform = g2d.getTransform();

		try {
			// double height2 = getHeight();
			// double centerX = getWidth() / 2;
			//
			// AffineTransform translate1 = new AffineTransform();
			// translate1.translate(-1 * centerX, 0);
			//
			// AffineTransform shear2 = new AffineTransform();
			// //g2d.shear(4.0 / height2 , 0.0);
			// shear2.shear(0.5 , 0.0);
			//
			// AffineTransform translate3 = new AffineTransform();
			// translate3.translate(centerX, 0);
			//
			// AffineTransform combined = new AffineTransform();
			// combined.concatenate(translate1);
			// combined.concatenate(shear2);
			// combined.concatenate(translate3);
			//
			// g2d.transform(combined);

			super.paint(g2d);
		} finally {
			g2d.setTransform(oldTransform);
		}
	}

	// /**
	// * This helper-class wraps a Graphics2D and distorts
	// * all coordinates in order to paint isometric.<br/>
	// * Images, {@link Shape}s and text (such as icons) are drawn at the
	// correct position but not distorted.
	// * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
	// */
	// private static class IsometricGraphics extends Graphics2D {
	// /**
	// * The graphics-Object we are wrapping.
	// */
	// private Graphics2D graphics;
	//
	// /**
	// * The height of the component.
	// */
	// private int height;
	//
	// /**
	// * The width of the component.
	// */
	// private int width;
	//
	// /**
	// * @param graphics The graphics-Object we are wrapping.
	// * @param height The height of the component.
	// * @param width The width of the component.
	// */
	// public IsometricGraphics(Graphics2D graphics, int height, int width) {
	// super();
	// this.graphics = graphics;
	// this.height = height;
	// this.width = width;
	// }
	//
	// private int getDistortedX(final int x, final int y) {
	// return x * (1 + y / width);
	// }
	// private float getDistortedX(final float x, final float y) {
	// return x * (1 + y / width);
	// }
	// private double getDistortedX(final double x, final double y) {
	// return x * (1 + y / width);
	// }
	//
	// @Override
	// public void addRenderingHints(final Map<?, ?> hints) {
	// this.graphics.addRenderingHints(hints);
	// }
	//
	// @Override
	// public void clip(final Shape s) {
	// this.graphics.clip(s);
	// }
	//
	// @Override
	// public void draw(final Shape s) {
	// if (s instanceof GeneralPath) {
	// GeneralPath path = (GeneralPath) s;
	// path.get
	// }
	//
	// PathIterator pathIterator = s.getPathIterator(null);
	// while (!pathIterator.isDone()) {
	// pathIterator.
	// }
	//
	// this.graphics.clip(s);
	// }
	//
	// @Override
	// public void drawGlyphVector(GlyphVector g, float x, float y) {
	// this.graphics.drawGlyphVector(g, getDistortedX(x, y), y);
	// }
	//
	// @Override
	// public boolean drawImage(Image img, AffineTransform xform,
	// ImageObserver obs) {
	// return this.graphics.drawImage(img, xform, obs);
	// }
	//
	// @Override
	// public void drawImage(BufferedImage img, BufferedImageOp op, int x,
	// int y) {
	// this.graphics.drawImage(img, op, getDistortedX(x, y), y);
	// }
	//
	// @Override
	// public void drawRenderableImage(RenderableImage img,
	// AffineTransform xform) {
	// this.graphics.drawRenderableImage(img, xform);
	// }
	//
	// @Override
	// public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
	// this.graphics.drawRenderedImage(img, xform);
	// }
	//
	// @Override
	// public void drawString(String str, int x, int y) {
	// this.graphics.drawString(str, getDistortedX(x, y), y);
	// }
	//
	// @Override
	// public void drawString(String str, float x, float y) {
	// this.graphics.drawString(str, getDistortedX(x, y), y);
	// }
	//
	// @Override
	// public void drawString(AttributedCharacterIterator iterator, int x,
	// int y) {
	// this.graphics.drawString(iterator, getDistortedX(x, y), y);
	// }
	//
	// @Override
	// public void drawString(AttributedCharacterIterator iterator, float x,
	// float y) {
	// this.graphics.drawString(iterator, getDistortedX(x, y), y);
	// }
	//
	// @Override
	// public void fill(Shape s) {
	// this.graphics.fill(s);
	// }
	//
	// @Override
	// public Color getBackground() {
	// return this.graphics.getBackground();
	// }
	//
	// @Override
	// public Composite getComposite() {
	// return this.graphics.getComposite();
	// }
	//
	// @Override
	// public GraphicsConfiguration getDeviceConfiguration() {
	// return this.graphics.getDeviceConfiguration();
	// }
	//
	// @Override
	// public FontRenderContext getFontRenderContext() {
	// return this.graphics.getFontRenderContext();
	// }
	//
	// @Override
	// public Paint getPaint() {
	// return this.graphics.getPaint();
	// }
	//
	// @Override
	// public Object getRenderingHint(final Key hintKey) {
	// return this.graphics.getRenderingHint(hintKey);
	// }
	//
	// @Override
	// public RenderingHints getRenderingHints() {
	// return this.graphics.getRenderingHints();
	// }
	//
	// @Override
	// public Stroke getStroke() {
	// return this.graphics.getStroke();
	// }
	//
	// @Override
	// public AffineTransform getTransform() {
	// return this.graphics.getTransform();
	// }
	//
	// @Override
	// public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
	// return this.graphics.hit(rect, s, onStroke);
	// }
	//
	// @Override
	// public void rotate(double theta) {
	// this.graphics.rotate(theta);
	// }
	//
	// @Override
	// public void rotate(double theta, double x, double y) {
	// this.graphics.rotate(theta, getDistortedX(x, y), y);
	// }
	//
	// @Override
	// public void scale(double sx, double sy) {
	// this.graphics.scale(sx, sy);
	// }
	//
	// @Override
	// public void setBackground(Color color) {
	// this.graphics.setBackground(color);
	// }
	//
	// @Override
	// public void setComposite(Composite comp) {
	// this.graphics.setComposite(comp);
	// }
	//
	// @Override
	// public void setPaint(Paint paint) {
	// this.graphics.setPaint(paint);
	// }
	//
	// @Override
	// public void setRenderingHint(Key hintKey, Object hintValue) {
	// this.graphics.setRenderingHint(hintKey, hintValue);
	// }
	//
	// @Override
	// public void setRenderingHints(Map<?, ?> hints) {
	// this.graphics.setRenderingHints(hints);
	// }
	//
	// @Override
	// public void setStroke(Stroke s) {
	// this.graphics.setStroke(s);
	// }
	//
	// @Override
	// public void setTransform(AffineTransform Tx) {
	// this.graphics.setTransform(Tx);
	// }
	//
	// @Override
	// public void shear(double shx, double shy) {
	// this.graphics.shear(shx, shy);
	// }
	//
	// @Override
	// public void transform(AffineTransform Tx) {
	// this.graphics.transform(Tx);
	// }
	//
	// @Override
	// public void translate(int x, int y) {
	// this.graphics.translate(x, y);
	// }
	//
	// @Override
	// public void translate(double tx, double ty) {
	// this.graphics.translate(tx, ty);
	// }
	//
	// @Override
	// public void clearRect(int x, int y, int width, int height) {
	// this.graphics.clearRect(getDistortedX(x, y), y, width, height);
	// }
	//
	// @Override
	// public void clipRect(int x, int y, int width, int height) {
	// this.graphics.clipRect(getDistortedX(x, y), y, width, height);
	// }
	//
	// @Override
	// public void copyArea(int x, int y, int width, int height, int dx, int dy)
	// {
	// this.graphics.copyArea(getDistortedX(x, y), y, width, height, dx, dy);
	// }
	//
	// @Override
	// public Graphics create() {
	// return this.graphics.create();
	// }
	//
	// @Override
	// public void dispose() {
	// this.graphics.dispose();
	// }
	//
	// @Override
	// public void drawArc(int x, int y, int width, int height,
	// int startAngle, int arcAngle) {
	// this.graphics.drawArc(getDistortedX(x, y), y, width, height, startAngle,
	// arcAngle);
	// }
	//
	// @Override
	// public boolean drawImage(Image img, int x, int y, ImageObserver observer)
	// {
	// return this.graphics.drawImage(img, getDistortedX(x, y), y, observer);
	// }
	//
	// @Override
	// public boolean drawImage(Image img, int x, int y, Color bgcolor,
	// ImageObserver observer) {
	// return this.graphics.drawImage(img, getDistortedX(x, y), y, bgcolor,
	// observer);
	// }
	//
	// @Override
	// public boolean drawImage(Image img, int x, int y, int width,
	// int height, ImageObserver observer) {
	// //graphics.drawImage((BufferedImage) img, TODO, getDistortedX(x, y), y);
	// //return true;
	// return this.graphics.drawImage(img, getDistortedX(x, y), y, width,
	// height, observer); //TODO Isometric does not yet work
	// }
	//
	// @Override
	// public boolean drawImage(Image img, int x, int y, int width,
	// int height, Color bgcolor, ImageObserver observer) {
	// return this.graphics.drawImage(img, getDistortedX(x, y), y, width,
	// height, bgcolor, observer);
	// }
	//
	// @Override
	// public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
	// int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
	// return this.graphics.drawImage(img, getDistortedX(dx1, dy1), dy1,
	// getDistortedX(dx2, dy2), dy2, sx1, sy1, sx2, sy2, observer);
	// }
	//
	// @Override
	// public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
	// int sx1, int sy1, int sx2, int sy2, Color bgcolor,
	// ImageObserver observer) {
	// return this.graphics.drawImage(img, getDistortedX(dx1, dy1), dy1,
	// getDistortedX(dx2, dy2), dy2, sx1, sy1, sx2, sy2, bgcolor, observer);
	// }
	//
	// @Override
	// public void drawLine(int x1, int y1, int x2, int y2) {
	// this.graphics.drawLine(getDistortedX(x1, y1), y1, getDistortedX(x2, y2),
	// y2);
	// }
	//
	// @Override
	// public void drawOval(int x, int y, int width, int height) {
	// this.graphics.drawOval(getDistortedX(x, y), y, width, height);
	//
	// }
	//
	// @Override
	// public void drawPolygon(int[] points, int[] points2, int points3) {
	// int[] points_ = new int[points3];
	// for (int i=0; i<points3; i++) {
	// points_[i] = getDistortedX(points[i], points2[i]);
	// }
	// this.graphics.drawPolygon(points_, points2, points3);
	// }
	//
	// @Override
	// public void drawPolyline(int[] points, int[] points2, int points3) {
	// int[] points_ = new int[points3];
	// for (int i=0; i<points3; i++) {
	// points_[i] = getDistortedX(points[i], points2[i]);
	// }
	// this.graphics.drawPolyline(points_, points2, points3);
	//
	// }
	//
	// @Override
	// public void drawRoundRect(int x, int y, int width, int height,
	// int arcWidth, int arcHeight) {
	// this.graphics.drawRoundRect(getDistortedX(x, y), y, width, height,
	// arcWidth, arcHeight);
	//
	// }
	//
	// @Override
	// public void fillArc(int x, int y, int width, int height,
	// int startAngle, int arcAngle) {
	// this.graphics.fillArc(getDistortedX(x, y), y, width, height, startAngle,
	// arcAngle);
	//
	// }
	//
	// @Override
	// public void fillOval(int x, int y, int width, int height) {
	// this.graphics.fillOval(getDistortedX(x, y), y, width, height);
	// }
	//
	// @Override
	// public void fillPolygon(int[] points, int[] points2, int points3) {
	// int[] points_ = new int[points3];
	// for (int i=0; i<points3; i++) {
	// points_[i] = getDistortedX(points[i], points2[i]);
	// }
	// this.graphics.fillPolygon(points_, points2, points3);
	//
	// }
	//
	// @Override
	// public void fillRect(int x, int y, int width, int height) {
	// this.graphics.fillRect(getDistortedX(x, y), y, width, height);
	// }
	//
	// @Override
	// public void fillRoundRect(int x, int y, int width, int height,
	// int arcWidth, int arcHeight) {
	// this.graphics.fillRoundRect(getDistortedX(x, y), y, width, height,
	// arcWidth, arcHeight);
	// }
	//
	// @Override
	// public Shape getClip() {
	// return this.graphics.getClip();
	// }
	//
	// @Override
	// public Rectangle getClipBounds() {
	// return this.graphics.getClipBounds();
	// }
	//
	// @Override
	// public Color getColor() {
	// return this.graphics.getColor();
	// }
	//
	// @Override
	// public Font getFont() {
	// return this.graphics.getFont();
	// }
	//
	// @Override
	// public FontMetrics getFontMetrics(Font f) {
	// return this.graphics.getFontMetrics(f);
	// }
	//
	// @Override
	// public void setClip(Shape clip) {
	// this.graphics.setClip(clip);
	//
	// }
	//
	// @Override
	// public void setClip(int x, int y, int width, int height) {
	// this.graphics.setClip(x, y, width, height);
	//
	// }
	//
	// @Override
	// public void setColor(Color c) {
	// this.graphics.setColor(c);
	// }
	//
	// @Override
	// public void setFont(Font font) {
	// this.graphics.setFont(font);
	// }
	//
	// @Override
	// public void setPaintMode() {
	// this.graphics.setPaintMode();
	// }
	//
	// @Override
	// public void setXORMode(Color c1) {
	// this.graphics.setXORMode(c1);
	// }
	//
	// }
	//
}
