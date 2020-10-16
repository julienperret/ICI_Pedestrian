package osm;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.geojson.GeoJSONDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.util.URLs;

import fr.ign.artiscales.tools.geoToolsFunctions.Csv;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;

public class ImportShops {
	public static void main(String[] args) throws IOException {
		// // FIXME doesn't work (do we need that api of send straight HTTP requests?)
		// OverpassStatus result = new OverpassStatus();
		// OsmConnection connection = new OsmConnection("https://overpass-api.de/api/", "ICI");
		// OverpassMapDataDao overpass = new OverpassMapDataDao(connection);
		// MapDataDao map = new MapDataDao(connection);
		// System.out.println(overpass.queryCount("[out:csv(name)];\n" + "node[amenity](bbox:2.3367,88.8375,2.3674,88.8553);\n"
		// + "for (t[\"amenity\"])\n" + "{\n" + " make ex name=_.val;\n" + " out;\n" + "}"));
		makeTabWithAttributesAndValues(new File("/home/ubuntu/workspace/ICI_Pedestrian/osm/amenites.geojson"),
				new File("/home/ubuntu/workspace/ICI_Pedestrian/osm/"));
	}

	public static void makeTabWithAttributesAndValues(File geojsonFile, File folderOut) throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.put(GeoJSONDataStoreFactory.URL_PARAM.key, URLs.fileToUrl(geojsonFile));
		DataStore ds = DataStoreFinder.getDataStore(params);
		SimpleFeatureCollection sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
		HashMap<String, String[]> data = new HashMap<String, String[]>();
		for (String attr : sfc.getSchema().getAttributeDescriptors().stream().map(x -> x.getLocalName()).toArray(String[]::new)) {
			String[] l = Collec.getEachUniqueFieldFromSFC(sfc, attr).toArray(String[]::new);
			data.put(attr, l);
		}
		Csv.generateCsvFile(data, folderOut, geojsonFile.getName() + "-attr", false, null);
	}
}
