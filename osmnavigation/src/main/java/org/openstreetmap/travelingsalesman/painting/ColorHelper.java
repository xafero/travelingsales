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
 *  LibOSM is distributed in the hope that it will be useful,
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
package org.openstreetmap.travelingsalesman.painting;

import java.awt.Color;

/**
 * Helper to convert from color to html string and back.
 */
public final class ColorHelper {

    /**
     * Utility-classes have no public constructor.
     */
    private ColorHelper() {
    }


    /**
     * 16.
     */
    private static final int HEX = 16;

    /**
     * Convert an html-color-code to an AWT-color.
     * @param html the code
     * @return the color it represents or null
     */
    public static Color html2color(final String html) {
        String code = html;
        if (code.length() > 0 && code.charAt(0) == '#')
            code = code.substring(1);
        if (code.length() != (2 + 2 + 2))
            return null;
        try {
            return new Color(
                    Integer.parseInt(code.substring(0, 2), HEX),
                    Integer.parseInt(code.substring(2, 2 + 2), HEX),
                    Integer.parseInt(code.substring(2 + 2, 2 + 2 + 2), HEX));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Convert an integer into a hex-string.
     * @param i the integer
     * @return the hex-value
     */
    private static String int2hex(final int i) {
        String s = Integer.toHexString(i / HEX) + Integer.toHexString(i % HEX);
        return s.toUpperCase();
    }

    /**
     * Convett an awt-color into an html-color-string.
     * @param col the color
     * @return the html-color-code
     */
    public static String color2html(final Color col) {
        return "#" + int2hex(col.getRed()) + int2hex(col.getGreen()) + int2hex(col.getBlue());
    }
}
