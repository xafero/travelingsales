/**
 * ITracksChangeListener.java
 *  created: 09.01.2009 22:31:23
 * (c) 2009 by <a href="mailto:oleg.chubaryov@mail.ru">Oleg Chubaryov</a>
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
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
package org.openstreetmap.travelingsalesman.gps.data;

import java.util.EventListener;

/**
 * Informs the listener that tracks storage state was changed.
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov</a>
 */
public interface ITracksChangeListener extends EventListener {
//    void addedTrack(final GpsTrack track);
//    void removedTrack(final GpsTrack track);

    /**
     * Informs the listener that a storage state has changed.
     * The listener can then invoke <code>GpsTrack</code> methods
     * to obtain information about the event.
     * @param action type of action
     * @param track the track
     */
    void updateTrack(final TrackEvent action, final GpsTrack track);

}
