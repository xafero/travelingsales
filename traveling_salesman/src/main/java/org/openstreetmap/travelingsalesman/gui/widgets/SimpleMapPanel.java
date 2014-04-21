package org.openstreetmap.travelingsalesman.gui.widgets;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.EastNorth;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.projection.Projection;
import org.openstreetmap.osm.data.visitors.BoundingXYVisitor;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.gui.MapMover;
import org.openstreetmap.travelingsalesman.painting.BasePaintVisitor;
import org.openstreetmap.travelingsalesman.painting.IPaintVisitor;
import org.openstreetmap.travelingsalesman.painting.ODRPaintVisitor;

/**
 * The SimpleMapPanel exists to render a rectangular area of the DataSet.
 * Subclasses need to provide a current location via overriding
 * {@link #getCurrentPosition()} and course via overriding
 * {@link #getCurrentCourse()} and may override
 * {@link #paintAdditionalVectorData(Graphics2D)}.<br/>
 * See {@link MapPanel} for an implementation.
 * 
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public abstract class SimpleMapPanel extends JPanel implements
		INavigatableComponent {

	/**
	 * Where to draw the {@link ZoomSlider}.
	 */
	private static final Rectangle ZOOMSLIDERLOCATION = new Rectangle(3, 0,
			114, 30);
	/**
	 * Where to draw the {@link MapScaler}.
	 */
	private static final Point SCALERLOCATION = new Point(10, 30);
	/**
	 * The default zoom-level to start uo with.
	 */
	private static final double DEFAULTZOOM = 6.4E-6;
	/**
	 * The default area to show on startup.
	 */
	public static final LatLon DEFAULTCENTER = new LatLon(47.99581527709961,
			7.852292776107788);

	/**
	 * my logger for debug and error-output.
	 */
	private static final Logger LOG = Logger.getLogger(SimpleMapPanel.class
			.getName());

	/**
	 * The border then {@link #recalculateCenterScale(BoundingXYVisitor)} shall
	 * leave at the edges.
	 */
	private static final int RECALCCENTERSCALE_BORDERPX = 20;

	/**
	 * The serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The data we are rendering.
	 */
	private IDataSet myDataSet;

	/**
	 * The projection we are using.
	 */
	private Projection myProjection = Settings.getProjection();

	/**
	 * used to calculate the OSM-conforming zoom factor.
	 */
	private final EastNorth world = getProjection().latlon2eastNorth(
			Projection.MAX_LAT, Projection.MAX_LON);

	/**
	 * The Visitor that paints everything.
	 */
	private IPaintVisitor myPainter = (IPaintVisitor) Settings.getInstance()
			.getPluginProxy(IPaintVisitor.class,
					ODRPaintVisitor.class.getName());

	/**
	 * The scale factor in x or y-units per pixel. This means, if scale = 10,
	 * every physical pixel on screen are 10 x or 10 y units in the
	 * northing/easting space of the projection.
	 */
	private double scale = DEFAULTZOOM;

	/**
	 * Center n/e coordinate of the desired screen center.
	 */
	private EastNorth center = new EastNorth(0, 0);

	/**
	 * Prev center value.
	 */
	private EastNorth myOldCenter = null;

	/**
	 * @return the myPainter
	 */
	protected IPaintVisitor getMyPainter() {
		return myPainter;
	}

	/**
	 * Raster cache object.
	 */
	private Graphics2D myOldG2D;

	/**
	 * Raster cache object.
	 */
	private BufferedImage myCache;

	/**
	 * Raster cache object.
	 */
	private BufferedImage rotatedCache = null;

	/**
	 * The current user-position to mark.
	 * 
	 * @return the currentPosition
	 */
	public abstract LatLon getCurrentPosition();

	/**
	 * The current course. Uses for auto-rotation
	 * 
	 * @return the current course in degrees
	 */
	public abstract double getCurrentCourse();

	/**
	 * This method initializes this panel.
	 */
	protected void initialize() {

		this.setBackground(new Color(0, 0, 0, Color.TRANSLUCENT));

		// show europe
		zoomTo(getProjection().latlon2eastNorth(DEFAULTCENTER), DEFAULTZOOM);

		// let the user adjust the zoom-factor
		ZoomSlider zoomSlider = new ZoomSlider(this);
		add(zoomSlider);
		zoomSlider.setBounds(ZOOMSLIDERLOCATION);

		// display an indicator for the current scale
		MapScaler scaler = new MapScaler(this);
		add(scaler);
		scaler.setLocation(SCALERLOCATION);

		// let the user pan the map by dragging with the
		// right mouse-button down
		new MapMover(this, this);

		// the first time we get our component-site, recalculate center+scale
		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(final ComponentEvent e) {
				if (getDataSet() != null
						&& getDataSet() instanceof MemoryDataSet) {
					MemoryDataSet d = (MemoryDataSet) getDataSet();
					BoundingXYVisitor boundsV = new BoundingXYVisitor(d);
					d.visitAll(boundsV, Bounds.WORLD);
					SimpleMapPanel.this.recalculateCenterScale(boundsV
							.getBounds());
					// recalculate does a repaint already
				} else {
					// MapPanel.this.recalculateCenterScale(DEFAULTBOUNDS);
					if (!Settings.getInstance().getBoolean(
							"traveling-salesman.restoreLastState", true)) {
						zoomTo(getProjection().latlon2eastNorth(DEFAULTCENTER),
								DEFAULTZOOM);
					}
				}
				removeComponentListener(this);
			}
		});
	}

	/**
	 * Auto rotation mode. Top of map is a driving direction.
	 */
	private boolean myAutoRotationMode = false;

	/**
	 * Auto-center mode. Draw moving vehicle at the center of map always.
	 */
	private boolean myAutoCenterMode = false;

	/**
	 * Windows saved size.
	 */
	private int oldWidth = 0;

	/**
	 * Windows saved size.
	 */
	private int oldHeight = 0;
	/**
	 * Use off-screen prerasterized caching strategy ?
	 */
	private boolean useOffScreenCache = Settings.getInstance().getBoolean(
			"Painter.OffScreenCachingStrategy", true);
	/**
	 * Mutiplier for size of cached area in number of visible map screens. Size
	 * of overlapped image is: cacheWidth = 2 * cacheSize + getWidth;
	 * cacheHeight = 2 * cacheSize + getHeight; Equal added area in four
	 * directions.
	 */
	private final float cacheSize = (float) Settings.getInstance().getDouble(
			"Painter.OffScreenCachingSize", 1);
	/**
	 * Cache image dimensions.
	 */
	private int cacheWidth;
	/**
	 * Cache image dimensions.
	 */
	private int cacheHeight;
	/**
	 * Center of our cached image.
	 */
	private EastNorth cacheCenter = null;
	/**
	 * Scale of our cached image.
	 */
	private double cacheScale = 0;

	// public SimpleMapPanel() {
	// super();
	// }
	//
	// public SimpleMapPanel(final LayoutManager aLayout) {
	// super(aLayout);
	// }
	//
	// public SimpleMapPanel(final boolean aIsDoubleBuffered) {
	// super(aIsDoubleBuffered);
	// }
	//
	// public SimpleMapPanel(final LayoutManager aLayout, final boolean
	// aIsDoubleBuffered) {
	// super(aLayout, aIsDoubleBuffered);
	// }

	/**
	 * Visible area is in preraterized cache bounds ?
	 * 
	 * @return true if visible area is in prerasterized cache bounds
	 */
	private boolean isInCacheBounds() {
		return (getPoint(cacheCenter).x - this.getPoint(getCenter()).x) < cacheWidth
				&& (getPoint(cacheCenter).x - this.getPoint(getCenter()).x) > -cacheWidth
				&& (getPoint(cacheCenter).y - this.getPoint(getCenter()).y) < cacheHeight
				&& (getPoint(cacheCenter).y - this.getPoint(getCenter()).y) > -cacheHeight;
	}

	/**
	 * Clear and fill background.
	 * 
	 * @param g2d
	 *            graphics context
	 */
	private void clearPanel(final Graphics2D g2d) {
		if (getMyPainter() instanceof BasePaintVisitor) {
			g2d.setColor(((BasePaintVisitor) getMyPainter())
					.getBackgroundColor());
		} else {
			g2d.setColor(Color.LIGHT_GRAY);
		}
		if (useOffScreenCache) {
			g2d.fillRect(-cacheWidth, -cacheHeight,
					2 * cacheWidth + getWidth(), 2 * cacheHeight + getHeight());
		} else {
			g2d.fillRect(0, 0, getWidth(), getHeight());
		}
	}

	/**
	 * Paint this map.
	 * 
	 * @param g
	 *            the graphics we paint on.
	 */
	@Override
	public synchronized void paint(final Graphics g) {

		if (this.getDataSet() == null) {
			super.paint(g);
			return;
		}

		LOG.log(Level.FINEST, "paint() visiting map scale=" + this.scale);

		try {
			this.getMyPainter().setNavigatableComponent(this);
			Graphics2D g2d = (Graphics2D) g;
			final EastNorth center2 = getCenter();
			final double scale2 = getScale();

			// Bypass tile painters.
			// if Painter is not instance of tile painters use one of two
			// rasterizing caching strategy.
			if (!(myPainter.toString().lastIndexOf("SmoothTilePainter") > 0 || myPainter
					.toString().lastIndexOf("TilePaintVisitor") > 0)) {
				// determine of there is enough memory for offscreen-caching
				if (useOffScreenCache) {
					if (myCache == null || oldWidth != getWidth()
							|| oldHeight != getHeight()) {
						long memory = Runtime.getRuntime().freeMemory();
						final long bytePerPixel = 4;
						long required = bytePerPixel
								* (2 * cacheWidth + getWidth())
								* (2 * cacheHeight + getHeight());
						if (required > memory) {
							useOffScreenCache = false;
							LOG.warning("Not enough free memory for offscreen-bitmap. Disabling offscreen-caching.");
						}
					}
				}
				if (useOffScreenCache) {
					if (myCache == null || oldWidth != getWidth()
							|| oldHeight != getHeight()) {
						cacheWidth = (int) (cacheSize * getWidth());
						cacheHeight = (int) (cacheSize * getHeight());
						myCache = g2d.getDeviceConfiguration()
								.createCompatibleImage(
										2 * cacheWidth + getWidth(),
										2 * cacheHeight + getHeight());
						myOldG2D = myCache.createGraphics();
						myOldG2D.translate(cacheWidth, cacheHeight);
					}
					// 1. if map bounds inside the cache bounds paint the cache.
					// 2. overwise create new cache
					// draw at new raster cache
					if (cacheCenter == null || !isInCacheBounds()
							|| cacheScale != scale2 || oldWidth != getWidth()
							|| oldHeight != getHeight()) {
						clearPanel(myOldG2D);
						this.getMyPainter().visitAll(this.getDataSet(),
								myOldG2D);
						cacheCenter = center2;
						cacheScale = scale2;
						oldWidth = getWidth();
						oldHeight = getHeight();
					}
					if (myAutoRotationMode && getCurrentPosition() != null) {
						rotatedCache = g2d.getDeviceConfiguration()
								.createCompatibleImage(
										2 * cacheWidth + getWidth(),
										2 * cacheHeight + getHeight());
						AffineTransform tx = new AffineTransform();
						Point rotationPoint = this.getPoint(this
								.getProjection().latlon2eastNorth(
										getCurrentPosition().lat(),
										getCurrentPosition().lon()));
						tx.rotate(Math.toRadians(-1 * getCurrentCourse()),
								rotationPoint.x + cacheWidth, rotationPoint.y
										+ cacheHeight);
						tx.translate(
								this.getPoint(cacheCenter).x
										- this.getPoint(center2).x,
								this.getPoint(cacheCenter).y
										- this.getPoint(center2).y);
						AffineTransformOp op = new AffineTransformOp(tx,
								AffineTransformOp.TYPE_BILINEAR);
						op.filter(myCache, rotatedCache);
					}
					if (myAutoRotationMode) {
						g.drawImage(rotatedCache, -cacheWidth, -cacheHeight,
								this);
					} else {
						g.drawImage(myCache,
								-cacheWidth + this.getPoint(cacheCenter).x
										- this.getPoint(center2).x,
								-cacheHeight + this.getPoint(cacheCenter).y
										- this.getPoint(center2).y, this);
					}
				} else {
					// regular caching in visible area only
					// create raster cache
					if (myCache == null || oldWidth != getWidth()
							|| oldHeight != getHeight()) {
						myCache = g2d.getDeviceConfiguration()
								.createCompatibleImage(getWidth(), getHeight());
						myOldG2D = myCache.createGraphics();
					}
					// don't repaint whole map if we don't move or scale change,
					// or change windows size.
					// bacause it slowdown TS on every gps fix (changeLocation)
					if (cacheScale != scale2 || myOldCenter == null
							|| !myOldCenter.equals(center2)
							|| oldWidth != getWidth()
							|| oldHeight != getHeight()) {
						clearPanel(myOldG2D);
						this.getMyPainter().visitAll(this.getDataSet(),
								myOldG2D);
						myOldCenter = center2;
						cacheScale = scale2;
						oldWidth = getWidth();
						oldHeight = getHeight();
					}
					// paint map if we in autorotation mode, where is map
					// rotating by direction of driving.
					// top is driving ahead
					if (myAutoRotationMode && getCurrentPosition() != null) {
						rotatedCache = g2d.getDeviceConfiguration()
								.createCompatibleImage(getWidth(), getHeight());
						clearPanel(rotatedCache.createGraphics());
						AffineTransform tx = new AffineTransform();
						tx.rotate(Math.toRadians(-1 * getCurrentCourse()),
								getWidth() / 2, getHeight() / 2);
						AffineTransformOp op = new AffineTransformOp(tx,
								AffineTransformOp.TYPE_BILINEAR);
						op.filter(myCache, rotatedCache);
					}
					// use cached map
					if (myAutoRotationMode) {
						g.drawImage(rotatedCache, 0, 0, this);
					} else {
						g.drawImage(myCache, 0, 0, this);
					}
				}
			} else {
				// we use the TilePainter
				g.setColor(Color.LIGHT_GRAY);
				g.fillRect(0, 0, getWidth(), getHeight());
				this.getMyPainter().visitAll(this.getDataSet(), g2d);
			}

			if (myAutoRotationMode) {
				g2d.rotate(Math.toRadians(-1 * getCurrentCourse()),
						getWidth() / 2, getHeight() / 2); // apply rotation for
															// additional
															// elements drawing
			}
			paintAdditionalVectorData(g2d);
			if (myAutoRotationMode) {
				g2d.rotate(Math.toRadians(getCurrentCourse()), getWidth() / 2,
						getHeight() / 2); // reverse rotation to all later
											// operations
			}

		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Problem while painting map! ", e);
		}

		super.paint(g);
		LOG.log(Level.FINEST, "done");
	}

	/**
	 * Paint aditional elements onto the map after the basic map has been
	 * painted.
	 * 
	 * @param aG2d
	 *            what to paint at
	 */
	protected void paintAdditionalVectorData(final Graphics2D aG2d) {
		// paint nothing
	}

	/**
	 * @return the map we render.
	 */
	public IDataSet getDataSet() {
		return myDataSet;
	}

	/**
	 * @param aDataSet
	 *            the map we render.
	 */
	public void setDataSet(final IDataSet aDataSet) {
		myDataSet = aDataSet;

		if (aDataSet != null) {
			if (aDataSet instanceof MemoryDataSet) {
				MemoryDataSet d = (MemoryDataSet) aDataSet;
				BoundingXYVisitor boundsV = new BoundingXYVisitor(d);
				d.visitAll(boundsV, Bounds.WORLD);
				this.recalculateCenterScale(boundsV.getBounds());
				// recalculate does a repaint already
			} else {
				this.repaint();
				firePropertyChange("scale", 0, scale); // fire scale change for
														// properly
														// initialization
			}
		} else {
			this.repaint();
		}
	}

	/**
	 * @return the projection we use.
	 */
	public Projection getProjection() {
		return myProjection;
	}

	/**
	 * Warning: The returned coordinates may be illegal if the window shows the
	 * whole earth and is panned. In such a case the left-upprt pixel is in fact
	 * outside the earth grid.
	 * 
	 * @param x
	 *            X-Pixelposition to get coordinate from
	 * @param y
	 *            Y-Pixelposition to get coordinate from
	 * 
	 * @return Geographic unprojected coordinates from a specific pixel
	 *         coordination on the screen.
	 */
	public LatLon getLatLon(final int x, final int y) {
		EastNorth eastNorth = new EastNorth(center.east()
				+ (x - getWidth() / 2.0) * scale, center.north()
				- (y - getHeight() / 2.0) * scale);
		return getProjection().eastNorth2latlon(eastNorth);
	}

	/**
	 * @return the lat/lon of the visible area.
	 */
	public Bounds getMapBounds() {
		if (useOffScreenCache) {
			return new Bounds(getLatLon(-cacheWidth, -cacheHeight), getLatLon(
					getWidth() + cacheWidth - 1, getHeight() + cacheHeight - 1));
		} else {
			return new Bounds(getLatLon(0, 0), getLatLon(getWidth() - 1,
					getHeight() - 1));
		}
	}

	/**
	 * Return the point on the screen where this Coordinate would be.
	 * 
	 * @param aPoint
	 *            The point, where this geopoint would be drawn.
	 * @return The point on screen where "point" would be drawn, relative to the
	 *         own top/left.
	 */
	public Point getPoint(final EastNorth aPoint) {
		double x = (aPoint.east() - center.east()) / scale + getWidth() / 2.0d;
		double y = (center.north() - aPoint.north()) / scale + getHeight()
				/ 2.0d;
		return new Point((int) x, (int) y);
	}

	/**
	 * @return the OSM-conforming zoom factor (0 for whole world, 1 for half, 2
	 *         for quarter...).
	 */
	public int getZoom() {
		double sizex = scale * getWidth();
		double sizey = scale * getHeight();
		final double defaultWindowSize = 100;

		// if we have no size yet, assume we are 100x100
		if (getWidth() < 1) {
			sizex = scale * defaultWindowSize;
		}
		if (getHeight() < 1) {
			sizey = scale * defaultWindowSize;
		}

		for (int zoom = 0; zoom <= INavigatableComponent.MAXZOOM; zoom++, sizex *= 2, sizey *= 2) {
			if (sizex > world.east() || sizey > world.north()) {
				return zoom;
			}
		}
		return INavigatableComponent.MAXZOOM;
	}

	/**
	 * ${@inheritDoc}.
	 * 
	 * @see org.openstreetmap.travelingsalesman.INavigatableComponent#zoomTo(org.openstreetmap.osm.data.coordinates.EastNorth,
	 *      double)
	 */
	public void zoomTo(final EastNorth newCenter, final double aScale) {
		final EastNorth oldCenter = this.center;
		final double oldScale = this.scale;

		this.center = newCenter;
		this.scale = aScale;

		// paint rescaled bitmap cache while vector map drawing
		// if (cache != null) {
		// double scaleRatio = oldScale / this.scale;
		// final int scaledWidth = (int) (getWidth() * scaleRatio) - 1;
		// final int scaledHeight = (int) (getHeight() * scaleRatio) - 1;
		// Image image = cache.getScaledInstance(scaledWidth, scaledHeight,
		// Image.SCALE_FAST);
		// Graphics2D g = (Graphics2D) getGraphics();
		// g.setColor(Color.LIGHT_GRAY);
		// g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
		// g.drawImage(image,
		// (cache.getWidth() - scaledWidth) / 2 + getPoint(oldCenter).x -
		// getPoint(center).x,
		// (cache.getHeight() - scaledHeight) / 2 + getPoint(oldCenter).y -
		// getPoint(center).y, this);
		// }

		Runnable r = new Runnable() {
			public void run() {
				repaint();
				if ((oldCenter == null && center != null)
						|| (center != null && oldCenter != null && !oldCenter
								.equals(center)))
					firePropertyChange("center", oldCenter, center);
				if (oldScale != scale)
					firePropertyChange("scale", oldScale, scale);
			}
		};
		Thread async = new Thread(r);
		async.setDaemon(true);
		async.start();
	}

	/**
	 * Return the current scale value.
	 * 
	 * @return The scale value currently used in display
	 */
	public synchronized double getScale() {
		return scale;
	}

	/**
	 * @return Returns the center point. A copy is returned, so users cannot
	 *         change the center by accessing the return value. Use zoomTo
	 *         instead.
	 */
	public EastNorth getCenter() {
		return center;
	}

	/**
	 * @return Returns the left upper point. A copy is returned, so users cannot
	 *         change the center by accessing the return value. Use zoomTo
	 *         instead.
	 */
	public LatLon getLeftUpper() {
		return getLatLon(0, 0);
	}

	/**
	 * Set the new dimension to the projection class. Also adjust the components
	 * scale, if in autoScale mode.
	 * 
	 * @param box
	 *            a bounding-box-visitor that has visited the part of the map in
	 *            question
	 */
	public void recalculateCenterScale(final Bounds box) {
		// -20 to leave some border
		int w = getWidth() - RECALCCENTERSCALE_BORDERPX;
		if (w < RECALCCENTERSCALE_BORDERPX)
			w = RECALCCENTERSCALE_BORDERPX;
		int h = getHeight() - RECALCCENTERSCALE_BORDERPX;
		if (h < RECALCCENTERSCALE_BORDERPX)
			h = RECALCCENTERSCALE_BORDERPX;

		EastNorth oldCenter = center;
		double oldScale = this.scale;

		if (box == null || box.getMin() == null || box.getMax() == null
				|| box.getMin().equals(box.getMax())) {
			// no bounds means whole world
			center = getProjection().latlon2eastNorth(0, 0);
			EastNorth world2 = getProjection().latlon2eastNorth(
					Projection.MAX_LAT, Projection.MAX_LON);
			double scaleX = world2.east() * 2 / w;
			double scaleY = world2.north() * 2 / h;
			this.scale = Math.max(scaleX, scaleY); // minimum scale to see all
													// of the screen
		} else {
			EastNorth min = getProjection().latlon2eastNorth(box.getMin());
			EastNorth max = getProjection().latlon2eastNorth(box.getMax());
			center = new EastNorth(min.east() + (max.east() - min.east()) / 2,
					min.north() + (max.north() - min.north()) / 2);
			double scaleX = (max.east() - min.east()) / w;
			double scaleY = (max.north() - min.north()) / h;
			this.scale = Math.max(scaleX, scaleY); // minimum scale to see all
													// of the screen
		}

		if (!center.equals(oldCenter)) {
			firePropertyChange("center", oldCenter, center);
		}
		if (oldScale != scale) {
			firePropertyChange("scale", oldScale, scale);
		}
		repaint();
	}

	/**
	 * @param x
	 *            X-Pixelposition to get coordinate from
	 * @param y
	 *            Y-Pixelposition to get coordinate from
	 * 
	 * @return Geographic coordinates from a specific pixel coordination on the
	 *         screen.
	 */
	public EastNorth getEastNorth(final int x, final int y) {
		return new EastNorth(center.east() + (x - getWidth() / 2.0) * scale,
				center.north() - (y - getHeight() / 2.0) * scale);
	}

	/**
	 * @return the autoCenterMode
	 */
	public boolean isAutoCenterMode() {
		return myAutoCenterMode;
	}

	/**
	 * @param newAutoCenterMode
	 *            the autoCenterMode to set
	 */
	public void setAutoCenterMode(final boolean newAutoCenterMode) {
		this.myAutoCenterMode = newAutoCenterMode;
	}

	/**
	 * @return the autoRotationMode
	 */
	public boolean isAutoRotationMode() {
		return myAutoRotationMode;
	}

	/**
	 * @param newAutoRotationMode
	 *            the autoRotationMode to set
	 */
	public void setAutoRotationMode(final boolean newAutoRotationMode) {
		this.myAutoRotationMode = newAutoRotationMode;
		if (!myAutoRotationMode) {
			// free heap memory
			rotatedCache = null;
		}
	}

	/**
	 * @return Returns the useOffScreenCache.
	 * @see #useOffScreenCache
	 */
	public boolean isUseOffScreenCache() {
		return useOffScreenCache;
	}

	/**
	 * @param aUseOffScreenCache
	 *            The useOffScreenCache to set.
	 * @see #useOffScreenCache
	 */
	public void setUseOffScreenCache(final boolean aUseOffScreenCache) {
		this.useOffScreenCache = aUseOffScreenCache;
	}

}
