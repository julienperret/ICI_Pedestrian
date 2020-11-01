package osm;

import java.io.File;
import java.io.IOException;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.GeoJSON;

public class ImportCommonTransportation {
	public static void main(String[] args) throws IOException {
		new ImportCommonTransportation(new File("../../osm/voirie.geojson"), new File("/tmp/"));
	}

	public static SimpleFeatureBuilder getBusStopSFB() {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		try {
			sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sfTypeBuilder.setName("BusStopOSM");
		sfTypeBuilder.add(Collec.getDefaultGeomName(), Point.class);
		sfTypeBuilder.add("type", String.class);
		sfTypeBuilder.add("name", String.class);
		sfTypeBuilder.add("pub_trans", String.class);
		sfTypeBuilder.add("route_ref", String.class);
		sfTypeBuilder.add("wheelchair", String.class);
		sfTypeBuilder.add("shelter", String.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}

	public ImportCommonTransportation(File geojsonFile, File folderOut) throws IOException {

		// information for i/o of geocollection
		Collec.setDefaultGISFileType(".geojson");
		// importing geojson
		DefaultFeatureCollection commonTransport = new DefaultFeatureCollection();
		DataStore ds = GeoJSON.getGeoJSONDataStore(geojsonFile);
		SimpleFeatureCollection sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
		SimpleFeatureBuilder sfbBusStop = getBusStopSFB();
		try (SimpleFeatureIterator it = sfc.features()) {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				String highway = (String) feat.getAttribute("highway");
				if (highway != null && highway.equals("bus_stop")) {
					sfbBusStop.set("type", highway);
					sfbBusStop.set("name", feat.getAttribute("name"));
					sfbBusStop.set("pub_trans", feat.getAttribute("public_transport"));
					sfbBusStop.set("name", feat.getAttribute("name"));
					sfbBusStop.set("route_ref", feat.getAttribute("route_ref"));
					sfbBusStop.set("shelter", feat.getAttribute("shelter"));
					sfbBusStop.set("wheelchair", feat.getAttribute("wheelchair"));
					sfbBusStop.set(Collec.getDefaultGeomName(), JTS.transform((Geometry) feat.getDefaultGeometry(),
							CRS.findMathTransform(CRS.decode("EPSG:4326", true), CRS.decode("EPSG:2154", true))));
					commonTransport.add(sfbBusStop.buildFeature(Attribute.makeUniqueId()));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Collec.exportSFC(commonTransport, new File(folderOut, "transport.gpkg"));
	}
}
