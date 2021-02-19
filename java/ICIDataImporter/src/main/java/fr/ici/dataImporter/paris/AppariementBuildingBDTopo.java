package fr.ici.dataImporter.paris;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.OpOnCollec;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AppariementBuildingBDTopo {
//    public static void main(String[] args) throws IOException {
//        apparieBuildingBDTopo(new File("/home/mc/Nextcloud/boulot/inria/ICIproject/donnees/IGN/batVeme.gpkg"), new File("/tmp/bParis.gpkg"));
//
////        apparieBuildingBDTopo(new File("/tmp/bBDT.shp"),new File("/tmp/bParis.gpkg"));
//
////        new File(Util.getRootFolder(),"IGN/batVeme.gpkg")
////        new File(Util.getRootFolder(),"paris/batVeme.geojson")
//    }

    public static void apparieBuildingBDTopo(File bBDTOpoF, File bParisF) throws IOException {
        DataStore dsBuildingBDTopo = CollecMgmt.getDataStore(bBDTOpoF);
        DataStore dsBuildingParis = CollecMgmt.getDataStore(bParisF);
        SimpleFeatureCollection buildingParisSFC = dsBuildingParis.getFeatureSource(dsBuildingParis.getTypeNames()[0]).getFeatures();
        SimpleFeatureCollection buildingBDTopoSFC = dsBuildingBDTopo.getFeatureSource(dsBuildingBDTopo.getTypeNames()[0]).getFeatures();

        SimpleFeatureBuilder sfbParis = Schemas.addIntColToSFB(buildingParisSFC, "NB_LOGTS");
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        List<String> proceedList = new ArrayList<>();
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        try (SimpleFeatureIterator itBParis = buildingParisSFC.features()) {
            while (itBParis.hasNext()) {
                SimpleFeature bParis = itBParis.next();
                // if no high, we skip
                if (bParis.getAttribute("h_et_max") != null && bParis.getAttribute("h_et_max").equals(0))
                    continue;
                // if building has already been proceeded, we skip
                if (!proceedList.isEmpty() && proceedList.contains(bParis.getAttribute("objectid")))
                    continue;
                // case Paris building is contained by BDTopo building (most common case)
                SimpleFeatureCollection bBDTopoContains = DataUtilities.collection(buildingBDTopoSFC.subCollection(
                        ff.contains(ff.property(buildingBDTopoSFC.getSchema().getGeometryDescriptor().getLocalName()),
                                ff.literal(((Geometry) bParis.getDefaultGeometry()).buffer(-1)))));
                if (bBDTopoContains.size() >= 1) {
                    SimpleFeatureCollection bParisWithin = buildingParisSFC.subCollection(
                            ff.within(ff.property(buildingParisSFC.getSchema().getGeometryDescriptor().getLocalName()),
                                    ff.literal(Geom.unionSFC(bBDTopoContains).buffer(3)))); // buffer is high coz some part of Paris's building polygon are erased with negative buffer and we need to get them
                    //deal with irregular division
                    int nbLogts = (int) OpOnCollec.getCollectionAttributeDescriptiveStat(bBDTopoContains, "NB_LOGTS", StatisticOperation.SUM);
                    int nbDiv = (nbLogts / bParisWithin.size());
                    int rest = nbLogts % bParisWithin.size();
                    boolean gaveRest = false;
                    //set households from BDTopo to BParis
                    try (SimpleFeatureIterator itBParisWithin = bParisWithin.features()) {
                        while (itBParisWithin.hasNext()) {
                            sfbParis = Schemas.setFieldsToSFB(sfbParis, itBParisWithin.next());
                            if (!gaveRest) {
                                sfbParis.set("NB_LOGTS", nbDiv + rest);
                                gaveRest = true;
                            } else
                                sfbParis.set("NB_LOGTS", nbDiv);
                            result.add(sfbParis.buildFeature(Attribute.makeUniqueId()));
                        }
                    } catch (Exception inter) {
                        inter.printStackTrace();
                    }
                    proceedList.addAll(CollecMgmt.getEachUniqueFieldFromSFC(bParisWithin, "objectid"));
                    continue;
                }
                // Paris buildings recovers BDTopo (rare)
                SimpleFeatureCollection bBDTopoWithin = DataUtilities.collection(buildingBDTopoSFC.subCollection(
                        ff.within(ff.property(buildingBDTopoSFC.getSchema().getGeometryDescriptor().getLocalName()),
                                ff.literal(((Geometry) bParis.getDefaultGeometry()).buffer(1)))));
                if (bBDTopoWithin.size() > 1) {
                    System.out.println("Paris buildings inside BDTopo (rare). ID : " + bParis.getAttribute("objectid")); //todo this case
                    continue;
                }

//                    // case building Paris intersects BDTopo buildings
                SimpleFeatureCollection bBDTopoInter = buildingBDTopoSFC.subCollection(
                        ff.intersects(ff.property(buildingBDTopoSFC.getSchema().getGeometryDescriptor().getLocalName()),
                                ff.literal(((Geometry) bParis.getDefaultGeometry()).buffer(1))));
                if (bBDTopoInter.size() >= 1) {
                    System.out.println("intersection : " + bBDTopoInter.size() + " of BDTopo building - ");
                }
//                    sfbParis = Schemas.setFieldsToSFB(sfbParis, bParis);
//                    sfbParis.set("NB_LOGT", OpOnCollec.getCollectionAttributeDescriptiveStat(bBDTopoInter, "NB_LOGTS", StatisticOperation.SUM));
//                    result.add(sfbParis.buildFeature(Attribute.makeUniqueId()));
//                    proceedList.addAll(CollecMgmt.getEachUniqueFieldFromSFC(bParisWithin,"objectid"));
//                    continue;
//                }
                // case building BDTopo is surrounding
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CollecMgmt.exportSFC(result, new File("/tmp/bat.gpkg"));
        dsBuildingBDTopo.dispose();
        dsBuildingParis.dispose();
    }
}
