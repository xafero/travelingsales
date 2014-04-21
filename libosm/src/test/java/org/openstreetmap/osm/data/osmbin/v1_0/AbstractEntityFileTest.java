/**
 * AbstractEntityFileTest.java
 * created: 21.01.2008
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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.osm.data.osmbin.AttrNames;
import org.openstreetmap.osm.data.osmbin.IDIndexFile;

import org.openstreetmap.osmosis.core.domain.v0_6.Tag;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * AbstractEntityFileTest.java<br/>
 * created: 21.01.2009<br/>
 *<br/><br/>
 * <b>Write some testdata into an OsmBinDataSet and retrieve it.</b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class AbstractEntityFileTest {

    private class Subject extends AbstractEntityFile {

        protected Subject() throws IOException {
            super(File.createTempFile("AbstractEntityFileTest", ".obm"),
                    new IDIndexFile(File.createTempFile("AbstractEntityFileTest", ".idx")),
                    new AttrNames(File.createTempFile("AbstractEntityFileTest", ".txt")));
        }

        @Override
        public int getRecordLength() {
            return 2;
        }
    }
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for {@link org.openstreetmap.osm.data.osmbin.v1_0.AbstractEntityFile#readAttribute(java.nio.ByteBuffer, int, org.openstreetmap.osmosis.core.domain.v0_5.Entity)}.
     * @throws IOException 
     */
    @Test
    public void testReadAttribute() throws IOException {
        Subject subject = new Subject();
        Tag preparedTag = new Tag("abc", "123");

        ByteBuffer mem = ByteBuffer.allocate(2 + preparedTag.getValue().length() * 2);
        subject.writeAttribute(mem, preparedTag.getValue().length(), preparedTag);
        Assert.assertEquals(mem.limit(), mem.position());
        mem.rewind();
        Tag reat = subject.readAttribute(mem, preparedTag.getValue().length(), new ArrayList<Tag>());
        Assert.assertNotNull(reat);
        Assert.assertEquals(preparedTag.getKey(), reat.getKey());
        Assert.assertEquals(preparedTag.getValue(), reat.getValue());
    }

    /**
     * Test method for {@link org.openstreetmap.osm.data.osmbin.v1_0.AbstractEntityFile#prepareTagList(java.util.List, int)}.
     * @throws IOException 
     */
    @Test
    public void testPrepareTagList() throws IOException {
        Subject subject = new Subject();
        Tag shortTag = new Tag("short", "abc");
        Tag longTag = new Tag("long", "1234567890");
        List<Tag> aTagList = new ArrayList<Tag>(2);
        aTagList.add(shortTag);
        aTagList.add(longTag);

        List<Object> preparedTagList = subject.prepareTagList(aTagList, shortTag.getValue().length());
        Assert.assertNotNull(preparedTagList);
        Assert.assertEquals(5, preparedTagList.size());
        Assert.assertEquals(shortTag, preparedTagList.get(0));
        Assert.assertEquals(longTag.getKey(), ((Tag) preparedTagList.get(1)).getKey());
        Assert.assertNull(((Tag) preparedTagList.get(2)).getKey());
        Assert.assertNull(((Tag) preparedTagList.get(3)).getKey());
        Assert.assertNull(((Tag) preparedTagList.get(4)).getKey());
        Assert.assertEquals("1", ((Tag) preparedTagList.get(1)).getValue());
        Assert.assertEquals("234", ((Tag) preparedTagList.get(2)).getValue());
        Assert.assertEquals("567", ((Tag) preparedTagList.get(3)).getValue());
        Assert.assertEquals("890", ((Tag) preparedTagList.get(4)).getValue());
    }

}
