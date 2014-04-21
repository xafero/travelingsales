/**
 * JosmMapFeatures.java
 * (c) 2008 by <a href="mailto:oleg.chubaryov@mail.ru">Oleg Chubaryov</a>
 * This file is part of osmnavigation by Marcus Wolschon <a href="mailto:Marcus@Wolscon.biz">Marcus@Wolscon.biz</a>.
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
package org.openstreetmap.travelingsalesman.painting.odr;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import org.openstreetmap.osm.ConfigurationSection;
import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.travelingsalesman.painting.ImageResources;

/**
 * Features set for ODR painting painting.
 * @author <a href="mailto:oleg_chubaryov@mail.ru">Oleg Chubaryov </a>
 */
public class JosmMapFeatures implements IMapFeaturesSet {

    /**
     * Automatically created logger for debug and error-output.
     */
    private static final Logger LOG = Logger.getLogger(JosmMapFeatures.class
            .getName());

    /**
     * Rendered caption cache.
     */
    //private HashMap<String, BufferedImage> captionCache = new HashMap<String, BufferedImage>();

    /**
     * Path contains icons inside jar.
     */
    private static final String IMAGE_PATH = "standard/icons/";

    /**
     * This plugin has no  settings, thus this method returns null
     * as described in {@link IPlugin#getSettings()}.
     * @return null
     */
    public ConfigurationSection getSettings() {
        return null;
    }

    /**
     * Render and store or fetch given caption from the cache.
     * @param caption Caption text
     * @return the image or null
     * @see #captionCache
     */
    /*
    private BufferedImage getCaption(final String caption, final Graphics2D g) {
        if (!captionCache.containsKey(caption)) {
            // render caption text to cache
            //BufferedImage img = g.getDeviceConfiguration().createCompatibleImage(100,100,Transparency.BITMASK);

            final int hFontSize = 16;
            Font font = new Font("", Font.BOLD | Font.ITALIC, hFontSize);
            g.setFont(font);
            FontMetrics fm = g.getFontMetrics();
            //graphicsContext.drawString(name, centerPoint.x - (fm.stringWidth(name) / 2), centerPoint.y - 8);
            BufferedImage img = g.getDeviceConfiguration().createCompatibleImage(fm.stringWidth(caption), hFontSize, Transparency.BITMASK);
            Graphics gr = img.getGraphics();
            gr.setFont(font);
            gr.setColor(Color.BLACK);
            gr.drawString(caption, 0, hFontSize);
            captionCache.put(caption, img);
        }
        return captionCache.get(caption);
    }
*/

    /**
     * Load the given icon or fetch it from the cache.
     * @param ifilename the relative path to the icon without a leasing /.
     * @return the image or null
     * @see #imageCache
     * @see #IMAGE_PATH
     */
    private BufferedImage getImage(final String ifilename) {
        return ImageResources.getImage(IMAGE_PATH + ifilename);
    }

