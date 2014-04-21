/**
 * MapFeatures.java
 * created: 09.12.2007 07:23:00
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 *
 *  osmnavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  osmnavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with osmnavigation.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.painting.odr;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Plugins.IPlugin;



/**
 * @author <a href="mailto:tdad@users.sourceforge.net">Sven Koehler</a>
 */
public class GermanMapFeatures implements IMapFeaturesSet {

    /**
     * This plugin has no  settings, thus this method returns null
     * as described in {@link IPlugin#getSettings()}.
     * @return null
     */
    public ConfigurationSection getSettings() {
        return null;
    }

    /**
     * {@inheritDoc}.
     */
    public void paintATM(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub

    }

    /**
     * paints a sign for a bus/railway station.
     *
     * @param centerPoint point to paint sign on
     * @param graphicsContext graphics context
     */
    public void paintStopSign(final Point centerPoint, final Graphics2D graphicsContext) {
        final int outerCircleRadius = 15;
        final int midCircleRadius = 13;
        final int innerCircleRadius = 9;
        final int hFontSize = 16;


        Ellipse2D outerCircle = new Ellipse2D.Float(
                                        centerPoint.x - outerCircleRadius,
                                        centerPoint.y - outerCircleRadius,
                                        outerCircleRadius * 2, outerCircleRadius * 2);
        Ellipse2D midCircle = new Ellipse2D.Float(
                                        centerPoint.x - midCircleRadius,
                                        centerPoint.y - midCircleRadius,
                                        midCircleRadius * 2, midCircleRadius * 2);
        Ellipse2D innerCircle = new Ellipse2D.Float(
                                        centerPoint.x - innerCircleRadius,
                                        centerPoint.y - innerCircleRadius,
                                        innerCircleRadius * 2, innerCircleRadius * 2);

        graphicsContext.setColor(Color.YELLOW);
        graphicsContext.fill(outerCircle);
        graphicsContext.setColor(Color.GREEN);
        graphicsContext.fill(midCircle);
        graphicsContext.setColor(Color.YELLOW);
        graphicsContext.fill(innerCircle);
        graphicsContext.setColor(Color.GREEN);
        graphicsContext.setFont(new Font("", Font.BOLD, hFontSize));
        FontMetrics fm = graphicsContext.getFontMetrics();
        graphicsContext.drawString("H", centerPoint.x - (fm.charWidth('H') / 2), centerPoint.y + (fm.charWidth('H') / 2));
    }

    /**
     * {@inheritDoc}.
     */
    public void paintBusStation(final Point centerPoint, final Graphics2D graphicsContext) {
        paintStopSign(centerPoint, graphicsContext);
    }

