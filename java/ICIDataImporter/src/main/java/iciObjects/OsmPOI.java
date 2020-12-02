package iciObjects;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class OsmPOI extends POI {
    public String capacity;

    public OsmPOI(String nAddress, String address, String typeRoad, String postCode, String amenityName, String name, Point p, String capacity) {
        super(nAddress, address, typeRoad, postCode, amenityName, amenityName, getIciAmenity(amenityName, "OSM"), "OSM", name, p);
        this.capacity = capacity;
    }

    public static List<OsmPOI> importOsmPOI(File osmPOIFile) throws IOException {
        DataStore ds = Collec.getDataStore(osmPOIFile);
        List<OsmPOI> lB = new ArrayList<>();
        try (SimpleFeatureIterator fIt = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().features()) {
            while (fIt.hasNext()) {
                SimpleFeature feat = fIt.next();
                OsmPOI ap = new OsmPOI(feat.getAttribute("addr:housenumber") != null ? (String) feat.getAttribute("addr:housenumber") : "",
                        feat.getAttribute("addr:street") != null ? ((String) feat.getAttribute("addr:street")).split("\\s")[0] : "",
                        feat.getAttribute("addr:street") != null ? ((String) feat.getAttribute("addr:street")).replace(((String) feat.getAttribute("addr:street")).split("\\s")[0],"") : "",
                        feat.getAttribute("addr:postcode") != null ? (String) feat.getAttribute("addr:postcode") : "",
                        feat.getAttribute("amenity") != null ? (String) feat.getAttribute("amenity") : "",
                        feat.getAttribute("name") != null ? (String) feat.getAttribute("name") : "",
                        (Point) feat.getDefaultGeometry(),
                        feat.getAttribute("capacity") != null ? (String) feat.getAttribute("capacity") : ""
                        );
                lB.add(ap);
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        ds.dispose();
        return lB;
    }

}
