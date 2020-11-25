package pop;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.artiscales.tools.carto.CountPointInPolygon;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import util.Util;

public class AffectSyntheticPop {

	public static void main(String[] args) throws IOException {
		File rootFolder = Util.getRootFolder();
		File syntheticPopFile = new File(rootFolder, "pop/populationInBuildingsSampledVeme.gpkg");
		File buildingFile = new File(rootFolder, "IGN/batVeme.gpkg");
		DataStore bDS = Collec.getDataStore(buildingFile);
		DataStore spDS = Collec.getDataStore(syntheticPopFile);
		// SimpleFeatureCollection ex = CountPointInPolygon.countPointInPolygon(spDS.getFeatureSource(spDS.getTypeNames()[0]).getFeatures(),
		// bDS.getFeatureSource(bDS.getTypeNames()[0]).getFeatures());
		SimpleFeatureCollection ex = CountPointInPolygon.countPointInPolygon(spDS.getFeatureSource(spDS.getTypeNames()[0]).getFeatures(),
				bDS.getFeatureSource(bDS.getTypeNames()[0]).getFeatures(), true, Arrays.asList("ageCat", "sex", "education"),
				Arrays.asList(StatisticOperation.SUM, StatisticOperation.MEAN, StatisticOperation.MEDIAN, StatisticOperation.STANDEV));
		try (SimpleFeatureIterator polyIt = ex.features()) {
			while (polyIt.hasNext()) {
				SimpleFeature df = polyIt.next();
				df.validate();
				if (((Geometry) df.getDefaultGeometry()).isEmpty()) {
					System.out.println("shame");
				}
			}
		}

		Collec.exportSFC(ex, new File(rootFolder, "ICI/BuildingSyntheticPop.shp"));
		bDS.dispose();
		spDS.dispose();
	}
}
