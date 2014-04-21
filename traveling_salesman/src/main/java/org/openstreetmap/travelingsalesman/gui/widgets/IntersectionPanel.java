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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.EastNorth;
import org.openstreetmap.osm.data.coordinates.LatLon;
import org.openstreetmap.osm.data.projection.Mercator;
import org.openstreetmap.osm.data.projection.Projection;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.travelingsalesman.INavigatableComponent;
import org.openstreetmap.travelingsalesman.gps.IGPSProvider.IExtendedGPSListener;
import org.openstreetmap.travelingsalesman.navigation.IRoutingStepListener;
import org.openstreetmap.travelingsalesman.painting.ODRPaintVisitor;
import org.openstreetmap.travelingsalesman.routing.IRouteChangedListener;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;
import org.openstreetmap.travelingsalesman.routing.describers.IRouteDescriber;
import org.openstreetmap.travelingsalesman.routing.describers.SimpleRouteDescriber;

import sun.awt.VerticalBagLayout;

/**
 * This panel displays the next intersection for driving a route.
 * ETA and other navigation information, it's placed over the map.
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
 */
public class IntersectionPanel extends JComponent implements IRouteChangedListener, IRoutingStepListener, IExtendedGPSListener {

        /**
         * Where to draw the {@link MapScaler}.
         */
        public static final Rectangle BOUNDS = new Rectangle(0, 60, 120, 150);

        /**
         * Our IntersectionPanel.java.
         */
        private static final long serialVersionUID = 1L;

        /**
         * Underlied black box.
         */
        private MapOverBox box = null;

        /**
         * The route we display.
         */
        private Route myRoute = null;

        /**
         * An {@link INavigatableComponent} to update the
         * "next maneuver-positoon" on.
         * May be null.
         */
        private Set<INavigatableComponent> myMapDisplays = new HashSet<INavigatableComponent>();

        /**
         * Component where we drawing.
         */
        private JPanel myParentPanel = null;

        /**
         * The label that shows the ETA and other such data.
         */
        private MetricsPanel myMetricsPanel = new MetricsPanel();

        /**
         * The label that shows the next driving-instruction
         * or message.
         */
        // TODO : Do not draw this widget if "no route"
        private JLabel myCurrentInstructionLabel = new JLabel("-no route yet-");

        /**
         * A button to advance to the next routing-step manually.
         */
        private JButton myNextStepButton;

        /**
         * The IRouteDescriber -plugin to
         * use for generating the driving-
         * instructions.
         */
        private IRouteDescriber describer = null;

        /**
         * Last known location.
         */
        private double myLastLat;

        /**
         * Last known location.
         */
        private double myLastLon;

        /**
         * @param aRoute The route we display
         * @param aParentPanel panel where we drawing this component.
         */
        public IntersectionPanel(final Route aRoute, final JPanel aParentPanel) {
            myRoute = aRoute;
            myParentPanel = aParentPanel;
            initialize();
            if (myParentPanel.getLayout() instanceof BorderLayout) {
                myParentPanel.add(this, BorderLayout.EAST);
            } else {
                myParentPanel.add(this);
            }
        }

        /**
         * Coordinates of next maneuer position.
         */
        private LatLon coords = null;

        /**
         * Draw an intersection of roads on the panel.
         * @author Oleg Chubaryov
         */
        final class Intersection extends JComponent implements INavigatableComponent {

            /**
             * Initial value for the {@link #scale}.
             */
            private static final double INITIALSCALE = 0.0000001;

            /**
             * Default serial ID.
             */
            private static final long serialVersionUID = 7015914725450767414L;

            private double scale = INITIALSCALE;

            private int zoom = 15;

            /**
             * The map we are working with.
             */
            private IDataSet dataSet = null;

            private ODRPaintVisitor painter = null;

            private Intersection () {
                final int borderWidth = 10;
                setPreferredSize(new Dimension(BOUNDS.width - borderWidth, BOUNDS.width - borderWidth));
                setOpaque(true);
                painter = new ODRPaintVisitor();
                painter.setNavigatableComponent(this);
                // compute scale for drawing the intersection and 20-30 meters around.
                if (Settings.getProjection() instanceof Mercator) {
                    scale = INITIALSCALE;
                } else {
                    scale = INITIALSCALE / 100;
                }
            }

