/**
 * AddressDBPlaceFinderTest.java
 * created: 24.11.2007 10:02:12 <br/>
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

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.osm.Settings;
import org.openstreetmap.osm.data.MemoryDataSet;
import org.openstreetmap.osm.data.h2.DatabaseContext;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

/**
 * (c) 2007 by <a href="http://Wolschon.biz>Wolschon Softwaredesign und Beratung</a>.<br/>
 * Project: libosm<br/>
 * AddressDBPlaceFinder.java<br/>
 * created: 24.11.2007 10:02:12 <br/>
 *<br/><br/>
 * Test-cases for the {@link AddressDBPlaceFinder}.
 * @author <a href="mailto:Marcus@Wolschon.biz">Marcus Wolschon</a>
 */
public class AddressDBPlaceFinderTest {

  /**
   * The class we are testing.
   */
  private static AddressDBPlaceFinder mySubject = null;

  /**
   * Store changed value of user preferences.
   */
  private static String userTiledMapCacheDir = null;

  /**
   * Path to temporary folder, should be removed after test.
   */
  private static File tempFile = null;

  /**
   * Database context for correct closing database.
   */
  private static DatabaseContext dbContext = null;

    /**
   * ${@inheritDoc}.
   * @see junit.framework.TestCase#setUp()
   */
  @Before
  public void setUp() throws Exception {

      //--------- logging
      Logger rootLogger = Logger.getLogger("");

      // Remove any existing handlers.
      for (Handler handler : rootLogger.getHandlers()) {
          rootLogger.removeHandler(handler);
      }

      // Add a new console handler.
      Handler consoleHandler = new ConsoleHandler();
      consoleHandler.setLevel(Level.FINEST);
      rootLogger.addHandler(consoleHandler);
      Logger.getLogger("").setLevel(Level.FINEST);
      Logger.getLogger("sun").setLevel(Level.WARNING);
      Logger.getLogger("com.sun").setLevel(Level.WARNING);
      Logger.getLogger("java").setLevel(Level.WARNING);
      Logger.getLogger("javax").setLevel(Level.WARNING);
  }

  /**
   * Entry point for all tests.
   * Define shared fixture here.
   */
  @BeforeClass
  public static void setUpSharedFixture() {
      // Set up shared fixture.
  }

  /**
   * Clean up all used resources.
   * Restore changed values.
   */
  @AfterClass
  public static void tearDownSharedFixture() {
      Settings.getInstance().put("mapdir.dir", userTiledMapCacheDir);
      // close database connection
      dbContext.executeStatement("SHUTDOWN"); // HSQL specific code
      dbContext.release();
      if (tempFile.exists()) {
          tempFile.delete();
      }
  }

  /**
   * @param args ignored
   */
  public static void main(final String[] args) {
      AddressDBPlaceFinderTest test = new AddressDBPlaceFinderTest();
      try {
        test.testFindAddressOfSquareCityMainStreet();
        test.testFindAddressOfRoundCityMainStreet();
        test.testFindAddressOfNoSquareCityMainStreet();
        test.testFindAddressOfNoRoundCityMainStreet();
        test.testFindAddressOfNonExistingStreet();
        test.testFindAddressOfNonExistingCity();
        test.testFindPlaces();
    } catch (IOException e) {
        e.printStackTrace();
    }
  }

    /**
     * find squareCityMainStreet in squareCity.
     * @throws IOException on problemes with temp-files
     */
    @Test
    public void testFindAddressOfSquareCityMainStreet() throws IOException {

        // find squareCityMainStreet in squareCity
        Collection<Place> mainStreetAddr = getSubject().findAddress(null, "squareCityMainStreet", "squareCity", null, null);
        assertNotNull(mainStreetAddr);
        assertEquals(1, mainStreetAddr.size());
        Place mainStreetAddrPlace = mainStreetAddr.iterator().next();
        assertNotNull(mainStreetAddrPlace);
        assertNotNull(mainStreetAddrPlace.getResult());
    }

    /**
     * find roundCityMainStreet in roundCity.
     * @throws IOException on problems with temp-files
     */
    @Test
    public void testFindAddressOfRoundCityMainStreet() throws IOException {
        Collection<Place> mainStreetAddr = getSubject().findAddress(null, "roundCityMainStreet", "roundCity", null, null);
        assertNotNull(mainStreetAddr);
        assertEquals(1, mainStreetAddr.size());
        Place mainStreetAddrPlace = mainStreetAddr.iterator().next();
        assertNotNull(mainStreetAddrPlace);
        assertNotNull(mainStreetAddrPlace.getResult());
        assertTrue(mainStreetAddrPlace instanceof AddressPlace);
        AddressPlace place = (AddressPlace) mainStreetAddrPlace;
        assertEquals("roundSuburb", place.getSubUrbName());
        assertTrue(place.toString().contains("roundSuburb"));
    }

