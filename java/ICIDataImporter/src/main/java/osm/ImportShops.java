package osm;

import java.io.File;
import java.io.IOException;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import util.Util;

public class ImportShops {
	public static void main(String[] args) throws IOException {
		// // FIXME doesn't work (do we need that api of send straight HTTP requests?)
		// OverpassStatus result = new OverpassStatus();
		// OsmConnection connection = new OsmConnection("https://overpass-api.de/api/", "ICI");
		// OverpassMapDataDao overpass = new OverpassMapDataDao(connection);
		// MapDataDao map = new MapDataDao(connection);
		// System.out.println(overpass.queryCount("[out:csv(name)];\n" + "node[amenity](bbox:2.3367,88.8375,2.3674,88.8553);\n"
		// + "for (t[\"amenity\"])\n" + "{\n" + " make ex name=_.val;\n" + " out;\n" + "}"));
		File rootFolder = Util.getRootFolder();
		importCyclePark(new File(rootFolder, "OSM/OSMamenities.gpkg"), new File(rootFolder, "OSM/"));
	}

	public static SimpleFeatureBuilder getCycleParkSFB() {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		try {
			sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sfTypeBuilder.setName("bicycleParkOSM");
		sfTypeBuilder.add(Collec.getDefaultGeomName(), Point.class);
		sfTypeBuilder.add("type", String.class);
		sfTypeBuilder.add("capacity", String.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}

	public static void importCyclePark(File geoFile, File folderOut) throws IOException {
		// information for i/o of geocollection
		Collec.setDefaultGISFileType(".geojson");
		// importing geojson
		DefaultFeatureCollection cyclePark = new DefaultFeatureCollection();
		DataStore ds = Collec.getDataStore(geoFile);
		SimpleFeatureCollection sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
		SimpleFeatureBuilder sfbBicyclePark = getCycleParkSFB();
		try (SimpleFeatureIterator it = sfc.features()) {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				if ((((String) feat.getAttribute("amenity")) != null && ((String) feat.getAttribute("amenity")).equals("bicycle_parking"))
						|| (((String) feat.getAttribute("bicycle")) != null && ((String) feat.getAttribute("bicycle")).equals("yes"))
						|| (((String) feat.getAttribute("official_amenity")) != null
								&& ((String) feat.getAttribute("official_amenity")).equals("bicycle_parking"))
						|| ((String) feat.getAttribute("bicycle_parking")) != null) {
					sfbBicyclePark.set("type", "cyclePark");
					sfbBicyclePark.set("capacity", feat.getAttribute("capacity"));
					sfbBicyclePark.set(Collec.getDefaultGeomName(), (Geometry) feat.getDefaultGeometry());
					cyclePark.add(sfbBicyclePark.buildFeature(Attribute.makeUniqueId()));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Collec.exportSFC(cyclePark, new File(folderOut, "bicyclePark.gpkg"));
	}

	public static void sortPOI(File geojsonFile, File folderOut) throws IOException {

	}

}
