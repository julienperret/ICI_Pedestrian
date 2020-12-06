package pop;

import fr.ign.artiscales.tools.carto.CountPointInPolygon;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import org.geotools.data.DataStore;
import util.Util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class AffectSyntheticPop {

    public static void main(String[] args) throws IOException {
        File rootFolder = Util.getRootFolder();
        File syntheticPopFile = new File("/home/mc/workspace/h24/results/populationInBuildings2.gpkg");
        File buildingFile = new File(rootFolder, "IGN/batVeme.gpkg");
        DataStore spDS = Collec.getDataStore(syntheticPopFile);
        DataStore bDS = Collec.getDataStore(buildingFile);
        Collec.exportSFC(CountPointInPolygon.countPointInPolygon(Collec.selectIntersection(spDS.getFeatureSource(spDS.getTypeNames()[0]).getFeatures(),bDS.getFeatureSource(bDS.getTypeNames()[0]).getFeatures()),
                bDS.getFeatureSource(bDS.getTypeNames()[0]).getFeatures(), true, null, Arrays.asList("age", "sex", "education"),null
                ), new File(rootFolder, "ICI/BuildingSyntheticPop.gpkg"));
        bDS.dispose();
        spDS.dispose();
    }
}
