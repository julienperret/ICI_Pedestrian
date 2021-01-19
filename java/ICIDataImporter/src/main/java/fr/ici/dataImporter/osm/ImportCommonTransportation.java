package fr.ici.dataImporter.osm;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.GeoJSON;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import fr.ici.dataImporter.transport.BusStation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ImportCommonTransportation {
//	public static void main(String[] args) throws IOException {
//		new ImportCommonTransportation(new File("../../fr.ici.dataImporter.osm/voirie.geojson"), new File(Util.getRootFolder(), "./OSM/"),
//				new File(Util.getRootFolder(), "5eme.shp"));
//	}

	public ImportCommonTransportation(File geojsonFile, File folderOut, File empriseFile) throws IOException {

		// information for i/o of geocollection
		Collec.setDefaultGISFileType(".geojson");
		// importing geojson
		DefaultFeatureCollection commonTransport = new DefaultFeatureCollection();
		DataStore ds = GeoJSON.getGeoJSONDataStore(geojsonFile);
		SimpleFeatureCollection sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
		SimpleFeatureBuilder sfbTC = BusStation.getBusStationSFB();
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		try (SimpleFeatureIterator it = sfc.features()) {
			while (it.hasNext()) {
				SimpleFeature feat = it.next();
				String highway = (String) feat.getAttribute("highway");
				if (highway != null && highway.equals("bus_stop")) {
					sfbTC.set("type", highway);
					sfbTC.set("name", feat.getAttribute("name"));
					sfbTC.set("pub_trans", feat.getAttribute("public_transport"));
					sfbTC.set("name", feat.getAttribute("name"));
					sfbTC.set("route_ref", feat.getAttribute("route_ref"));
					sfbTC.set("shelter", feat.getAttribute("shelter"));
					sfbTC.set("wheelchair", feat.getAttribute("wheelchair"));
					String ref;
					try {
						ref = ((String) feat.getAttribute("ref:FR:STIF:stop_id"))
								.split(":")[((String) feat.getAttribute("ref:FR:STIF:stop_id")).split(":").length - 1];
					} catch (NullPointerException np) {
						ref = "";
					}
					sfbTC.set("IdSTIF", ref);
					sfbTC.set(Collec.getDefaultGeomName(), JTS.transform((Geometry) feat.getDefaultGeometry(),
							CRS.findMathTransform(CRS.decode("EPSG:4326", true), CRS.decode("EPSG:2154", true))));
					commonTransport.add(sfbTC.buildFeature(Attribute.makeUniqueId()));
				}
				String railway = (String) feat.getAttribute("railway");
				if (railway != null && !railway.equals("") && !railway.equals("abandoned")) {
					sfbTC.set("type", railway);
					sfbTC.set("name", feat.getAttribute("name"));
					sfbTC.set("pub_trans", feat.getAttribute("public_transport"));
					sfbTC.set("name", feat.getAttribute("name"));
					sfbTC.set("route_ref", feat.getAttribute("route_ref"));
					sfbTC.set("shelter", feat.getAttribute("shelter"));
					sfbTC.set("wheelchair", feat.getAttribute("wheelchair"));
					sfbTC.set(Collec.getDefaultGeomName(), JTS.transform((Geometry) feat.getDefaultGeometry(),
							CRS.findMathTransform(CRS.decode("EPSG:4326", true), CRS.decode("EPSG:2154", true))));
					commonTransport.add(sfbTC.buildFeature(Attribute.makeUniqueId()));
				}
				String building = (String) feat.getAttribute("building");
				if (building != null && !building.equals("") && building.equals("train_station")) {
					// get entrance points
					SimpleFeatureCollection entrances = Collec.selectIntersection(sfc, (Geometry) feat.getDefaultGeometry())
							.subCollection(ff.or(Arrays.asList(ff.like(ff.property("entrance"), "main"), ff.like(ff.property("entrance"), "yes"),
									ff.like(ff.property("entrance"), "exit"))));
					System.out.println(entrances.size());
					try (SimpleFeatureIterator itEntrances = entrances.features()) {
						while (itEntrances.hasNext()) {
							SimpleFeature f = itEntrances.next();
							sfbTC.set("type", "train_station_entrance");
							sfbTC.set("name", feat.getAttribute("name"));
							sfbTC.set("pub_trans", feat.getAttribute("public_transport"));
							sfbTC.set("name", feat.getAttribute("name"));
							sfbTC.set("route_ref", feat.getAttribute("route_ref"));
							sfbTC.set("shelter", feat.getAttribute("shelter"));
							sfbTC.set("wheelchair", feat.getAttribute("wheelchair"));
							sfbTC.set(Collec.getDefaultGeomName(), JTS.transform((Geometry) f.getDefaultGeometry(),
									CRS.findMathTransform(CRS.decode("EPSG:4326", true), CRS.decode("EPSG:2154", true))));
							commonTransport.add(sfbTC.buildFeature(Attribute.makeUniqueId()));
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Collec.exportSFC(Collec.selectIntersection(commonTransport, empriseFile), new File(folderOut, "fr.ici.dataImporter.transport.gpkg"));
	}
}
