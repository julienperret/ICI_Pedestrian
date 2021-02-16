package fr.ici.dataImporter.osm;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class ImportOSM {
/*
	public static void main(String[] args) throws IOException {
		// FIXME doesn't work (do we need that api of send straight HTTP requests?)
 // TODO look this project to get better categories https://framagit.org/PanierAvide/GeoDataMine/-/blob/master/themes/shop_craft_office.sql
		// OverpassStatus result = new OverpassStatus();
		// OsmConnection connection = new OsmConnection("https://overpass-api.de/api/", "ICI");
		// OverpassMapDataDao overpass = new OverpassMapDataDao(connection);
		// MapDataDao map = new MapDataDao(connection);
		// System.out.println(overpass.queryCount("[out:csv(name)];\n" + "node[amenity](bbox:2.3367,88.8375,2.3674,88.8553);\n"
		// + "for (t[\"amenity\"])\n" + "{\n" + " make ex name=_.val;\n" + " out;\n" + "}"));
		File rootFolder = Util.getRootFolder();
		importCyclePark(new File(rootFolder, "OSM/OSMamenities.gpkg"), new File(rootFolder, "OSM/"));
	}
*/

    public static void main(String[] args) throws IOException, URISyntaxException {

    }

    /**
     * Not working. Use script scriptOSM.sh from ici_pedestrian root project
     * @deprecated
     * @param outFolder
     * @throws IOException
     * @throws URISyntaxException
     */
    public static void getOSMData(File outFolder) throws IOException, URISyntaxException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        List<String> osmPOI = Arrays.asList("bank","cemetery","cinema","drinking_water","education","fuel",
                "healthcare","historic","hosting","library","playground","public_service","restaurant","shop_craft_office"
                ,"sports","toilets");
        List<String> osmTransport = Arrays.asList("bicycle-parking","carpool","charging_station","cycleway","parking");
        for (String theme : osmPOI) {

            URI uri = new URIBuilder().setScheme("https").setHost("geodatamine.fr")
                    .setPath("/data/"+theme+"/-20873")
                    .setParameter("format", "geojson").build();
            HttpPost httppost = new HttpPost(uri);
            try (CloseableHttpResponse response = httpclient.execute(httppost)) {
                // System.out.println(response.getStatusLine().getStatusCode());
                InputStream stream = response.getEntity().getContent();
                java.nio.file.Files.copy(stream, new File(outFolder,theme+".geojson").toPath(), StandardCopyOption.REPLACE_EXISTING);
                stream.close();
            }
        }
    }
    public static SimpleFeatureBuilder getCycleParkSFB() {
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName("bicycleParkOSM");
        sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Point.class);
        sfTypeBuilder.add("type", String.class);
        sfTypeBuilder.add("capacity", String.class);
        sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(featureType);
    }

    /**
     * Select cycle parks from a overpass OSM amenity dump
     * @param geoFile File containing OSM dump
     * @param folderOut Folder to export bike related informations (in <i>bicyclePark.gpkg</i> file)
     * @throws IOException
     */
    public static void importCyclePark(File geoFile, File folderOut) throws IOException {
        // information for i/o of geocollection
        CollecMgmt.setDefaultGISFileType(".geojson");
        // importing geojson
        DefaultFeatureCollection cyclePark = new DefaultFeatureCollection();
        DataStore ds = CollecMgmt.getDataStore(geoFile);
        SimpleFeatureCollection sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
        SimpleFeatureBuilder sfbBicyclePark = getCycleParkSFB();
        try (SimpleFeatureIterator it = sfc.features()) {
            while (it.hasNext()) {
                SimpleFeature feat = it.next();
                if ((feat.getAttribute("amenity") != null && feat.getAttribute("amenity").equals("bicycle_parking"))
                        || (feat.getAttribute("bicycle") != null && feat.getAttribute("bicycle").equals("yes"))
                        || (feat.getAttribute("official_amenity") != null
                        && feat.getAttribute("official_amenity").equals("bicycle_parking"))
                        || feat.getAttribute("bicycle_parking") != null) {
                    sfbBicyclePark.set("type", "cyclePark");
                    sfbBicyclePark.set("capacity", feat.getAttribute("capacity"));
                    sfbBicyclePark.set(CollecMgmt.getDefaultGeomName(), feat.getDefaultGeometry());
                    cyclePark.add(sfbBicyclePark.buildFeature(Attribute.makeUniqueId()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CollecMgmt.exportSFC(cyclePark, new File(folderOut, "bicyclePark.gpkg"));
    }
/*    public static void sortPOI(File geojsonFile, File folderOut) {

    }*/
}