            /**
             * {@inheritDoc}.
             */
            public void paintComponent(final Graphics g) {
                if (myRoute != null && coords != null) {
                    Graphics2D g2d = (Graphics2D) g;
                    Color oldColor = g2d.getColor();
                    Composite oldComposite = g2d.getComposite();
                    try {
                        painter.visitAll(myRoute.getMap(), g2d);
                        List<RoutingStep> routeSegments = myRoute.getRoutingSteps();
                        // TODO : Draw path on intersection as yellow arrow instead of black line segments
                        painter.visitRoute(myRoute.getMap(), routeSegments);
                    } finally {
                        g2d.setComposite(oldComposite);
                        g2d.setColor(oldColor);
                    }
                }
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public EastNorth getCenter() {
                return Settings.getProjection().latlon2eastNorth(coords);
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public LatLon getCurrentPosition() {
                return coords;
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public IDataSet getDataSet() {
                return dataSet;
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public EastNorth getEastNorth(final int aX, final int aY) {
                // TODO Auto-generated method stub
                return null;
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public LatLon getLatLon(final int aX, final int aY) {
                // TODO Auto-generated method stub
                return null;
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public LatLon getLeftUpper() {
                // TODO Auto-generated method stub
                return null;
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public Bounds getMapBounds() {
                return new Bounds(coords, scale * 10000);
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public LatLon getNextManeuverPosition() {
                return coords;
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public Point getPoint(final EastNorth aPoint) {
                EastNorth center = Settings.getProjection().latlon2eastNorth(coords);
                double x = (aPoint.east()  - center.east()) / scale + getWidth() / 2.0d;
                double y = (center.north() - aPoint.north()) / scale + getHeight() / 2.0d;
                return new Point((int) x, (int) y);
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public Projection getProjection() {
                return Settings.getProjection();
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public double getScale() {
                return scale;
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public int getZoom() {
                return zoom;
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public void setCurrentPosition(final LatLon aCurrentPosition) {
                coords = myMetricsPanel.getNextIntersectionLocation();
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public void setNextManeuverPosition(final LatLon aNextManeuverPosition) {
                coords = aNextManeuverPosition;
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public void setSelectedNodePosition(final LatLon aSelectedNodePosition) {
            }

            /**
             * {@inheritDoc}.
             */
            @Override
            public void zoomTo(final EastNorth aCenter, final double aZoomLevel) {
            }
        }

        /**
         * Create the graphical-interface.
         */
        private void initialize() {
            this.setLayout(new VerticalBagLayout(3));
            this.box = new MapOverBox(BOUNDS.getSize());
            box.setLayout(new BorderLayout());
            JComponent intersection = new Intersection();
            this.myCurrentInstructionLabel.setForeground(Color.WHITE);
            this.myCurrentInstructionLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            box.add(intersection, BorderLayout.NORTH);
            box.add(this.myCurrentInstructionLabel, BorderLayout.CENTER);
            this.add(box);
        }


        /**
         * Button thar displays the icon for the type of the current
         * driving-instruction (ahead, left, right, ...) and allows skipping
         * to the next one.
         * @return the button
         */
        private JButton getNextStepButton() {
            if (this.myNextStepButton == null) {
                Icon icon = IRouteDescriber.DrivingInstructionType.TURN_LEFT.getDefaultIcon();
                this.myNextStepButton = new JButton(icon);
                this.myNextStepButton.setBackground(new Color(0, 0, 0, 0));
                this.myNextStepButton.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
                this.myNextStepButton.setBorder(null);
                this.myNextStepButton.addActionListener(new ActionListener() {

                    public void actionPerformed(final ActionEvent arg0) {
                        if (describer != null && describer.hasNextInstruction()) {
                            myCurrentInstructionLabel.setText(describer.getNextInstruction());
                            getNextStepButton().setIcon(describer.getCurrentInstructionType().getDefaultIcon());

                            Node step = describer.getCurrentInstructionLocation();
                            LatLon nextManeuverPosition = new LatLon(step.getLatitude(), step.getLongitude());
                            for (INavigatableComponent myMapDisplay : myMapDisplays) {
                                myMapDisplay.setNextManeuverPosition(nextManeuverPosition);
                            }

                            myMetricsPanel.setNextIntersectionLocation(nextManeuverPosition, describer.getCurrentStep(), myRoute);

                            // if we have a current location, display
                            // "in 200m turn left" instead of "turn left"
                            if (myLastLat != 0 && myLastLon != 0) {
                                gpsLocationChanged(myLastLat, myLastLon);
                            }
                        } else {
                            myCurrentInstructionLabel.setText("-no more instructions-");
                            for (INavigatableComponent myMapDisplay : myMapDisplays) {
                                myMapDisplay.setNextManeuverPosition(null);
                            }
                            myMetricsPanel.setNextIntersectionLocation(null, null, null);
                        }
                    }
                });
            }
            return this.myNextStepButton;
        }

        /**
         * @return The route we display
         */
        public Route getRoute() {
            return myRoute;
        }

        /**
         * @param aRoute The route we display
         */
        public void setRoute(final Route aRoute) {
            myRoute = aRoute;
            if (aRoute == null) {
                describer = null;
                myCurrentInstructionLabel.setText("-no route yet-");
                getNextStepButton().setIcon(null);
                for (INavigatableComponent myMapDisplay : myMapDisplays) {
                    myMapDisplay.setNextManeuverPosition(null);
                }
                this.myMetricsPanel.setNextIntersectionLocation(null, null, null);
            } else {
                if (describer == null) {
                    describer = Settings.getInstance().getPlugin(IRouteDescriber.class, SimpleRouteDescriber.class.getName());
                }
                describer.setRoute(aRoute);

                for (INavigatableComponent myMapDisplay : myMapDisplays) {
                    myMapDisplay.setNextManeuverPosition(null);
                }
                // display the first instruction
                getNextStepButton().getActionListeners()[0].actionPerformed(null);
            }
        }

        /**
         * The route to travel has changed.
         * @param newRoute the new route or null if
         * there is no route anymore.
         */
        public void routeChanged(final Route newRoute) {
            setRoute(newRoute);
        }
        /**
         * ${@inheritDoc}.
         */
        @Override
        public void noRouteFound() {
            routeChanged(null);
        }


        /**
         * An {@link INavigatableComponent} to update the
         * "next maneuver-positoon" on.
         * May be null.
         * @param aComponent the NC
         */
        public void addMapPanel(final INavigatableComponent aComponent) {
            this.myMapDisplays.add(aComponent);
        }

        /**
         * Another routing-step is now the nearest one to the
         * current GPS-location.
         * @param aCurrentStep the step.
         */
        public void nearestRoutingStepChanged(final RoutingStep aCurrentStep) {
            coords = new LatLon(aCurrentStep.getEndNode().getLatitude(), aCurrentStep.getEndNode().getLongitude());
            if (this.myRoute != null && this.describer != null) {
                String simpleInstruction = this.describer.setCurrentStep(aCurrentStep);
                getNextStepButton().setIcon(describer.getCurrentInstructionType().getDefaultIcon());
                Node stepNode = describer.getCurrentInstructionLocation();
                LatLon nextManeuverPosition = new LatLon(stepNode.getLatitude(), stepNode.getLongitude());
                for (INavigatableComponent myMapDisplay : myMapDisplays) {
                    myMapDisplay.setNextManeuverPosition(
                            nextManeuverPosition);
                }
                this.myMetricsPanel.setNextIntersectionLocation(nextManeuverPosition, aCurrentStep, myRoute);
                if (myLastLat != 0 && myLastLon != 0) {
                    // show complex instruction with distance to point
                    gpsLocationChanged(myLastLat, myLastLon);
                } else {
                    // show simple instruction
                    this.myCurrentInstructionLabel.setText(simpleInstruction);
                }
            }
        }


        /**
         * ${@inheritDoc}.
         */
        @Override
        public void gpsCourseChanged(final double aCourse) {
            this.myMetricsPanel.gpsCourseChanged(aCourse);
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void gpsLocationChanged(final double aLat, final double aLon) {
            this.myLastLat = aLat;
            this.myLastLon = aLon;
            this.myMetricsPanel.gpsLocationChanged(aLat, aLon);
            // TODO we should use the distance on the road instead of a straight line
            if (this.describer == null) {
                return;
            }
            Node currentStepLocation = this.describer.getCurrentInstructionLocation();
            if (currentStepLocation != null) {
                LatLon next = new LatLon(currentStepLocation.getLatitude(), currentStepLocation.getLongitude());
                LatLon cur = new LatLon(aLat, aLon);
                final int kilo = 1000;
                double distance = (double) (LatLon.distanceInMeters(cur, next)) / kilo;

                // get an instruction like "in 200m turn left".
                if (describer.getCurrentStep() == null) {
                    this.myCurrentInstructionLabel.setText("");
                } else {
                    this.myCurrentInstructionLabel.setText(describer.getCurrentInstruction(distance));
                }
            }
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void gpsLocationLost() {
            this.myLastLat = 0;
            this.myLastLon = 0;
            this.myMetricsPanel.gpsLocationLost();
        }


        /**
         * {@inheritDoc}
         */
        @Override
        public void gpsLocationObtained() {
            // ignored
            this.myMetricsPanel.gpsLocationObtained();
        }


        /**
         * ${@inheritDoc}.
         */
        @Override
        public void setBackground(final Color aBg) {
            // work around a bug with transparency in swing
            if (this.myMetricsPanel != null) {
                this.myMetricsPanel.setBackground(aBg);
            }
            super.setBackground(aBg);
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void gpsAltitudeChanged(final double aAltitude) {
            // ignored
            this.myMetricsPanel.gpsAltitudeChanged(aAltitude);
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void gpsDateTimeChanged(final long aDate, final long aTime) {
            // ignored
            this.myMetricsPanel.gpsDateTimeChanged(aDate, aTime);
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void gpsDopChanged(final double aHdop, final double aVdop, final double aPdop) {
            // ignored
            this.myMetricsPanel.gpsDopChanged(aHdop, aVdop, aPdop);
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void gpsFixQualityChanged(final int aFixQuality) {
            // ignored
            this.myMetricsPanel.gpsFixQualityChanged(aFixQuality);
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void gpsSpeedChanged(final double aSpeed) {
            // TODO indicate if we are above the speed-limit
            this.myMetricsPanel.gpsSpeedChanged(aSpeed);
        }

        /**
         * ${@inheritDoc}.
         */
        @Override
        public void gpsUsedSattelitesChanged(final int aSatellites) {
            // ignored
            this.myMetricsPanel.gpsUsedSattelitesChanged(aSatellites);
        }
}
