package fr.ici.dataImporter.paris;

import fr.ici.dataImporter.util.Util;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.OpOnCollec;
import org.geotools.data.DataStore;
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
    public static void main(String[] args) throws IOException {

        DataStore d = CollecMgmt.getDataStore(new File("/home/mc/Nextcloud/boulot/inria/ICIproject/donnees/paris/batVeme.geojson"));
        CollecMgmt.exportSFC(d.getFeatureSource(d.getTypeNames()[0]).getFeatures(), new File("/tmp/bBDT.gpkg"));
//        apparieBuildingBDTopo(new File("/tmp/bBDT.shp"),new File("/tmp/bParis.gpkg"));

//        new File(Util.getRootFolder(),"IGN/batVeme.gpkg")
//        new File(Util.getRootFolder(),"paris/batVeme.geojson")
    }

    public static void apparieBuildingBDTopo(File bBDTOpoF, File bParisF) throws IOException{
        DataStore dsBuildingBDTopo = CollecMgmt.getDataStore(bBDTOpoF);
        DataStore dsBuildingParis = CollecMgmt.getDataStore(bParisF);
        SimpleFeatureCollection buildingParisSFC = dsBuildingParis.getFeatureSource(dsBuildingParis.getTypeNames()[0]).getFeatures();
        SimpleFeatureCollection buildingBDTopoSFC = dsBuildingBDTopo.getFeatureSource(dsBuildingBDTopo.getTypeNames()[0]).getFeatures();
        SimpleFeatureBuilder sfbParis = Schemas.addIntColToSFB(buildingParisSFC, "NB_LOGT");
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
                // Paris buildings inside BDTopo (rare)
                SimpleFeatureCollection bBDTopoContains = buildingBDTopoSFC.subCollection(
                        ff.contains(ff.property(buildingBDTopoSFC.getSchema().getGeometryDescriptor().getLocalName()),
                                ff.literal(((Geometry) bParis.getDefaultGeometry()).buffer(1))));
                if (bBDTopoContains.size() > 1)
                    System.out.println("Paris buildings inside BDTopo (rare) "); //todo this case

                SimpleFeatureCollection bBDTopoContained = buildingBDTopoSFC.subCollection(
                        ff.contains(ff.property(buildingBDTopoSFC.getSchema().getGeometryDescriptor().getLocalName()),
                                ff.literal(((Geometry) bParis.getDefaultGeometry()).buffer(1))));
                // case building Paris is surrounding BDTopo buildings
                if (bBDTopoContained.size() >= 1) {
                    sfbParis = Schemas.setFieldsToSFB(sfbParis, bParis);
                    sfbParis.set("NB_LOGT", OpOnCollec.getCollectionAttributeDescriptiveStat(bBDTopoContained, "NB_LOGTS", StatisticOperation.SUM));
                    result.add(sfbParis.buildFeature(Attribute.makeUniqueId()));
                    continue;
                }
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
