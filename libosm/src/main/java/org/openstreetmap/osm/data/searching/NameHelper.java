/**
 * NameHelper.java
 * created: 25.11.2007 09:06:19
 * (c) 2007 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
 * This file is part of libosm by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  libosm is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  libosm is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with libosm.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.osm.data.searching;


/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * NameHelper.java<br/>
 * created: 25.11.2007 09:06:19 <br/>
 *<br/><br/>
 * The name-helper exists to normalize names that have
 * multiple ways of spelling.
 * e.g. "main-road". "mainrd.", "main road", "Main rd.".
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public final class NameHelper {

    /**
     * Utility-classes have no public constructor.
     */
    private NameHelper() {
    }

    /**
     * Normalize the street-name to a lower-case-name
     * with common short forms normalized to a single
     * way of spelling.
     * @param name a street-name
     * @return a normalized street-name
     */
    public static String normalizeName(final String name) {
        if (name == null)
            throw new IllegalArgumentException("null name given to normalize");
        return name.toLowerCase()
        .replaceAll("\u00df", "ss")// German sharp s (Eszett) to double-s
        .replace("strasse", "str")
        .replace("\u0423\u043b", "\u0438\u0446")// cyrillic "uliza"-"ul"
        .replace("road", "rd")
        .replace("avenue", "av")
        .replace('.', ' ')
        .replace('\t', ' ')
        .replace('\n', ' ')
        .replace('-', ' ')
        .replaceAll("  ", " ")
        .replace("von", "v.")  // "Heinrich von Stefan Straße" == "Heinrich v. Stefan Str"
        .replace("of", "o.")
        .replace("der", "d."); // "Platz der weißen Rose" == "Platz d. weißen Rose"
    }

    /**
     * Build a regular expression to compare with
     * normalized names.
     * @param lookFor the entered search
     * @return a regexp to be used with {@link String#matches(String)}
     */
    public static String buildNameSearchRegexp(final String lookFor) {
    	return NameHelper.normalizeName(lookFor).replaceAll("[^a-zA-Z1-9à-ÿÀ-ß]", ".*") + ".*";
    }

    /**
     * Build a SQL-"like" expression to compare with
     * normalized names.
     * @param lookFor the entered search
     * @return a regexp to be used with sql "like"
     */
    public static String buildNameSearchSQLMatch(final String lookFor) {
        return "%"
           + NameHelper.normalizeName(lookFor)
           .replaceAll("\\s", "%") // \s = whitespace
           + "%";
    }
}


