/**
 * IDIndexFileTest.java
 * created: 29.11.2008 11:39
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
package org.openstreetmap.osm.data;

//other imports
import static org.junit.Assert.*;

import java.util.*;

//automatically created logger for debug and error -output
import java.util.logging.Logger;

//automatically created propertyChangeListener-Support
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.osmbin.IDIndexFile;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;


/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * IDIndexFileTest.java<br/>
 * created: 29.11.2008 11:39 <br/>
 *<br/><br/>
 * <b>Write some testdata into an OsmBinDataSet and retrieve it.</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class IDIndexFileTest {


    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(IDIndexFileTest.class
            .getName());

    /**
     * The class we are testing.
     */
    private IDIndexFile mySubject;

    /**
     * A temporary directory for our subject to use.
     */
    private File myTempFile;

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "IDIndexFileTest@" + hashCode();
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        this.myTempFile = File.createTempFile("OsmBinDataSetTest", null);
        this.mySubject = new IDIndexFile(this.myTempFile);
        this.myTempFile.deleteOnExit();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        this.mySubject.close();
        this.myTempFile.delete();
    }

    /**
     * Test method for {@link org.openstreetmap.osm.data.OsmBinDataSet#addNode(org.openstreetmap.osmosis.core.domain.v0_5.Node)}.
     * @throws IOException may happen
     */
    @Test
    public void testReplacing() throws IOException {
        final long value1 = 100;
        final long value2 = 200;
        final long value3 = 300;
        final long key1 = 1;
        final long key2 = 2;
        final long key3 = 3;
        this.mySubject.put(key1, value1);
        this.mySubject.put(key2, value2);
        this.mySubject.put(key3, value3);

        assertEquals(value1, this.mySubject.get(key1));
        assertEquals(value2, this.mySubject.get(key2));
        assertEquals(value3, this.mySubject.get(key3));

        this.mySubject.put(2, value2 + 1);
        assertEquals(value1, this.mySubject.get(key1));
        assertEquals(value2 + 1, this.mySubject.get(key2));
        assertEquals(value3, this.mySubject.get(key3));

        this.mySubject.remove(2);
        assertEquals(value1, this.mySubject.get(key1));
        assertEquals(-1, this.mySubject.get(key2));
        assertEquals(value3, this.mySubject.get(key3));
    }

}


