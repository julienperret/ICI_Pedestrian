package iciObjects;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.io.Csv;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import util.Util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class MakeCommonPOIinBuilding {
    public static void main(String[] args) throws IOException {

    }

    public static List<POI> checkIfPOIIsDoubled(List<POI> inpuiListPOI){

        return null;
    }

    public static SimpleFeatureBuilder getSFB() {
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName("batimentAppareillementPOI");
        sfTypeBuilder.add(Collec.getDefaultGeomName(), Polygon.class);
        sfTypeBuilder.add("buildingID", String.class);
        sfTypeBuilder.add("", String.class);
        sfTypeBuilder.add("nbPOISirene", String.class);
        sfTypeBuilder.add("nbPOI_BPE", String.class);
        sfTypeBuilder.add("nbPOI_OSM", String.class);
        sfTypeBuilder.add("typesWorkingPlace", String.class);
        sfTypeBuilder.add("typesPOISirene", String.class);
        sfTypeBuilder.add("typesPOI_BPE", String.class);
        sfTypeBuilder.add("typesPOI_OSM", String.class);
        sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(featureType);
    }

    public static void ComparePOIInBuilding(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection workingPlaceSFC,
                                            SimpleFeatureCollection poiSirereSFC, SimpleFeatureCollection poiBPESFC, SimpleFeatureCollection poiOSMSFC, File folderOut)
            throws IOException {
        HashMap<String, String[]> data = new HashMap<String, String[]>();
        String[] fLine = {"buildingID", "nbWorkingPlace", "nbPOISirene", "nbPOI_BPE", "nbPOI_OSM", "typesWorkingPlace", "typesPOISirene",
                "typesPOI_BPE", "typesPOI_OSM"};
        int moreBPE = 0;
        int moreSirene = 0;
        int moreOSM = 0;
        int count = 0;
        DefaultFeatureCollection export = new DefaultFeatureCollection();
        SimpleFeatureBuilder sfb = getSFB();

        try (SimpleFeatureIterator buildingIt = buildingSFC.features()) {
            while (buildingIt.hasNext()) {
                String[] line = new String[8];
                SimpleFeature building = buildingIt.next();
                System.out.println(count++ + " on " + buildingSFC.size());
                Geometry buildingBuffered = ((Geometry) building.getDefaultGeometry()).buffer(1);
                // Get the points in each buildings
                SimpleFeatureCollection wpInDaBuilding = Collec.selectIntersection(workingPlaceSFC, buildingBuffered);
                SimpleFeatureCollection poiSInDaBuilding = Collec.selectIntersection(poiSirereSFC, buildingBuffered);
                SimpleFeatureCollection poiBPEInDaBuilding = Collec.selectIntersection(poiBPESFC, buildingBuffered);
                SimpleFeatureCollection poiOSMInDaBuilding = Collec.selectIntersection(poiOSMSFC, buildingBuffered);
                // Count points
                line[0] = String.valueOf(wpInDaBuilding.size());
                line[1] = String.valueOf(poiSInDaBuilding.size());
                line[2] = String.valueOf(poiBPEInDaBuilding.size());
                line[3] = String.valueOf(poiOSMInDaBuilding.size());
                if (poiSInDaBuilding.size() > poiBPEInDaBuilding.size() && poiSInDaBuilding.size() > poiOSMInDaBuilding.size())
                    moreSirene++;
                else if (poiBPEInDaBuilding.size() > poiSInDaBuilding.size() && poiBPEInDaBuilding.size() > poiOSMInDaBuilding.size())
                    moreBPE++;
                else if (poiOSMInDaBuilding.size() > poiBPEInDaBuilding.size() && poiOSMInDaBuilding.size() > poiSInDaBuilding.size())
                    moreOSM++;
                // Count single point types
                String amenites = "";
                try (SimpleFeatureIterator wpInDaBuildingIt = wpInDaBuilding.features()) {
                    while (wpInDaBuildingIt.hasNext())
                        amenites = amenites + ((String) wpInDaBuildingIt.next().getAttribute("amenite")).replace(",", " -") + "--";
                } catch (Exception e) {
                    e.printStackTrace();
                }
                line[4] = amenites;
                amenites = "";
                try (SimpleFeatureIterator poiSInDaBuildingIt = poiSInDaBuilding.features()) {
                    while (poiSInDaBuildingIt.hasNext())
                        amenites = amenites + ((String) poiSInDaBuildingIt.next().getAttribute("amenite")).replace(",", " -") + "--";
                } catch (Exception e) {
                    e.printStackTrace();
                }
                line[5] = amenites;
                amenites = "";
                try (SimpleFeatureIterator poiBPEInDaBuildingIt = poiBPEInDaBuilding.features()) {
                    while (poiBPEInDaBuildingIt.hasNext())
                        amenites = amenites + ((String) poiBPEInDaBuildingIt.next().getAttribute("Type")).replace(",", " -") + "--";
                } catch (Exception e) {
                    e.printStackTrace();
                }
                line[6] = amenites;
                amenites = "";
                try (SimpleFeatureIterator poiOSMInDaBuildingIt = poiOSMInDaBuilding.features()) {
                    while (poiOSMInDaBuildingIt.hasNext())
                        amenites = amenites + ((String) poiOSMInDaBuildingIt.next().getAttribute("amenity")).replace(",", " -") + "--";
                } catch (Exception e) {
                    e.printStackTrace();
                }
                line[7] = amenites;
                data.put((String) building.getAttribute("ID"), line);

                sfb.set(Collec.getDefaultGeomName(), building.getDefaultGeometry());
                sfb.set("buildingID", building.getAttribute("ID"));
                sfb.set("nbWorkingPlace", line[0]);
                sfb.set("nbPOISirene", line[1]);
                sfb.set("nbPOI_BPE", line[2]);
                sfb.set("nbPOI_OSM", line[3]);
                sfb.set("typesWorkingPlace", line[4]);
                sfb.set("typesPOISirene", line[5]);
                sfb.set("typesPOI_BPE", line[6]);
                sfb.set("typesPOI_OSM", line[7]);
                export.add(sfb.buildFeature(Attribute.makeUniqueId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("More OSM :" + moreOSM);
        System.out.println("More Sirene :" + moreSirene);
        System.out.println("More BPE :" + moreBPE);
        Csv.generateCsvFile(data, folderOut, "statBuilding", false, fLine);
        Collec.exportSFC(export, new File(folderOut, "buildingPOIappareillement"));
    }
}
