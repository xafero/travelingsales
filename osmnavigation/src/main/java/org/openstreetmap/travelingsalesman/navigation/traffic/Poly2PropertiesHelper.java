/**
 * 
 */
package org.openstreetmap.travelingsalesman.navigation.traffic;

import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;

import org.openstreetmap.osmosis.core.filter.common.PolygonFileReader;

/**
 * Helper-program to convert a .poly -file
 * into a template-properties -files for a country.
 */
public class Poly2PropertiesHelper {

    /**
     * @param args the directory
     */
    public static void main(final String[] args) {

        try {
            File dir = new File(".");
            if (args.length == 1) {
                dir = new File(args[0]);
            }
            new Poly2PropertiesHelper().createCountriesProperties(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param dir a direcory with .poly -files
     * @throws IOException if we cannot read or write
     */
    public void createCountriesProperties(final File dir) throws IOException {
        File[] poly = dir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(final File aDir, final String aName) {
                return aName.endsWith(".poly");
            }
        });
        File outfile = new File(dir, "countries.properties");
        Properties out = new Properties();
        if (outfile.exists()) {
            System.out.println("loading " + outfile.getName());
            out.load(new FileInputStream(outfile));
        }

        for (File polyFile : poly) {
            System.out.println("loading " + polyFile.getName());
            String countryCode = polyFile.getName().replace(".poly", "");
            PolygonFileReader reader = new PolygonFileReader(polyFile);
            Area polygon = reader.loadPolygon();
            String countryName = reader.getPolygonName();
            Rectangle2D bounds2D = polygon.getBounds2D();
            if (!out.containsKey(countryCode)) {
                out.put(countryCode, countryCode);
                out.put(countryCode + ".name", countryName);
                out.put(countryCode + ".maxlat", "" + bounds2D.getMaxY());
                out.put(countryCode + ".maxlon", "" + bounds2D.getMaxX());
                out.put(countryCode + ".minlat", "" + bounds2D.getMinY());
                out.put(countryCode + ".minlon", "" + bounds2D.getMinX());
            }
        }
        System.out.println("writing new " + outfile.getName());
        out.store(new FileOutputStream(outfile), "");
    }

}
