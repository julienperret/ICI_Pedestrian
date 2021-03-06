package fr.ici.dataImporter.transport;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.apache.commons.math3.exception.NullArgumentException;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import si.uom.SI;
import fr.ici.dataImporter.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CreatePublicTransportation {

    public static void main(String[] args) throws IOException {
        File rootFolder = Util.getRootFolder();
        // File rF = new File(rootFolder, "fr.ici.dataImporter.paris/fr.ici.dataImporter.transport/positions-geographiques-des-stations-du-reseau-ratp_Veme.gpkg");
        // File dailyProfiles = new File(rootFolder, "fr.ici.dataImporter.paris/mobilite/validations-sur-le-reseau-ferre-profils-horaires-par-jour-type-1er-sem.csv");
        // File dailyValidation = new File(rootFolder, "fr.ici.dataImporter.paris/mobilite/validations-sur-le-reseau-ferre-nombre-de-validations-par-jour-1er-sem.csv");
        // getMorningWeekdayAffluence(dailyProfiles, dailyValidation, "525");
        // getEveningWeekdayAffluence(dailyProfiles, dailyValidation, "525");
        // getBestAffluence(dailyProfiles, dailyValidation, Arrays.asList("525").toArray(String[]::new), "SAHV");
        // System.out.println(rF.getAbsoluteFile());
        // DataStore dsRATP = Collec.getDataStore(rF);
        // DataStore dsOSM = Collec.getDataStore(new File(Util.getRootFolder(), "OSM/fr.ici.dataImporter.transport.gpkg"));
        // SimpleFeatureCollection sfcRATP = dsRATP.getFeatureSource(dsRATP.getTypeNames()[0]).getFeatures();
        // SimpleFeatureCollection sfcOSM = dsOSM.getFeatureSource(dsOSM.getTypeNames()[0]).getFeatures();
        // new CreatePublicTransportation(sfcOSM, sfcRATP);
        File transportGeoFileOSM = new File(rootFolder, "OSM/fr.ici.dataImporter.transport.gpkg");
        File transportGeoFileSTIF = new File(rootFolder, "fr/ici/dataImporter/paris/mobilite/emplacement-des-gares-idf-Veme.gpkg");
        // File transportGeoFileSTIF = new File("/tmp/juss.gpkg");
        createSubwayStation(transportGeoFileOSM, transportGeoFileSTIF, new File(rootFolder, "transports"));
    }

    public static void createSubwayStation(File transportGeoFileOSM, File transportGeoFileSTIF, File outFolder) throws IOException {
        DataStore dsOSM = CollecMgmt.getDataStore(transportGeoFileOSM);
        DataStore dsSTIF = CollecMgmt.getDataStore(transportGeoFileSTIF);
        SimpleFeatureCollection sfcOSM = dsOSM.getFeatureSource(dsOSM.getTypeNames()[0]).getFeatures();
        SimpleFeatureCollection sfcSTIF = dsSTIF.getFeatureSource(dsSTIF.getTypeNames()[0]).getFeatures();
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 2154);
        List<SubwayStation> listSubwayStation = new ArrayList<>();
        try (SimpleFeatureIterator it = sfcSTIF.features()) {
            while (it.hasNext()) {
                SimpleFeature stif = it.next();
                SubwayStation.add(
                        gf.createMultiPoint(Arrays.stream(sfcOSM
                                .subCollection(ff.or(Arrays.asList(ff.like(ff.property("type"), "subway_entrance"),
                                        ff.like(ff.property("type"), "train_station_entrance"))))
                                .subCollection(ff.dwithin(ff.property(sfcOSM.getSchema().getGeometryDescriptor().getLocalName()),
                                        ff.literal(stif.getDefaultGeometry()), 150, SI.METRE.toString()))
                                .toArray(new SimpleFeature[0])).map(x -> (Point) x.getDefaultGeometry()).toArray(Point[]::new)),
                        new String[]{transformExploitantToCode(String.valueOf(stif.getAttribute("exploitant"))),
                                String.valueOf(stif.getAttribute("res_stif")).split("\\.")[0],
                                String.valueOf(stif.getAttribute("gares_id")).split("\\.")[0]},
                        (String) stif.getAttribute("nomlong"), (String) stif.getAttribute("res_com"), listSubwayStation);
            }
        }
        DefaultFeatureCollection featCollection = new DefaultFeatureCollection();
        for (SubwayStation subwayStation : listSubwayStation)
            featCollection.add(subwayStation.generateFeature());
        CollecMgmt.exportSFC(featCollection, new File(outFolder, "SubwayStation.gpkg"));
        dsOSM.dispose();
        dsSTIF.dispose();
    }

    public static String transformExploitantToCode(String exploitant) {
        switch (exploitant) {
            case "RATP":
                return "100";
            case "SNCF":
                return "800";
            case "Transdev":
                return "810";
        }
        throw new NullArgumentException();
    }
}