    /**
     * find no squareCityMainStreet in roundCity.
     * @throws IOException on problemes with temp-files
     */
    @Test
    public void testFindAddressOfNoSquareCityMainStreet() throws IOException {

        // find squareCityMainStreet in squareCity
        Collection<Place> mainStreetAddr = getSubject().findAddress(null, "squareCityMainStreet", "roundCity", null, null);
        assertNotNull(mainStreetAddr);
        assertEquals(0, mainStreetAddr.size());
    }

    /**
     * find no roundCityMainStreet in squareCity.
     * @throws IOException on problems with temp-files
     */
    @Test
    public void testFindAddressOfNoRoundCityMainStreet() throws IOException {

        // find squareCityMainStreet in squareCity
        Collection<Place> mainStreetAddr = getSubject().findAddress(null, "roundCityMainStreet", "squareCity", null, null);
        assertNotNull(mainStreetAddr);
        assertEquals(0, mainStreetAddr.size());
    }

    /**
     * Do not find a street that does not exist.
     * @throws IOException on problems with temp-files
     */
    @Test
    public void testFindAddressOfNonExistingStreet() throws IOException {

        // find squareCityMainStreet in squareCity
        Collection<Place> mainStreetAddr = getSubject().findAddress(null, "garbage", "squareCity", null, null);
        assertNotNull(mainStreetAddr);
        assertEquals(0, mainStreetAddr.size());
    }

    /**
     * Do not find a city that does not exist.
     * @throws IOException on problemes with temp-files
     */
    @Test
    public void testFindAddressOfNonExistingCity() throws IOException {

        // find squareCityMainStreet in squareCity
        Collection<Place> mainStreetAddr = getSubject().findAddress(null, "roundCityMainStreet", "garbage", null, null);
        assertNotNull(mainStreetAddr);
        assertEquals(0, mainStreetAddr.size());
    }

    /**
     * Testing the simple search.
     * @throws IOException on problemes with temp-files
     */
    @Test
    public void testFindPlaces() throws IOException {
        Collection<Place> mainStreetAddr = getSubject().findPlaces("squareCityMainStreet");
        assertNotNull(mainStreetAddr);
        assertEquals(1, mainStreetAddr.size());
        Place mainStreetAddrPlace = mainStreetAddr.iterator().next();
        assertNotNull(mainStreetAddrPlace);
        assertNotNull(mainStreetAddrPlace.getResult());
    }

    /**
     * find city by name or part of name.
     * @throws IOException on problems with temp-files
     */
    @Test
    public void testFindAddressByCityName() throws IOException {
        // find squareCity
        Collection<Place> mainAddr = getSubject().findAddress(null, "", "squareCity", null, null);
        assertNotNull(mainAddr);
        assertEquals(1, mainAddr.size());
        // find roundCity
        Collection<Place> mainAddr2 = getSubject().findAddress(null, "", "roundCity", null, null);
        assertNotNull(mainAddr2);
        assertEquals(1, mainAddr2.size());
        // do not find any city
        Collection<Place> mainAddr3 = getSubject().findAddress(null, "", "garbige", null, null);
        assertNotNull(mainAddr3);
        assertEquals(0, mainAddr3.size());
        // find cities by partial name
        Collection<Place> mainAddr4 = getSubject().findAddress(null, "", "city", null, null);
        assertNotNull(mainAddr4);
        assertEquals(2, mainAddr4.size());
        // find cyrillic city
        Collection<Place> mainAddr5 = getSubject().findAddress(null, "", "���", null, null);
        assertNotNull(mainAddr5);
        assertEquals(1, mainAddr5.size());
    }

    /**
     * @return Returns the subject.
     * @throws IOException on problems with temp-files
     * @see #mySubject
     */
    public AddressDBPlaceFinder getSubject() throws IOException {
        if (mySubject == null) {
            tempFile = File.createTempFile("AddressDBPlaceFinderTest", "");
            if (tempFile.exists())
                tempFile.delete();
            tempFile.mkdirs();
            tempFile.deleteOnExit();
            userTiledMapCacheDir = Settings.getInstance().get("mapdir.dir", ".");
            Settings.getInstance().put("mapdir.dir", tempFile.getAbsolutePath() + File.separator);
            (new File(tempFile, "streets.data")).deleteOnExit();
            (new File(tempFile, "streets.log")).deleteOnExit();
            (new File(tempFile, "streets.properties")).deleteOnExit();
            (new File(tempFile, "streets.backup")).deleteOnExit();
            (new File(tempFile, "streets.script")).deleteOnExit();
            MemoryDataSet testMap = new MemoryDataSet();
            createSquareCity(testMap);
            createRoundCity(testMap);
            createRoundSuburb(testMap);
            createCyrillicCity(testMap);

            mySubject = new AddressDBPlaceFinder();
            mySubject.setMap(testMap);
            mySubject.indexDatabase(testMap);
            dbContext = mySubject.getDatabase();
        }

        return mySubject;
    }