    /**
     * {@inheritDoc}.
     */
    public void paintHospital(final Point centerPoint, final Graphics2D graphicsContext) {
        final int outerCircleRadius = 15;
        final int innerCircleRadius = 13;
        final int crossLineHalfLength = 9;
        final int crossLineWidth = 6;

        Ellipse2D outerCircle = new Ellipse2D.Float(
                                        centerPoint.x - outerCircleRadius,
                                        centerPoint.y - outerCircleRadius,
                                        outerCircleRadius * 2, outerCircleRadius * 2);
        Ellipse2D innerCircle = new Ellipse2D.Float(
                                        centerPoint.x - innerCircleRadius,
                                        centerPoint.y - innerCircleRadius,
                                        innerCircleRadius * 2, innerCircleRadius * 2);

        GeneralPath crossPath = new GeneralPath();
        crossPath.moveTo(centerPoint.x - crossLineHalfLength, centerPoint.y);
        crossPath.lineTo(centerPoint.x + crossLineHalfLength, centerPoint.y);
        crossPath.moveTo(centerPoint.x, centerPoint.y - crossLineHalfLength);
        crossPath.lineTo(centerPoint.x, centerPoint.y + crossLineHalfLength);
        BasicStroke crossStroke = new BasicStroke(crossLineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

        graphicsContext.setColor(Color.RED);
        graphicsContext.fill(outerCircle);
        graphicsContext.setColor(Color.WHITE);
        graphicsContext.fill(innerCircle);
        graphicsContext.setColor(Color.RED);
        graphicsContext.fill(crossStroke.createStrokedShape(crossPath));
    }

    /**
     * {@inheritDoc}.
     */
    public void paintParking(final Point centerPoint, final Graphics2D graphicsContext) {
        final int parkingSignHalfWidth = 14;
        final int fontSize = 22;
        final int arc = 3;
        final float alpha = 0.65f;

        RoundRectangle2D outerRect = new RoundRectangle2D.Float(
                                            centerPoint.x - parkingSignHalfWidth,
                                            centerPoint.y - parkingSignHalfWidth,
                                            parkingSignHalfWidth * 2,
                                            parkingSignHalfWidth * 2,
                                            arc, arc);

        Composite originalComposite = graphicsContext.getComposite();

        graphicsContext.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        graphicsContext.setColor(Color.BLUE);
        graphicsContext.fill(outerRect);
        graphicsContext.setColor(Color.WHITE);
        graphicsContext.setFont(new Font("", Font.BOLD, fontSize));
        graphicsContext.drawString(
                                    "P",
                                    centerPoint.x - parkingSignHalfWidth + (fontSize - parkingSignHalfWidth),
                                    centerPoint.y + parkingSignHalfWidth - (fontSize - parkingSignHalfWidth));
        graphicsContext.setComposite(originalComposite);
    }

    /**
     * {@inheritDoc}.
     */
    public void paintPharmacy(final Point centerPoint, final Graphics2D graphicsContext) {
        final int outestRectHalfWidth = 15;
        final int whiteRectHalfWidth = 13;
        final int innerRectHalfWidth = 11;
        final int crossLineHalfLength = 9;
        final int crossLineWidth = 6;
        final int arc = 3;

        RoundRectangle2D outerRect = new RoundRectangle2D.Float(
                                            centerPoint.x - outestRectHalfWidth,
                                            centerPoint.y - outestRectHalfWidth,
                                            outestRectHalfWidth * 2,
                                            outestRectHalfWidth * 2,
                                            arc, arc);
        RoundRectangle2D whiteRect = new RoundRectangle2D.Float(
                                            centerPoint.x - whiteRectHalfWidth,
                                            centerPoint.y - whiteRectHalfWidth,
                                            whiteRectHalfWidth * 2,
                                            whiteRectHalfWidth * 2,
                                            arc, arc);
        RoundRectangle2D innerRect = new RoundRectangle2D.Float(
                                            centerPoint.x - innerRectHalfWidth,
                                            centerPoint.y - innerRectHalfWidth,
                                            innerRectHalfWidth * 2,
                                            innerRectHalfWidth * 2,
                                            arc, arc);

        GeneralPath crossPath = new GeneralPath();
        crossPath.moveTo(centerPoint.x - crossLineHalfLength, centerPoint.y);
        crossPath.lineTo(centerPoint.x + crossLineHalfLength, centerPoint.y);
        crossPath.moveTo(centerPoint.x, centerPoint.y - crossLineHalfLength);
        crossPath.lineTo(centerPoint.x, centerPoint.y + crossLineHalfLength);
        BasicStroke crossStroke = new BasicStroke(crossLineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

        graphicsContext.setColor(Color.GREEN);
        graphicsContext.fill(outerRect);
        graphicsContext.setColor(Color.WHITE);
        graphicsContext.fill(whiteRect);
        graphicsContext.setColor(Color.GREEN);
        graphicsContext.fill(innerRect);
        graphicsContext.setColor(Color.WHITE);
        graphicsContext.fill(crossStroke.createStrokedShape(crossPath));
    }

    /**
     * {@inheritDoc}.
     */
    public void paintPostBox(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}.
     */
    public void paintPostOffice(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}.
     */
    public void paintRailwayStation(final Point centerPoint,
            final Graphics2D graphicsContext) {
        paintStopSign(centerPoint, graphicsContext);
    }

    /**
     * {@inheritDoc}.
     */
    public void paintToilets(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub

    }

    /**
     * {@inheritDoc}.
     */
    public void paintFuel(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}.
     */
    public void paintTramStop(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintBusStop(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintAerodrome(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintBank(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintBicycleParking(final Point centerPoint,
            final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintCarSharing(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintCourthouse(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintFountain(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintMonument(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintPlaceOfWorship(final Point centerPoint,
            final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintPolice(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintSchool(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintSubwayEntrance(final Point centerPoint,
                                    final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintUniversity(final Point centerPoint, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintCity(final Point node2Point, final String nodeCaption, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintHamlet(final Point node2Point, final String nodeCaption,
                            final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintSuburb(final Point node2Point, final String nodeCaption,
                            final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintTown(final Point node2Point, final String nodeCaption,
                          final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintVillage(final Point node2Point, final String nodeCaption, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintShop(final Point centerPoint, final String shopKind, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintAmenity(final Point centerPoint, final String amenityType, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }

    /**
     * ${@inheritDoc}.
     */
    public void paintTourism(final Point centerPoint, final String tourismNodeType, final Graphics2D graphicsContext) {
        // TODO Auto-generated method stub
    }
}
