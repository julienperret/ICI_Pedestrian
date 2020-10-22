package osm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.geojson.GeoJSONDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.util.URLs;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

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
		Collec.setDefaultGISFileType(".geojson");
		makeTabWithAttributesAndValues(new File("/home/ubuntu/workspace/ICI_Pedestrian/osm/amenites.geojson"),
				new File("/home/ubuntu/workspace/ICI_Pedestrian/osm/"));
	}

	public static void makeTabWithAttributesAndValues(File geojsonFile, File folderOut) throws IOException {
		Map<String, Object> params = new HashMap<>();
		params.put(GeoJSONDataStoreFactory.URL_PARAM.key, URLs.fileToUrl(geojsonFile));
		DataStore ds = DataStoreFinder.getDataStore(params);
		SimpleFeatureCollection sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
		HashMap<String, Object[]> data = new HashMap<String, Object[]>();
		List<String> listAttr = new ArrayList<String>();
		try (SimpleFeatureIterator it = sfc.features()) {
			while (it.hasNext()) {
				SimpleFeature f = it.next();
				for (AttributeDescriptor attr : f.getFeatureType().getAttributeDescriptors()) {
					String sAttr = attr.getLocalName();
					if (sAttr.equals(Collec.getDefaultGeomName()) || sAttr.equals("id"))
						continue;
					if (!listAttr.contains(sAttr))
						listAttr.add(sAttr);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println(listAttr);

		for (String attr : listAttr) {
			List<String> list = Collec.getEachUniqueFieldFromSFC(sfc, attr, true);
			if (list != null && list.size() > 0)
				data.put(attr, list.toArray(String[]::new));
		}
		Csv.generateCsvFileCol(data, folderOut, geojsonFile.getName() + "-attr");
	}
}
