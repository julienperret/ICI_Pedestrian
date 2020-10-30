package osm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.GeoJSON;
import fr.ign.artiscales.tools.io.Csv;

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

	public static void sortPOI(File geojsonFile, File folderOut) throws IOException {
		
	}
		
	
	
	/**
	 * Create a table summing up the geojson file coming from OSM with heterogenous attributes and values for those attributes. On columns, every attributes of the features, on
	 * line, every unique value for each attribute.
	 * 
	 * @param geojsonFile
	 *            input file with geojson
	 * @param folderOut
	 *            folder where the \{$geojson.name()\}-attr.csv file will be created
	 * @throws IOException
	 */
	public static void makeTabWithAttributesAndValues(File geojsonFile, File folderOut) throws IOException {
		// information for i/o of geocollection
		Collec.setDefaultGISFileType(".geojson");
		// importing geojson
		DataStore ds = GeoJSON.getGeoJSONDataStore(geojsonFile);;
		SimpleFeatureCollection sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
		HashMap<String, Object[]> table = new HashMap<String, Object[]>();
		List<String> listAttr = new ArrayList<String>();
		// for every features
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
		for (String attr : listAttr) {
			List<String> list = Collec.getEachUniqueFieldFromSFC(sfc, attr, true);
			if (list != null && list.size() > 0)
				table.put(attr, list.toArray(String[]::new));
		}
		Csv.generateCsvFileCol(table, folderOut, geojsonFile.getName() + "-attr");
	}
}
