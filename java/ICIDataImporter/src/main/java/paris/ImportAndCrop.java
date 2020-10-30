package paris;

import java.io.File;
import java.io.IOException;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.GeoJSON;

public class ImportAndCrop {

	public static void main(String[] args) throws IOException {
		// FIXME that aint finishing
		// ProcessBuilder pb = new ProcessBuilder("../../script_VilleParis.sh");
		// Process p = pb.start();
		// BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		// String line = null;
		// while ((line = reader.readLine()) != null)
		// {
		// System.out.println(line);
		// }
		// FIXME lot of bugs (coming from geojson I guess)
		File folderOut = new File("/home/ubuntu/Documents/INRIA/donnees/paris/parisOut");
		File cropGeometryFile = new File("/home/ubuntu/Documents/INRIA/donnees/5eme.shp");
		DataStore cropDS = Collec.getDataStore(cropGeometryFile);
		SimpleFeatureCollection crop = cropDS.getFeatureSource(cropDS.getTypeNames()[0]).getFeatures();
		for (File f : new File("../../input_l93").listFiles()) {
			File subFoulderOut = new File(folderOut, f.getName());
			subFoulderOut.mkdir();
			for (File ff : f.listFiles()) {
				DataStore ds = GeoJSON.getGeoJSONDataStore(ff);
				Collec.exportSFC(Collec.selectIntersection(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures(), crop),
						new File(subFoulderOut, ff.getName()), ".gpkg", true);
				ds.dispose();
			}
		}
		cropDS.dispose();
	}

}