package fr.ici.dataImporter.iciObjects;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecTransform;
import fr.ign.artiscales.tools.io.Csv;
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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class MakeCommonPOIinBuilding {
//    public static void main(String[] args) throws IOException {
//
//    }

    public static SimpleFeatureBuilder getSFB() {
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName("batimentAppareillementPOI");
        sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Polygon.class);
        sfTypeBuilder.add("buildingID", String.class);
        sfTypeBuilder.add("", String.class);
        sfTypeBuilder.add("nbPOISirene", String.class);
        sfTypeBuilder.add("nbPOI_BPE", String.class);
        sfTypeBuilder.add("nbPOI_OSM", String.class);
        sfTypeBuilder.add("typesWorkingPlace", String.class);
        sfTypeBuilder.add("typesPOISirene", String.class);
        sfTypeBuilder.add("typesPOI_BPE", String.class);
        sfTypeBuilder.add("typesPOI_OSM", String.class);
        sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(featureType);
    }

    public static void ComparePOIInBuilding(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection workingPlaceSFC,
                                            SimpleFeatureCollection poiSirereSFC, SimpleFeatureCollection poiBPESFC, SimpleFeatureCollection poiOSMSFC, File folderOut)
            throws IOException {
        HashMap<String, String[]> data = new HashMap<>();
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
                SimpleFeatureCollection wpInDaBuilding = CollecTransform.selectIntersection(workingPlaceSFC, buildingBuffered);
                SimpleFeatureCollection poiSInDaBuilding = CollecTransform.selectIntersection(poiSirereSFC, buildingBuffered);
                SimpleFeatureCollection poiBPEInDaBuilding = CollecTransform.selectIntersection(poiBPESFC, buildingBuffered);
                SimpleFeatureCollection poiOSMInDaBuilding = CollecTransform.selectIntersection(poiOSMSFC, buildingBuffered);
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
                StringBuilder amenites = new StringBuilder();
                try (SimpleFeatureIterator wpInDaBuildingIt = wpInDaBuilding.features()) {
                    while (wpInDaBuildingIt.hasNext())
                        amenites.append(((String) wpInDaBuildingIt.next().getAttribute("amenite")).replace(",", " -")).append("--");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                line[4] = amenites.toString();
                amenites = new StringBuilder();
                try (SimpleFeatureIterator poiSInDaBuildingIt = poiSInDaBuilding.features()) {
                    while (poiSInDaBuildingIt.hasNext())
                        amenites.append(((String) poiSInDaBuildingIt.next().getAttribute("amenite")).replace(",", " -")).append("--");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                line[5] = amenites.toString();
                amenites = new StringBuilder();
                try (SimpleFeatureIterator poiBPEInDaBuildingIt = poiBPEInDaBuilding.features()) {
                    while (poiBPEInDaBuildingIt.hasNext())
                        amenites.append(((String) poiBPEInDaBuildingIt.next().getAttribute("Type")).replace(",", " -")).append("--");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                line[6] = amenites.toString();
                amenites = new StringBuilder();
                try (SimpleFeatureIterator poiOSMInDaBuildingIt = poiOSMInDaBuilding.features()) {
                    while (poiOSMInDaBuildingIt.hasNext())
                        amenites.append(((String) poiOSMInDaBuildingIt.next().getAttribute("amenity")).replace(",", " -")).append("--");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                line[7] = amenites.toString();
                data.put((String) building.getAttribute("ID"), line);

                sfb.set(CollecMgmt.getDefaultGeomName(), building.getDefaultGeometry());
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
        CollecMgmt.exportSFC(export, new File(folderOut, "buildingPOIappareillement"));
    }
}
