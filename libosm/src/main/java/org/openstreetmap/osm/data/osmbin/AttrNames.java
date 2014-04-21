/**
 * AttrNames.java
 * created: 16.11.2008 12:05:00
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
package org.openstreetmap.osm.data.osmbin;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * (c) 2008 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * AttrNames.java<br/>
 * created: 14.11.2008<br/>
 *<br/><br/>
 * <b>This is the attrnames.txt-file as described in
 * <a href="http://wiki.openstreetmap.org/index.php/User:MarcusWolschon%5Cosmbin_draft">here</a></b>
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class AttrNames {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(AttrNames.class
            .getName());
    /**
     * All attribute-names.
     * The ID is their index + Short.MIN_VALUE + 1;
     */
    private List<String> myAttrNames = new ArrayList<String>();
    /**
     * The filename of the attrnames.txt -file.
     */
    private File myFileName;
    /**
     * @see #appendToFile(String).
     */
//    private Writer myAppendWriter;

    /**
     * @param aFileName The filename of the attrnames.txt -file.
     * @throws IOException if the file cannot be opened/created.
     */
    public AttrNames(final File aFileName) throws IOException {
        setFileName(aFileName);
        loadFile();
    }
    /**
     * Load the file into memory.
     * @throws IOException if we cannot load
     */
    private void loadFile() throws IOException {
        File file = getFileName();
        this.myAttrNames.clear();
        if (!file.exists()) {
            return;
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        String line = reader.readLine();
        while (line != null) {
            this.myAttrNames.add(line);
            line = reader.readLine();
        }
    }

    /**
     * Save the file that contains the attribute-names.
     * @throws IOException if something happens
     */
    private void saveFile() throws IOException {

        long start =  System.currentTimeMillis();
        LOG.info("saving attribute-names-file");
        // we write to e temp-file in the same directory,
        // so the attrnames-file is not destroyed
        // due to an interrupted write
        File file = getFileName();
        File fileTmp = new File(file.getAbsolutePath() + "tmp");
        file.getParentFile().mkdirs();
        Writer out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(fileTmp)), "UTF-8");

        for (String attrName : this.myAttrNames) {
            out.write(attrName);
            out.write('\n');
        }
        out.close();
        if (file.exists()) {
            file.delete();
        }
        fileTmp.renameTo(file);

        // done
        final double msPerSecond = 1000.0;
        LOG.info("saving attribute-names-file ...done "
                + ((System.currentTimeMillis() - start) / msPerSecond) + " seconds");
    }

    /**
     * Instead of writing the complete file, only append
     * the given attribute. It Must not be present yet
     * in the file and have the ID corresponding to it's
     * new location in the file.
     * @param attrName the attribute to append.
     * @throws IOException if we cannot write
     */
  //this leaves empty lines in the file     private void appendToFile(final String attrName) throws IOException {
//        long start = System.currentTimeMillis();
//        if (myAppendWriter == null) {
//            File file = getFileName();
//            file.getParentFile().mkdirs();
//            myAppendWriter = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file, true)), "UTF-8");
//        }
//        myAppendWriter.write(attrName.replace('\n', ' '));
//        myAppendWriter.write('\n');
//        myAppendWriter.flush();
//     // done
//        final double msPerSecond = 1000.0;
//        double duration = System.currentTimeMillis() - start;
//        if (duration > msPerSecond) {
//            LOG.info("appending to attribute-names-file ...took exceptionally long "
//                    + (duration / msPerSecond) + " seconds");
//        }
//    }

    /**
     * Close all open streams.
     * @throws IOException if we cannot close the writer
     */
    public void close() throws IOException {
//        if (myAppendWriter != null) {
//            myAppendWriter.close();
//            myAppendWriter = null;
//        }
    }

    /**
     * @param key the key to look up
     * @return the attribute-name for that key
     */
    public String getAttributeName(final short key) {
        if (key == Short.MIN_VALUE) {
            return null; // reserved key for the empty attribut-name
        }
        if (key == Short.MIN_VALUE - 1) {
            return null; // reserved key for the empty attribut-name
        }
        if ((key - Short.MIN_VALUE - 2) >= this.myAttrNames.size()) {
            return null;
        }
        return this.myAttrNames.get(key - Short.MIN_VALUE - 2);
    }

    /**
     * Get the key for this attribute-name but do not
     * create a new key if it does not exist.
     * @param anAttrName the attribut-name to look up
     * @return the key for this attribute-name of Short.MIN_VALUE
     */
    public short getKey(final String anAttrName) {
        short key = Short.MIN_VALUE + 2;
        if (anAttrName == null) {
            return Short.MIN_VALUE;
        }
        String cleanedAttrName = anAttrName.replace('\n', ' ').replace('\r', ' ').trim();
        if (cleanedAttrName.length() == 0) {
            return Short.MIN_VALUE;
        }
        for (String attrName : this.myAttrNames) {
            if (attrName.equals(cleanedAttrName)) {
                return key;
            }
            key++;
        }
        return Short.MIN_VALUE;
    }
    /**
     * @param anAttrName the attribut-name to look up
     * @return the key for this attribute-name (the values Short.MIN_VALUE and Short.MIN_VALUE+1 are not used as keys)
     * @throws IOException if we cannot save the file
     */
    public short getOrCreateKey(final String anAttrName) throws IOException {
        if (anAttrName == null) {
            return Short.MIN_VALUE;
        }
        String cleanedAttrName = anAttrName.replace('\n', ' ').replace('\r', ' ').trim();
        if (cleanedAttrName.length() == 0) {
            return Short.MIN_VALUE;
        }
        short key = getKey(cleanedAttrName);
        if (key != Short.MIN_VALUE) {
            return key;
        }
        synchronized (this.myAttrNames) {
            key = getKey(cleanedAttrName);
            if (key != Short.MIN_VALUE) {
                return key;
            }
            this.myAttrNames.add(cleanedAttrName);
            //this leaves empty lines in the file appendToFile(anAttrName);
            saveFile();
            return  (short) (Short.MIN_VALUE + 1 + this.myAttrNames.size());
        }
    }
    /**
     * @return the fileName
     */
    protected File getFileName() {
        return myFileName;
    }

    /**
     * @param aFileName the fileName to set
     */
    protected void setFileName(final File aFileName) {
        myFileName = aFileName;
    }
}