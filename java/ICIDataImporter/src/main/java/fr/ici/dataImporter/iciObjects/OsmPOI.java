package fr.ici.dataImporter.iciObjects;

import fr.ici.dataImporter.util.Util;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

/**
 * OpenStreetMap point of interest. types are gotten from the https://geodatamine.fr/ project
 */
public class OsmPOI extends POI {
    public static File nomenclatureFile = new File(Util.getRootFolder(), "OSM/nomenclatureOSM.csv");
    public String capacity, outdoor;

    public OsmPOI(String nAddress, String address, String typeRoad, String postCode, String amenityName, String name, Point p, String capacity, String openingHour, String outdoor) throws InvalidPropertiesFormatException {
        super("POI-" + Attribute.makeUniqueId(), nAddress, address, typeRoad, postCode, amenityName, amenityName, getIciAmenity(amenityName, "OSM"), "OSM", name, openingHour, p);
        this.capacity = capacity;
        this.outdoor = outdoor;
    }

    public static void main(String[] args) throws IOException {
        List<OsmPOI> l = importOsmPOIGeoDataMine(new File("../../osm/output/OSM-POI/"));
        POI.exportListPOI(l, new File("/tmp/osmPOI.gpkg"));
    }

    /**
     * import a list of OSM POI from a file. Can be used on an overpass dump or on single geodatamine dump.
     *
     * @param osmPOIFile the dump from OSM
     * @return
     * @throws IOException
     */
    public static List<OsmPOI> importOsmPOI(File osmPOIFile) throws IOException {
        DataStore ds = CollecMgmt.getDataStore(osmPOIFile);
        List<OsmPOI> lB = new ArrayList<>();
        try (SimpleFeatureIterator fIt = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().features()) {
            while (fIt.hasNext()) {
                SimpleFeature feat = fIt.next();
                Point pt;
                Geometry geom = (Geometry) feat.getDefaultGeometry();
                if (geom instanceof Point)
                    pt = (Point) geom;
                else
                    pt = geom.getInteriorPoint();
                lB.add(new OsmPOI(feat.getAttribute("addr:housenumber") != null ? (String) feat.getAttribute("addr:housenumber") : "",
                        feat.getAttribute("addr:street") != null ? ((String) feat.getAttribute("addr:street")).split("\\s")[0] : "",
                        feat.getAttribute("addr:street") != null ? ((String) feat.getAttribute("addr:street")).replace(((String) feat.getAttribute("addr:street")).split("\\s")[0], "") : "",
                        feat.getAttribute("addr:postcode") != null ? (String) feat.getAttribute("addr:postcode") : feat.getAttribute("com_insee") != null  ? (String) feat.getAttribute("com_insee") : "" ,
                        feat.getAttribute("amenity") != null ? (String) feat.getAttribute("amenity") : feat.getAttribute("type") != null ? (String) feat.getAttribute("type") : osmPOIFile.getName().replace(".gpkg","") ,
                        feat.getAttribute("name") != null ? (String) feat.getAttribute("name") : "",
                        pt,
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

    /**
     * import a list of OSM POI from an geodatamine dump
     *
     * @param osmFolderOutput
     * @return
     * @throws IOException
     */
    public static List<OsmPOI> importOsmPOIGeoDataMine(File osmFolderOutput) throws IOException {
        List<OsmPOI> lB = new ArrayList<>();
        for (File f : osmFolderOutput.listFiles())
            if(f.getName().endsWith(".gpkg"))
                lB.addAll(importOsmPOI(f));
        return lB;
    }

    private static String isOutdoor(SimpleFeature feat) {
        return feat.getAttribute("outdoor_seating") != null ? (String) feat.getAttribute("outdoor_seating") : "";
    }

    public String getCapacity(){
        return capacity;
    }
}
