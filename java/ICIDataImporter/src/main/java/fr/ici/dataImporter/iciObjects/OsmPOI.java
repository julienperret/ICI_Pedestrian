package fr.ici.dataImporter.iciObjects;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import fr.ici.dataImporter.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

/**
 * OpenStreetMap point of interest. TODO add leisure objects https://wiki.openstreetmap.org/wiki/Key:leisure
 */
public class OsmPOI extends POI {
    public static File nomenclatureFile = new File(Util.getRootFolder(), "OSM/nomenclatureOSM.csv");
    public String capacity, outdoor;

    public OsmPOI(String nAddress, String address, String typeRoad, String postCode, String amenityName, String name, Point p, String capacity, String openingHour, String outdoor) throws InvalidPropertiesFormatException {
        super(nAddress, address, typeRoad, postCode, amenityName, amenityName, getIciAmenity(amenityName, "OSM"), "OSM", name,openingHour, p);
        this.capacity = capacity;
        this.outdoor = outdoor;
    }

    public static List<OsmPOI> importOsmPOI(File osmPOIFile) throws IOException {
        DataStore ds = CollecMgmt.getDataStore(osmPOIFile);
        List<OsmPOI> lB = new ArrayList<>();
        try (SimpleFeatureIterator fIt = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().features()) {
            while (fIt.hasNext()) {
                SimpleFeature feat = fIt.next();
                lB.add(new OsmPOI(feat.getAttribute("addr:housenumber") != null ? (String) feat.getAttribute("addr:housenumber") : "",
                        feat.getAttribute("addr:street") != null ? ((String) feat.getAttribute("addr:street")).split("\\s")[0] : "",
                        feat.getAttribute("addr:street") != null ? ((String) feat.getAttribute("addr:street")).replace(((String) feat.getAttribute("addr:street")).split("\\s")[0], "") : "",
                        feat.getAttribute("addr:postcode") != null ? (String) feat.getAttribute("addr:postcode") : "",
                        feat.getAttribute("amenity") != null ? (String) feat.getAttribute("amenity") : "",
                        feat.getAttribute("name") != null ? (String) feat.getAttribute("name") : "",
                        (Point) feat.getDefaultGeometry(),
                        feat.getAttribute("capacity") != null ? (String) feat.getAttribute("capacity") : "",
                        feat.getAttribute("opening_hours") != null ? (String) feat.getAttribute("opening_hours") : "",
                        isOutdoor(feat)));
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        ds.dispose();
        return lB;
    }
    private static String isOutdoor(SimpleFeature feat){
        return feat.getAttribute("outdoor_seating") != null ? (String) feat.getAttribute("outdoor_seating") : "";
    }
}
