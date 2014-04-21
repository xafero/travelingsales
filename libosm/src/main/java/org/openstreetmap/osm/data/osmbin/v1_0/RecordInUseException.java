/**
 * RecordInUseException.java
 * created: 26.02.2009
 * (c) 2008 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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

package org.openstreetmap.osm.data.osmbin.v1_0;

import java.io.IOException;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * RecordInUseException.java<br/>
 * created: 26.02.2009<br/>
 *<br/>
 * <b>Exception to indicate that this record is already in use by another
 * entity.</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 * The NodeID in the record allows cleaning up and defragmenting the file. The byte-index into the file is stored in the attrvalue-slot prepended by a marker
 */

public class RecordInUseException extends IOException {

    /**
     * generated.
     */
    private static final long serialVersionUID = -2991831177910657436L;
    /**
     * The record-number that was already in use.
     */
    private long myRecordNumber;
    /**
     * @param aRecordNb The record-number that was already in use.
     * @param aString a message to the user
     */
    public RecordInUseException(final long aRecordNb, final String aString) {
        super(aString);
        this.myRecordNumber = aRecordNb;
    }
    /**
     * @return the recordNumber
     */
    public long getRecordNumber() {
        return myRecordNumber;
    }

}
