/**
 * ImageResources.java
 * created: 01.02.2009 08:19:53
 * (c) 2009 by <a href="http://Wolschon.biz">Wolschon Softwaredesign und Beratung</a>
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
package org.openstreetmap.travelingsalesman.painting;


import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;



/**
 * (c) 2009 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: osmnavigation<br/>
 * ImageResources.java<br/>
 * created: 01.02.2009 08:19:53 <br/>
 *<br/><br/>
 * Helper-class to deal with images needed for rendering.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public final class ImageResources {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(ImageResources.class
            .getName());

    /**
     * Folder contains icons.
     */
    private static final String IMAGE_PATH = ".." + File.separator
                               + "osmnavigation" + File.separator
                               + "images" + File.separator;
    /**
     * Path contains icons inside jar.
     */
    private static final String JAR_IMAGE_PATH = "images/";

    /**
     * Image cache. Stores the bitmaps of map-features to paint.
     */
    private static HashMap<String, BufferedImage> imageCache = new HashMap<String, BufferedImage>();

    /**
     * Utility-classes have no public constructor.
     * All business-methods are static.
     */
    private ImageResources() {
    }

    /**
     * Load the given icon or fetch it from the cache.
     * @param ifilename the relative path to the icon.
     * @return the image or null
     * @see #imageCache
     * @see #IMAGE_PATH
     */
    public static BufferedImage getImage(final String ifilename) {
        String filename = ifilename.replace('/', File.separatorChar); // Fix slash to system dependent File.separator. (back-slash for Windows)
        if (!imageCache.containsKey(filename)) {
            // load image to cache
            try {
                File imageFile = new File(IMAGE_PATH + filename);
                BufferedImage img = null;
                if (!imageFile.exists()) {
                    // load the icons via getClass().getClassLoader().getResourceAsStream
                    // Thus we can put the icons into the jar-file.
                    filename = ifilename.replace(File.separatorChar, '/');
                    InputStream imageStream = (new ImageResources()).getClass().getClassLoader().getResourceAsStream(JAR_IMAGE_PATH + filename);
                    if (imageStream == null) {
                        LOG.severe("image-resource '" + JAR_IMAGE_PATH + filename + "' not found");
                        return null;
                    }
                    img = ImageIO.read(imageStream);
                    imageStream.close();
                } else {
                    img = ImageIO.read(imageFile);
                }
                // we also store null-values here to not look them up again
                imageCache.put(filename, img);
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Cannot load bitmap '"
                        + filename + "'", e);
            }
        }
        return imageCache.get(filename);
    }

    /**
     * Just an overridden ToString to return this classe's name
     * and hashCode.
     * @return className and hashCode
     */
    public String toString() {
        return "ImageResources@" + hashCode();
    }
}


