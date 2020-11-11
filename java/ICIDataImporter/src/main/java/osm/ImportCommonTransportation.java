package osm;

import java.io.File;
import java.io.IOException;

import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.GeoJSON;
import transport.BusStation;
import util.Util;

public class ImportCommonTransportation {
	public static void main(String[] args) throws IOException {
		new ImportCommonTransportation(new File("../../osm/voirie.geojson"), new File(Util.getRootFolder(), "./OSM/"));
	}

	public ImportCommonTransportation(File geojsonFile, File folderOut) throws IOException {

		// information for i/o of geocollection
		Collec.setDefaultGISFileType(".geojson");
		// importing geojson
		DefaultFeatureCollection commonTransport = new DefaultFeatureCollection();
		DataStore ds = GeoJSON.getGeoJSONDataStore(geojsonFile);
		SimpleFeatureCollection sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
		SimpleFeatureBuilder sfbBusStop = BusStation.getBusStationSFB();
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
					String ref;
					try {
						ref = ((String) feat.getAttribute("ref:FR:STIF:stop_id"))
								.split(":")[((String) feat.getAttribute("ref:FR:STIF:stop_id")).split(":").length - 1];
					} catch (NullPointerException np) {
						ref = "";
					}
					sfbBusStop.set("IdSTIF", ref);
					sfbBusStop.set(Collec.getDefaultGeomName(), JTS.transform((Geometry) feat.getDefaultGeometry(),
							CRS.findMathTransform(CRS.decode("EPSG:4326", true), CRS.decode("EPSG:2154", true))));
					commonTransport.add(sfbBusStop.buildFeature(Attribute.makeUniqueId()));
				}
				String railway = (String) feat.getAttribute("railway");
				if (railway != null && !railway.equals("") && !railway.equals("abandoned")) {
					sfbBusStop.set("type", railway);
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
