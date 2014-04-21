/**
 * This file is part of LibOSM by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
 * You can purchase support for a sensible hourly rate or
 * a commercial license of this file (unless modified by others) by contacting him directly.
 *
 *  LibOSM is free software: you can redistribute it and/or modify
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
 *  along with LibOSM.  If not, see <http://www.gnu.org/licenses/>.
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
package org.openstreetmap.osm.io;


import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.IDataSet;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.coordinates.Bounds;
import org.openstreetmap.osm.data.coordinates.LatLon;

/*import org.openstreetmap.osmosis.core.migrate.MigrateV05ToV06;*/ // TODO: Check migrate!
import org.openstreetmap.osmosis.xml.v0_6.XmlDownloader;

/**
 * The BoundingBoxDownloader can download all
 * <a hef="http://wiki.openstreetmap.org/index.php/Develop">OpenStreetMap</a>-Data within a given bounding-box.<br/>
 * by utilizing <a href="http://wiki.openstreetmap.org/index.php/Osmosis">Osmosis</a>.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class BoundingBoxDownloader {

    /**
     * The bounding-box to download.
     */
    private double mylat0;

    /**
     * The bounding-box to download.
     */
    private double mylon0;

    /**
     * The bounding-box to download.
     */
    private double mylat1;

    /**
     * The bounding-box to download.
     */
    private double mylon1;

    /**
     * Prepare downloading and parsing the given bounding-box.
     * (Start downloading with {@link #parseOsm()}}).
     * @param aMylat0 the bounding-box to download.
     * @param aMylon0 the bounding-box to download.
     * @param aMylat1 the bounding-box to download.
     * @param aMylon1 the bounding-box to download.
     */
    public BoundingBoxDownloader(final double aMylat0, final double aMylon0,
                                 final double aMylat1, final double aMylon1) {
        super();
        this.mylat0 = Math.min(aMylat0, aMylat1);
        this.mylon0 = Math.min(aMylon0, aMylon1);
        this.mylat1 = Math.max(aMylat0, aMylat1);
        this.mylon1 = Math.max(aMylon0, aMylon1);
    }

    /**
     * Prepare downloading and parsing the given bounding-box.
     * (Start downloading with {@link #parseOsm()}}).
     * @param aCorner0 one of the 2 corners spanning the  bounding-box to download.
     * @param aCorner1 one of the 2 corners spanning the  bounding-box to download.
     */
    public BoundingBoxDownloader(final LatLon aCorner0,
                                 final LatLon aCorner1) {
        super();
        this.mylat0 = Math.min(aCorner0.lat(), aCorner1.lat());
        this.mylon0 = Math.min(aCorner0.lon(), aCorner1.lon());
        this.mylat1 = Math.max(aCorner0.lat(), aCorner1.lat());
        this.mylon1 = Math.max(aCorner0.lon(), aCorner1.lon());
    }

    /**
     * Prepare downloading and parsing the given bounding-box.
     * (Start downloading with {@link #parseOsm()}}).
     * @param aBoundingBox the  bounding-box to download.
     */
    public BoundingBoxDownloader(final Bounds aBoundingBox) {
        super();
        this.mylat0 = aBoundingBox.getMin().lat();
        this.mylon0 = aBoundingBox.getMin().lon();
        this.mylat1 = aBoundingBox.getMax().lat();
        this.mylon1 = aBoundingBox.getMax().lon();
    }


    /**
     * Start downloading and parsing the given bounding-box.
     * @return the downloaded DataSet or null.
     */
    public MemoryDataSet parseOsm() {
        return (MemoryDataSet) parseOsm(new MemoryDataSet());
    }

    /**
     * Start downloading and parsing the given bounding-box.
     * @return the downloaded DataSet or null.
     * @param aWriteTo the map to write to
     */
    public IDataSet parseOsm(final IDataSet aWriteTo) {

        //OSMFactory.setFactory(new org.openstreetmap.osm.data.ExtendedNodeFactory(this.myDataSet));

        String serverBaseURL = Settings.getInstance().get("osm.ServerBaseURL.v0.6", "http://www.openstreetmap.org/api/0.6");
        if (serverBaseURL.endsWith("/")) {
            serverBaseURL = serverBaseURL.substring(serverBaseURL.length() - 1);
        }
        //System.out.println(serverBaseURL + "/map?bbox=" + this.mylat0 + "," + this.mylon0 + "," + this.mylat1 + "," + this.mylon1);
        XmlDownloader task = new XmlDownloader(this.mylon0, this.mylon1, this.mylat0, this.mylat1, serverBaseURL);

        DataSetSink sink = new DataSetSink(aWriteTo);

        task.setSink(sink);
        try {
            task.run();
        } catch (org.openstreetmap.osmosis.core.OsmosisRuntimeException e) {
            if (e.getMessage().indexOf("Received API HTTP response code 403") != -1) {
                String oldServerBaseURL = Settings.getInstance().get("osm.ServerBaseURL.v0.5", "http://www.openstreetmap.org/api/0.5");
                if (oldServerBaseURL.endsWith("/")) {
                    oldServerBaseURL = oldServerBaseURL.substring(serverBaseURL.length() - 1);
                }
                org.openstreetmap.osmosis.xml.v0_6.XmlDownloader oldTask =
                    new org.openstreetmap.osmosis.xml.v0_6.XmlDownloader(this.mylon0, this.mylon1, this.mylat0, this.mylat1, oldServerBaseURL);
                /*MigrateV05ToV06 migrate = new MigrateV05ToV06();
                oldTask.setSink(migrate);
                migrate.setSink(sink);*/ // TODO: Check migrate!
                oldTask.run();
            } else {
                throw e;
            }
        }

        return (MemoryDataSet) sink.getDataSet();
    }

}