    /**
     * Generate cyrillic named test-city.
     * @param testMap the map to add the city to
     */
    private void createCyrillicCity(final MemoryDataSet testMap) {
        final double pos = 10.0d;
        final int nodeID = 654;
        //Collection<Tag> tags = new LinkedList<Tag>();
        Node center   = new Node(nodeID, 0, new Date(), null, 0,  -2 * pos, -2 * pos);
        center.getTags().add(new Tag("name", "������")); //TODO: use the /uXXXX -format here
        center.getTags().add(new Tag("place", "city"));
        testMap.addNode(center);
    }

    /**
     * Generate a test-city bounded by a square polygon.
     * @param testMap the map to add the city to
     */
    private void createRoundCity(final MemoryDataSet testMap) {
        final double pos = 5.0d;
        final double streetPos = 0.001d;
        final int startIndex = 10;
        int i = startIndex;
        Node center   = new Node(i++, 0, new Date(), null, 0, 2 * pos, 2 * pos);
        center.getTags().add(new Tag("name", "roundCity"));
        center.getTags().add(new Tag("place", "city"));
        Node streetLeft  = new Node(i++, 0, new Date(), null, 0, 2 * pos, 2 * pos + streetPos);
        Node streetRight = new Node(i++, 0, new Date(), null, 0, 2 * pos, 2 * pos - streetPos);
        testMap.addNode(center);
        testMap.addNode(streetLeft);
        testMap.addNode(streetRight);
        Way street = new Way(i++, 0, new Date(), null, 0);
        street.getTags().add(new Tag("name", "roundCityMainStreet"));
        street.getTags().add(new Tag("highway", "residential"));
        street.getWayNodes().add(new WayNode(streetLeft.getId()));
        street.getWayNodes().add(new WayNode(streetRight.getId()));
        i = startIndex;
        i++;
        testMap.addNode(center);
        testMap.addWay(street);
    }

    /**
     * Generate a test-city-suburb bounded by a square polygon.
     * @param testMap the map to add the city to
     */
    private void createRoundSuburb(final MemoryDataSet testMap) {
        final double pos = 5.0d;
        final int startIndex = 20;
        int i = startIndex;
        Node center   = new Node(i++, 0, new Date(), null, 0,  2 * pos, 2 * pos);
        center.getTags().add(new Tag("name", "roundSuburb"));
        center.getTags().add(new Tag("place", "suburb"));
        testMap.addNode(center);
        testMap.addNode(center);
    }

    /**
     * Generate a test-city bounded by a square polygon.
     * @param testMap the map to add the city to
     */
    private void createSquareCity(final MemoryDataSet testMap) {
        final double boundsSize = 5.0d;
        int i = 1;
        Node leftUpper   = new Node(i++, 0, new Date(), null, 0,      boundsSize, -1 * boundsSize);
        Node rightUpper  = new Node(i++, 0, new Date(), null, 0,      boundsSize,      boundsSize);
        Node leftLower   = new Node(i++, 0, new Date(), null, 0, -1 * boundsSize, -1 * boundsSize);
        Node rightLower  = new Node(i++, 0, new Date(), null, 0, -1 * boundsSize,      boundsSize);
        Node streetLeft  = new Node(i++, 0, new Date(), null, 0, -1 * boundsSize / 2, boundsSize / 2);
        Node streetRight = new Node(i++, 0, new Date(), null, 0,      boundsSize / 2, boundsSize / 2);
        testMap.addNode(leftUpper);
        testMap.addNode(leftLower);
        testMap.addNode(rightUpper);
        testMap.addNode(rightLower);
        testMap.addNode(streetLeft);
        testMap.addNode(streetRight);

        Way cityBounds = new Way(1, 0, new Date(), null, 0);
        cityBounds.getWayNodes().add(new WayNode(leftUpper.getId()));
        cityBounds.getWayNodes().add(new WayNode(leftLower.getId()));
        cityBounds.getWayNodes().add(new WayNode(rightLower.getId()));
        cityBounds.getWayNodes().add(new WayNode(rightUpper.getId()));
        cityBounds.getWayNodes().add(new WayNode(leftUpper.getId()));
        cityBounds.getTags().add(new Tag("name", "squareCity"));
        cityBounds.getTags().add(new Tag("place", "city"));

        Way street = new Way(2, 0, new Date(), null, 0);
        street.getTags().add(new Tag("name", "squareCityMainStreet"));
        street.getTags().add(new Tag("highway", "residential"));
        street.getWayNodes().add(new WayNode(streetLeft.getId()));
        street.getWayNodes().add(new WayNode(streetRight.getId()));

        i = 1;
        testMap.addWay(cityBounds);
        testMap.addWay(street);
    }

}
