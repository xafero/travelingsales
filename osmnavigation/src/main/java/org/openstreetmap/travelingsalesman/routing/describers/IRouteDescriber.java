/**
 * This file is part of OSMNavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  OSMNavigation is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  OSMNavigation is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with OSMNavigation.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.travelingsalesman.routing.describers;

import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.travelingsalesman.painting.ImageResources;
import org.openstreetmap.travelingsalesman.routing.Route;
import org.openstreetmap.travelingsalesman.routing.Route.RoutingStep;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;

/**
 * This interface is implemented by all classes that
 * provide the driving-instructions for a given route.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public interface IRouteDescriber extends IPlugin {

    /**
     * @return true if there is a next driving-instruction.
     */
    boolean hasNextInstruction();

    /**
     * The driving instruction returned is indepentend of the current location of the vehicle.
     * Example: "Turn right onto XYZ road.".
     * @return the next driving-instruction to be read/shown to the user.
     * @see #getCurrentInstruction(double, double)
     */
    String getNextInstruction();

    /**
     * @return the node where the action described to the user by {@link #getNextInstruction()}} has to be taken.
     */
    Node getCurrentInstructionLocation();

    /**
     * @return Returns the type of the current driving-instruction.
     * @see #myCurrentInstructionType
     */
    DrivingInstructionType getCurrentInstructionType();

    /**
     * Example: "In 200m turn right onto XYZ road".
     * @return The current instruction in a location-dependent way.
     * @param aDistance distance to {@link #getCurrentInstructionLocation()} in kilometers.
     * @see #getNextInstruction()
     */
    String getCurrentInstruction(final double aDistance);

    /**
     * @return The route we are describing.
     */
    Route getRoute();

    /**
     * @param aStep the step that is now nearest to the GPS-location
     * @return the description to be read/shown to the user. (as {@link #getNextInstruction()} would do)
     */
    String setCurrentStep(final RoutingStep aStep);

    /**
     * Set the route to describe.
     * @param aRoute the new route (may be null)
     */
    void setRoute(final Route aRoute);

    /**
     * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
     * Project: osmnavigation<br/>
     * IRouteDescriber.java<br/>
     * created: 01.02.2009 08:54:31 <br/>
     *<br/><br/>
     * This enumeration helps to classify driving-instructions.
     * It is e.g. used to load the proper icon.
     * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
     */
    enum DrivingInstructionType {
        /**
         * Unclassified Driving-instruction.
         */
        OTHER {
            /**
             * @return the name of the default-icon-resource without the path.
             */
            public String getDefaultIconName() {
                return null;
            }
        },
        /**
         * Driving-instruction to turn left.
         */
        TURN_LEFT {
            /**
             * @return the name of the default-icon-resource without the path.
             */
            public String getDefaultIconName() {
                return "turn_left.png";
            }
        },
        /**
         * Driving-instruction to turn right.
         */
        TURN_RIGHT {
            /**
             * @return the name of the default-icon-resource without the path.
             */
            public String getDefaultIconName() {
                return "turn_right.png";
            }
        },
        /**
         * Driving-instruction to turn left in a steep angle.
         */
        TURN_LEFT_HARD {
            /**
             * @return the name of the default-icon-resource without the path.
             */
            public String getDefaultIconName() {
                return "turn_left.png";
            }
        },
        /**
         * Driving-instruction to turn right in a steep angle.
         */
        TURN_RIGHT_HARD {
            /**
             * @return the name of the default-icon-resource without the path.
             */
            public String getDefaultIconName() {
                return "turn_right.png";
            }
        },
        /**
         * Driving-instruction to turn around.
         */
        TURN_AROUND {
            /**
             * @return the name of the default-icon-resource without the path.
             */
            public String getDefaultIconName() {
                return "uturn.png";
            }
        },
        /**
         * Driving-instruction to keep but not quite turn left.
         */
        KEEP_LEFT {
            /**
             * @return the name of the default-icon-resource without the path.
             */
            public String getDefaultIconName() {
                return "keep_left.png";
            }
        },
        /**
         * Driving-instruction to keep but not quite turn right.
         */
        KEEP_RIGHT {
            /**
             * @return the name of the default-icon-resource without the path.
             */
            public String getDefaultIconName() {
                return "keep_right.png";
            }
        },
        /**
         * Driving-instruction involving a roundabout.
         */
        ROUNDABOUT {
            /**
             * @return the name of the default-icon-resource without the path.
             */
            public String getDefaultIconName() {
                return "roundabout.png";
            }
        },
        /**
         * Driving-instruction to follow ahead.
         */
        GO_AHEAD {
            /**
             * @return the name of the default-icon-resource without the path.
             */
            public String getDefaultIconName() {
                return "ahead.png";
            }
        };

        /**
         * @return the name of the default-icon-resource without the path.
         */
        public abstract String getDefaultIconName();

        /**
         * @return the default-icon associated with the type of driving-instruction. May be null
         */
        public Icon getDefaultIcon() {
            String defaultIconName = getDefaultIconName();
            if (defaultIconName == null) {
                return null;
            }
            BufferedImage image = ImageResources.getImage("navigation/" + defaultIconName);
            if (image == null) {
                return null;
            }
            return new ImageIcon(image);
        }
    }

    /**
     * @return The RoutingStep the current instruction belongs to.
     */
    RoutingStep getCurrentStep();
}