    /**
     * {@inheritDoc}
     */
    public void paintATM(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("money/atm.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void paintBusStation(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("transport/bus.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void paintHospital(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("health/hospital.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

    // /images/standard/icons/vehicle/parking.png
    /**
     * {@inheritDoc}
     */
    public void paintParking(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("vehicle/parking.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void paintPharmacy(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("health/pharmacy.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void paintPostBox(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("public/post_box.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void paintPostOffice(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("public/post_office.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void paintRailwayStation(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("transport/railway.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void paintToilets(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("public/toilets.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void paintFuel(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("vehicle/fuel_station.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

   /**
    * {@inheritDoc}
    */
   public void paintTramStop(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("transport/tram.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void paintBusStop(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("transport/bus_small.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void paintUniversity(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("education/university.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void paintAerodrome(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("transport/airport.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
     }
    /**
     * {@inheritDoc}
     */
    public void paintBank(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("money/bank.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void paintBicycleParking(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("vehicle/parking/bicycle.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void paintCarSharing(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("vehicle/car_sharing.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void paintCourthouse(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("public/administration/court_of_law.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void paintFountain(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("misc/landmark/spring.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void paintHotel(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("accommodation/hotel.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void paintMonument(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("sightseeing/monument.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void paintPlaceOfWorship(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("religion/church.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void paintPolice(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("public/police.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void paintSchool(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("education/school.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /**
     * {@inheritDoc}
     */
    public void paintSubwayEntrance(final Point centerPoint, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("transport/underground.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
    }
    /* Paint places captions */
    /**
     * {@inheritDoc}
     */
    public void paintCity(final Point centerPoint, final String name, final Graphics2D graphicsContext) {
        //BufferedImage img = this.getCaption(name, graphicsContext);
        //graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        final int hFontSize = 16;
        graphicsContext.setFont(new Font("", Font.BOLD | Font.ITALIC, hFontSize));
        FontMetrics fm = graphicsContext.getFontMetrics();
        final int yoffset = 8;
        graphicsContext.drawString(name, centerPoint.x - (fm.stringWidth(name) / 2), centerPoint.y - yoffset);
    }
    /**
     * {@inheritDoc}
     */
    public void paintTown(final Point centerPoint, final String name, final Graphics2D graphicsContext) {
        final int hFontSize = 12;
        graphicsContext.setFont(new Font("", Font.BOLD, hFontSize));
        FontMetrics fm = graphicsContext.getFontMetrics();
        final int yoffset = 6;
        graphicsContext.drawString(name, centerPoint.x - (fm.stringWidth(name) / 2), centerPoint.y - yoffset);
    }
    /**
     * {@inheritDoc}
     */
    public void paintHamlet(final Point centerPoint, final String name, final Graphics2D graphicsContext) {
        if (graphicsContext == null) {
            throw new IllegalArgumentException("null graphicsContext given");
        }
        final int hFontSize = 10;
        graphicsContext.setFont(new Font("", Font.PLAIN, hFontSize));
        FontMetrics fm = graphicsContext.getFontMetrics();
        if (fm == null) {
            //here we try to work around bug #175
            // http://travelingsales.sourceforge.net/bugs/view.php?id=175
            LOG.severe("Cannot get Font-Metrics for the font \"\"!");
            final String[] availableFontFamilyNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            graphicsContext.setFont(new Font(availableFontFamilyNames[0], Font.PLAIN, hFontSize));
            fm = graphicsContext.getFontMetrics();
        }
        final int yoffset = 5;
        graphicsContext.drawString(name, centerPoint.x - (fm.stringWidth(name) / 2), centerPoint.y - yoffset);
    }
    /**
     * {@inheritDoc}
     */
    public void paintSuburb(final Point centerPoint, final String name, final Graphics2D graphicsContext) {
        if (name == null) {
            LOG.warning("not painting suburb without name");
            return;
        }
        final int hFontSize = 12;
        graphicsContext.setFont(new Font("", Font.ITALIC, hFontSize));
        FontMetrics fm = graphicsContext.getFontMetrics();
        final int yoffset = 6;
        graphicsContext.drawString(name, centerPoint.x - (fm.stringWidth(name) / 2), centerPoint.y - yoffset);
    }
    /**
     * {@inheritDoc}
     */
    public void paintVillage(final Point centerPoint, final String name, final Graphics2D graphicsContext) {
        final int hFontSize = 9;
        graphicsContext.setFont(new Font("", Font.PLAIN, hFontSize));
        FontMetrics fm = graphicsContext.getFontMetrics();
        final int yoffset = 4;
        graphicsContext.drawString(name, centerPoint.x - (fm.stringWidth(name) / 2), centerPoint.y - yoffset);
    }

    /**
     * {@inheritDoc}
     */
    public void paintShop(final Point centerPoint, final String shopKind, final Graphics2D graphicsContext) {
        BufferedImage img = this.getImage("shopping/mall.png");
        if (shopKind.equals("supermarket"))
            img = this.getImage("shopping/supermarket.png");
        else if (shopKind.equals("convenience"))
            img = this.getImage("shopping/supermarket.png");
        else if (shopKind.equals("department_store"))
            img = this.getImage("shopping/mall.png");
        else if (shopKind.equals("mall"))
            img = this.getImage("shopping/mall.png");
        else if (shopKind.equals("shop_center"))
            img = this.getImage("shopping/mall.png");
        else if (shopKind.equals("jewelry"))
            img = this.getImage("shopping/jewelry.png");
        else if (shopKind.equals("kiosk"))
            img = this.getImage("shopping/kiosk.png");
        else if (shopKind.equals("butcher"))
            img = this.getImage("shopping/groceries/butcher.png");
        else if (shopKind.equals("bakery"))
            img = this.getImage("shopping/groceries/bakery.png");
        else if (shopKind.equals("bicycle"))
            img = this.getImage("sports/bicycle.png");
        else if (shopKind.equals("cycle_repair"))
            img = this.getImage("sports/bicycle.png");
        else if (shopKind.equals("florist"))
            img = this.getImage("shopping/florist.png");
        else if (shopKind.equals("groceries"))
            img = this.getImage("shopping/groceries/fruits.png");
        else if (shopKind.equals("organic"))
            img = this.getImage("shopping/groceries/fruits.png");
        else if (shopKind.equals("beverages"))
            img = this.getImage("shopping/beverages.png");
        else if (shopKind.equals("books"))
            img = this.getImage("shopping/rental/library.png");
        else if (shopKind.equals("car"))
            img = this.getImage("transport/car.png");
        else if (shopKind.equals("car_dealer"))
            img = this.getImage("transport/car.png");
        else if (shopKind.equals("car_repair"))
            img = this.getImage("transport/car.png");
        else if (shopKind.equals("chemist"))
            img = this.getImage("shopping/chemist.png");
        else if (shopKind.equals("clothes"))
            img = this.getImage("shopping/clothes.png");
        else if (shopKind.equals("computer"))
            img = this.getImage("shopping/computer.png");
        else if (shopKind.equals("electronics"))
            img = this.getImage("shopping/electronics.png");
        else if (shopKind.equals("hifi"))
            img = this.getImage("shopping/hifi.png");
        else if (shopKind.equals("furniture"))
            img = this.getImage("shopping/furniture.png");
        else if (shopKind.equals("garden_centre"))
            img = this.getImage("shopping/garden_centre.png");
        else if (shopKind.equals("hardware"))
            img = this.getImage("shopping/diy_store.png");
        else if (shopKind.equals("doityourself"))
            img = this.getImage("shopping/diy_store.png");
        else if (shopKind.equals("hairdresser"))
            img = this.getImage("shopping/hairdresser.png");
        else if (shopKind.equals("shoes"))
            img = this.getImage("shopping/shoes.png");
        else if (shopKind.equals("toys"))
            img = this.getImage("shopping/toys.png");
        else if (shopKind.equals("video"))
            img = this.getImage("shopping/video.png");
        else if (shopKind.equals("dry_cleaning"))
            img = this.getImage("shopping/clothes.png");
        else if (shopKind.equals("laundry"))
            img = this.getImage("shopping/laundry.png");
        else if (shopKind.equals("outdoor"))
            img = this.getImage("shopping/sports/outdoor.png");
        else if (shopKind.equals("optician"))
            img = this.getImage("shopping/optician.png");
        else if (shopKind.equals("motorcycle"))
            img = this.getImage("vehicle/motorbike.png");
        else if (shopKind.equals("newsagent"))
            img = this.getImage("shopping/kiosk.png");
        else if (shopKind.equals("tailor"))
            img = this.getImage("shopping/tailor.png");
        else if (shopKind.equals("travel_agency"))
            img = this.getImage("places/island.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
   }

    /**
     * {@inheritDoc}
     */
    public void paintAmenity(final Point centerPoint, final String amenityType, final Graphics2D graphicsContext) {
        BufferedImage img = null;
        if (amenityType.equals("pub"))
            img = this.getImage("food/pub.png");
        else if (amenityType.equals("biergarten"))
            img = this.getImage("food/biergarten.png");
        else if (amenityType.equals("nightclub"))
            img = this.getImage("recreation/nightclub.png");
        else if (amenityType.equals("cafe"))
            img = this.getImage("food/cafe.png");
        else if (amenityType.equals("restaurant"))
            img = this.getImage("food/restaurant.png");
        else if (amenityType.equals("fast_food"))
            img = this.getImage("food/fastfood.png");
        else if (amenityType.equals("bar"))
            img = this.getImage("food/bar.png");
        else if (amenityType.equals("icecream"))
            img = this.getImage("food/icecream.png");
        else if (amenityType.equals("drinking_water"))
            img = this.getImage("food/drinking_water.png");
        else if (amenityType.equals("fountain"))
            img = this.getImage("misc/landmark/spring.png");
        else if (amenityType.equals("telephone"))
            img = this.getImage("public/telephone.png");
        else if (amenityType.equals("emergency_phone"))
            img = this.getImage("vehicle/emergency_phone.png");
        else if (amenityType.equals("theatre"))
            img = this.getImage("recreation/theater.png");
        else if (amenityType.equals("cinema"))
            img = this.getImage("recreation/cinema.png");
        else if (amenityType.equals("recycling"))
            img = this.getImage("public/recycling.png");
        else if (amenityType.equals("public_building") || amenityType.equals("embassy") || amenityType.equals("town_hall") || amenityType.equals("community_cenre"))
            img = this.getImage("service.png");
        else if (amenityType.equals("gave_yard"))
            img = this.getImage("religion/cemetery.png");
        else if (amenityType.equals("library"))
            img = this.getImage("shopping/rental/library.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
   }

    /**
     * {@inheritDoc}
     */
    public void paintTourism(final Point centerPoint, final String tourismNodeType, final Graphics2D graphicsContext) {
        BufferedImage img = null;
        if (tourismNodeType.equals("museum"))
            img = this.getImage("sightseeing/museum.png");
        else if (tourismNodeType.equals("hotel"))
            img = this.getImage("accommodation/hotel.png");
        else if (tourismNodeType.equals("motel"))
            img = this.getImage("accommodation/motel.png");
        else if (tourismNodeType.equals("guest_house"))
            img = this.getImage("accommodation/guest_house.png");
        else if (tourismNodeType.equals("hostel"))
            img = this.getImage("accommodation/hostel.png");
        else if (tourismNodeType.equals("chalet"))
            img = this.getImage("accommodation/chalet.png");
        else if (tourismNodeType.equals("camp_site"))
            img = this.getImage("accommodation/camping.png");
        else if (tourismNodeType.equals("caravan_site"))
            img = this.getImage("accommodation/camping/caravan.png");
        else if (tourismNodeType.equals("picnic_site"))
            img = this.getImage("recreation/picnic.png");
        else if (tourismNodeType.equals("viewpoint"))
            img = this.getImage("sightseeing/viewpoint.png");
        else if (tourismNodeType.equals("theme_park"))
            img = this.getImage("recreation/theme_park.png");
        else if (tourismNodeType.equals("attraction"))
            img = this.getImage("sightseeing.png");
        else if (tourismNodeType.equals("zoo"))
            img = this.getImage("recreation/zoo.png");
        else if (tourismNodeType.equals("art_work"))
            img = this.getImage("public/arts_centre.png");
        if (img != null) {
            graphicsContext.drawImage(img, null, centerPoint.x - img.getWidth() / 2, centerPoint.y - img.getHeight() / 2);
        }
   }

}
