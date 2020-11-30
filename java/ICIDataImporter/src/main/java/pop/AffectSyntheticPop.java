package pop;

import fr.ign.artiscales.tools.carto.CountPointInPolygon;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import util.Util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class AffectSyntheticPop {

    public static void main(String[] args) throws IOException {
        File rootFolder = Util.getRootFolder();
        File syntheticPopFile = new File(rootFolder, "pop/populationInBuildingsSampledVeme.gpkg");
        File buildingFile = new File(rootFolder, "IGN/batVeme.gpkg");
        DataStore bDS = Collec.getDataStore(buildingFile);
        DataStore spDS = Collec.getDataStore(syntheticPopFile);
        Collec.exportSFC(CountPointInPolygon.countPointInPolygon(spDS.getFeatureSource(spDS.getTypeNames()[0]).getFeatures(),
                bDS.getFeatureSource(bDS.getTypeNames()[0]).getFeatures(), true, null, Arrays.asList("ageCat", "sex", "education"),null
                ), new File(rootFolder, "ICI/BuildingSyntheticPop.gpkg"));
        bDS.dispose();
        spDS.dispose();
    }
}
