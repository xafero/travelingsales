package org.openstreetmap.travelingsalesman.painting.odr;

import java.awt.Graphics2D;
import java.awt.Point;

import org.openstreetmap.osm.Plugins.IPlugin;
import org.openstreetmap.travelingsalesman.painting.ODRPaintVisitor;


/**
 * this is the interface for POI icon painters.<br/>
 * It is being used by {@link ODRPaintVisitor} to paint
 * points of interest onto the map.
 *
 * @author <a href="mailto:tdad@users.sourceforge.net">Sven Koehler</a>
 */
public interface IMapFeaturesSet extends IPlugin {

    /**
     * paints the icon of a railway station to the given coordinates.
     *
     * @param centerPoint center of the railway station
     * @param graphicsContext graphics context to paint on
     */
    void paintRailwayStation(Point centerPoint, Graphics2D graphicsContext);

    /**
     * paints the icon of a pharmacy.
     *
     * @param centerPoint center of the railway station
     * @param graphicsContext graphics context to paint on
     */
    void paintPharmacy(Point centerPoint, Graphics2D graphicsContext);

    /**
     * paints the icon of a parking ground.
     *
     * @param centerPoint center of the railway station
     * @param graphicsContext graphics context to paint on
     */
    void paintParking(Point centerPoint, Graphics2D graphicsContext);

    /**
     * paints the icon for public toilets.
     *
     * @param centerPoint center of the railway station
     * @param graphicsContext graphics context to paint on
     */
    void paintToilets(Point centerPoint, Graphics2D graphicsContext);

    /**
     * paints the icon of a post office.
     *
     * @param centerPoint center of the railway station
     * @param graphicsContext graphics context to paint on
     */
    void paintPostOffice(Point centerPoint, Graphics2D graphicsContext);

    /**
     * paints the icon of a post box.
     *
     * @param centerPoint center of the railway station
     * @param graphicsContext graphics context to paint on
     */
    void paintPostBox(Point centerPoint, Graphics2D graphicsContext);

    /**
     * paints the icon of a hospital.
     *
     * @param centerPoint center of the hospital
     * @param graphicsContext graphics context to paint on
     */
    void paintHospital(Point centerPoint, Graphics2D graphicsContext);

    /**
     * paints the icon of a bus station.
     *
     * @param centerPoint center of the bus station
     * @param graphicsContext graphics context to paint on
     */
    void paintBusStation(Point centerPoint, Graphics2D graphicsContext);

    /**
     * paints the icon for a atm machine (amenity=bank;atm, amenity=atm).
     *
     * @param centerPoint center of the railway station
     * @param graphicsContext graphics context to paint on
     */
    void paintATM(Point centerPoint, Graphics2D graphicsContext);

    /**
     * paints the icon for a fuel-station (amenity=fuel).
     *
     * @param centerPoint center of the fuel station
     * @param graphicsContext graphics context to paint on
     */
    void paintFuel(Point centerPoint, Graphics2D graphicsContext);
    void paintTramStop(Point centerPoint, Graphics2D graphicsContext);
    void paintBusStop(Point centerPoint, Graphics2D graphicsContext);
    void paintAerodrome(Point centerPoint, Graphics2D graphicsContext);
    void paintSubwayEntrance(Point centerPoint, Graphics2D graphicsContext);
    void paintBicycleParking(Point centerPoint, Graphics2D graphicsContext);
    void paintCarSharing(Point centerPoint, Graphics2D graphicsContext);
    void paintUniversity(Point centerPoint, Graphics2D graphicsContext);
    void paintSchool(Point centerPoint, Graphics2D graphicsContext);
    void paintPlaceOfWorship(Point centerPoint, Graphics2D graphicsContext);
    void paintCourthouse(Point centerPoint, Graphics2D graphicsContext);
    void paintMonument(Point centerPoint, Graphics2D graphicsContext);
    void paintBank(Point centerPoint, Graphics2D graphicsContext);
    void paintFountain(Point centerPoint, Graphics2D graphicsContext);
    void paintPolice(Point centerPoint, Graphics2D graphicsContext);
    void paintCity(Point centerPoint, String nodeCaption, Graphics2D graphicsContext);
    void paintTown(Point centerPoint, String nodeCaption, Graphics2D graphicsContext);
    void paintHamlet(Point centerPoint, String nodeCaption, Graphics2D graphicsContext);
    void paintSuburb(Point centerPoint, String nodeCaption, Graphics2D graphicsContext);
    void paintVillage(Point centerPoint, String nodeCaption, Graphics2D graphicsContext);
    /**
     * Paint one of the shop icon.
     * @param centerPoint center of the node
     * @param shopKind shop kind, e.g. supermarket, electronics, ...
     * @param graphicsContext graphics context to paint on
     */
    void paintShop(Point centerPoint, String shopKind, Graphics2D graphicsContext);
    /**
     * Paint one of the amenity icon.
     * @param centerPoint center of the node
     * @param amenityType amenity type, e.g. telephone, cafe, ...
     * @param graphicsContext graphics context to paint on
     */
    void paintAmenity(Point centerPoint, String amenityType, Graphics2D graphicsContext);
    /**
     * Paint one of the tourism icon.
     * @param centerPoint center of the node
     * @param tourismNodeType tourism node type, e.g. museum, viewpoint, ...
     * @param graphicsContext graphics context to paint on
     */
    void paintTourism(Point centerPoint, String tourismNodeType, Graphics2D graphicsContext);
}
