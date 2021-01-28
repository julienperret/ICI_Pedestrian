package fr.ici.dataImporter.pop;

import fr.ign.artiscales.tools.carto.CountPointInPolygon;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import org.geotools.data.DataStore;
import fr.ici.dataImporter.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class AffectSyntheticPop {

    public static void main(String[] args) throws IOException {
        File rootFolder = Util.getRootFolder();
        File syntheticPopFile = new File("/home/mc/workspace/h24/results/populationInBuildings2.gpkg");
        File buildingFile = new File(rootFolder, "IGN/batVeme.gpkg");
        File blockFile = new File(rootFolder, "fr/ici/dataImporter/paris/ilotVeme.gpkg");
        affectSyntheticPop("building",syntheticPopFile, buildingFile);
        affectSyntheticPop("building",syntheticPopFile, blockFile);
    }
    public static void affectSyntheticPop(String zoneName , File syntheticPopFile, File zoneGeoFile) throws IOException {

        DataStore spDS = Collec.getDataStore(syntheticPopFile);
        DataStore iDS = Collec.getDataStore(zoneGeoFile);

        Collec.exportSFC(CountPointInPolygon.countPointInPolygon(Collec.selectIntersection(spDS.getFeatureSource(spDS.getTypeNames()[0]).getFeatures(), iDS.getFeatureSource(iDS.getTypeNames()[0]).getFeatures()),"POP",
                iDS.getFeatureSource(iDS.getTypeNames()[0]).getFeatures(), true, null, Arrays.asList("age", "sex", "education"),null
        ), new File(Util.getRootFolder(), "ICI/"+zoneName+"SyntheticPop.gpkg"));
        spDS.dispose();
        iDS.dispose();
    }
}