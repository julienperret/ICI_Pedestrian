package fr.ici.dataImporter.iciObjects.exporter;

import fr.ici.dataImporter.iciObjects.Address;
import fr.ici.dataImporter.iciObjects.WorkingPlace;
import fr.ici.dataImporter.insee.SirenePOI;
import fr.ici.dataImporter.util.Util;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.rasters.Rasters;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.io.Csv;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.raster.BandMergeProcess;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConvertToRaster {

    private static final String[] demoColl = {"count", "age_00_02", "age_03_05", "age_06_10", "age-11 - 1", "age-15 - 1", "age_18_24",
            "age-25 - 2", "age-30 - 3", "age-40 - 4", "age_45_54", "age_55_59", "age-60 - 6", "age-65 - 7", "age-75 - 7",
            "age-80 - P", "sex_Femme", "sex-Homme"}; // missing education (but the field names are f* up)

    public static void main(String[] args) throws Exception {
        makeRasterWithCSV();
//        makeUniqueRaster();
    }

    /**
     * Associate a raster with a csv file containing attribute values
     */
    public static void makeRasterWithCSV() throws IOException, FactoryException {
        int cellResolution = 5;
        DataStore dsBBox = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "5eme.gpkg"));
        ReferencedEnvelope bb = dsBBox.getFeatureSource(dsBBox.getTypeNames()[0]).getFeatures().getBounds();
        DataStore dsAddress = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "IGN/outban_75_20201002Vemr.gpkg"));
        SimpleFeatureCollection address = dsAddress.getFeatureSource(dsAddress.getTypeNames()[0]).getFeatures();
        DataStore dsBuilding = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "ICI/BuildingSyntheticPop2.shp"));
        File outputFolder = new File("/tmp");

        //Prepare the coordinate raster
        DataStore dsPOI = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "ICI/POI.gpkg"));
        Rasters.writeGeotiff(Rasters.rasterize(CollecMgmt.convertAttributeToFloat(summarizePOI(SirenePOI.getRestaurant(dsPOI.getFeatureSource(dsPOI.getTypeNames()[0]).getFeatures()), address), "POINumber"), "POINumber", Rasters.getDimentionValuesForSquaredRasters(bb, cellResolution), bb, "POINumber"), new File(outputFolder, "resto.tif"));
        File rastID = Rasters.createRasterWithID(Rasters.getDimentionValuesForSquaredRasters(bb, cellResolution), cellResolution, new File("/tmp/resto.tif"), outputFolder);
        List<File> csvToMerge = new ArrayList<>();

        // Buildings with population
        Rasters.rasterize(Address.affectDemoStatToAddress(dsBuilding.getFeatureSource(dsBuilding.getTypeNames()[0]).getFeatures(), address, demoColl), demoColl, Rasters.getDimentionValuesForSquaredRasters(bb, cellResolution), bb, demoColl, true, outputFolder);
        for (String attr : demoColl)
            csvToMerge.add(Rasters.convertRasterAndPositionValuesToCsv(new File(outputFolder, attr + ".tif"), rastID, attr));
        System.out.println("Done buildings");

        // Restaurant
        String attrRestos = "POINumber";
        GridCoverage2D rasterResto = Rasters.rasterize(CollecMgmt.convertAttributeToFloat(summarizePOI(SirenePOI.getRestaurant(dsPOI.getFeatureSource(dsPOI.getTypeNames()[0]).getFeatures()), address), attrRestos), attrRestos, Rasters.getDimentionValuesForSquaredRasters(bb, cellResolution), bb, attrRestos);
        csvToMerge.add(Rasters.convertRasterAndPositionValuesToCsv(Rasters.writeGeotiff(rasterResto, new File(outputFolder, "resto.tif")), rastID, attrRestos));
        dsPOI.dispose();
        System.out.println("Done restaurant");

        // Working place
        String attrWF = "WorkforceEstimate";
        DataStore dsWP = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "POI/SIRENE-WorkingPlace.gpkg"));
        GridCoverage2D rasterWorkforce = Rasters.rasterize(CollecMgmt.convertAttributeToFloat(summarizeWP(dsWP.getFeatureSource(dsWP.getTypeNames()[0]).getFeatures(), address), "WorkforceEstimate"), attrWF, Rasters.getDimentionValuesForSquaredRasters(bb, cellResolution), bb, attrWF);
        csvToMerge.add(Rasters.convertRasterAndPositionValuesToCsv(Rasters.writeGeotiff(rasterWorkforce, new File(outputFolder, "wf.tif")), rastID, attrWF));
        dsPOI.dispose();
        System.out.println("done Working Place");

        Csv.mergeCsvFilesCol(csvToMerge, outputFolder, "attributes", true);

        dsBuilding.dispose();
        dsAddress.dispose();
        dsBuilding.dispose();
        dsBBox.dispose();
    }

    public static void makeUniqueRaster() throws IOException, FactoryException {
        DataStore dsBBox = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "5eme.gpkg"));
        ReferencedEnvelope bb = dsBBox.getFeatureSource(dsBBox.getTypeNames()[0]).getFeatures().getBounds();

        // Rasterizing buildings and households
        DataStore dsAddress = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "IGN/outban_75_20201002Vemr.gpkg"));
        SimpleFeatureCollection address = dsAddress.getFeatureSource(dsAddress.getTypeNames()[0]).getFeatures();
        DataStore dsBuilding = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "ICI/BuildingSyntheticPop2.shp"));
//        GridCoverage2D batRaster = Rasters.rasterize(affectDemoStatToAddress(dsBuilding.getFeatureSource(dsBuilding.getTypeNames()[0]).getFeatures(), address), demoColl, Rasters.getDimentionValuesForSquaredRasters(bb, 1), bb,demoColl, false,null);
        GridCoverage2D batRaster = Rasters.rasterize(Address.affectIndivToAddress(dsBuilding.getFeatureSource(dsBuilding.getTypeNames()[0]).getFeatures(), address), "NB_INDIV", Rasters.getDimentionValuesForSquaredRasters(bb, 1), bb, "NB_INDIV");
//        Rasters.writeGeotiff(batRaster, new File(Util.getRootFolder(), "rasters/households.geotiff"));
        System.out.println("Done rasterizing addresses");
        System.out.println();
        dsBuilding.dispose();

        // Rasterizing Working Place
        DataStore dsWP = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "POI/SIRENE-WorkingPlace.gpkg"));
        GridCoverage2D rasterWorkforce = Rasters.rasterize(CollecMgmt.convertAttributeToFloat(summarizeWP(dsWP.getFeatureSource(dsWP.getTypeNames()[0]).getFeatures(), address), "WorkforceEstimate"), "WorkforceEstimate", Rasters.getDimentionValuesForSquaredRasters(bb, 1), bb, "WorkforceEstimate");
//        Rasters.writeGeotiff(rasterWorkforce, new File(Util.getRootFolder(), "/rasters/workforce.geotiff"));
        System.out.println("Done rasterizing working places");
        dsWP.dispose();
        System.out.println();

        //Get the restaurants
        DataStore dsPOI = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "ICI/POI.gpkg"));
//        String[] attrRestos = {"POINumber", "POIimportance"};
        String attrRestos = "POINumber";
        GridCoverage2D rasterResto = Rasters.rasterize(CollecMgmt.convertAttributeToFloat(summarizePOI(SirenePOI.getRestaurant(dsPOI.getFeatureSource(dsPOI.getTypeNames()[0]).getFeatures()),
                address), attrRestos), attrRestos, Rasters.getDimentionValuesForSquaredRasters(bb, 1), bb, attrRestos);
        System.out.println("done rasterizing retaurants");
        System.out.println();
//        Rasters.writeGeotiff(rasterResto, new File(Util.getRootFolder(), "/rasters/resto.geotiff"));
        dsPOI.dispose();

        //get the pedestrian infos (from QGIS, failed to do it with GeoTools yet)
        GridCoverage2D ped = Rasters.importRaster(new File(Util.getRootFolder(), "rasters/cheminementPieton.tif"));

        // merge those rasters together
        BandMergeProcess bm = new BandMergeProcess();
        GridCoverage2D rastermerged = bm.execute(Arrays.asList(ped,
                rasterResto, rasterWorkforce, batRaster), null, null, null);
//                batRaster),null,null,null);

        Rasters.writeGeotiff(rastermerged, new File(Util.getRootFolder(), "rasters/out.geotiff"));
        dsAddress.dispose();
        System.out.println("done");
    }

//    public static GridCoverage2D generatePedestrianRepRaster() throws IOException {
//        todo not working, done on QGIS
//        DataStore dsBBox = Collec.getDataStore(new File(Util.getRootFolder(), "5eme.shp"));
//        ReferencedEnvelope bb = dsBBox.getFeatureSource(dsBBox.getTypeNames()[0]).getFeatures().getBounds();
//        int resWeight = 1000;
//        int resHeight = 1000;
//        DataStore dsRoad = Collec.getDataStore(new File(Util.getRootFolder(), "paris/outplan-de-voirie-chausseesVemr.gpkg"));
//        GridCoverage2D road = Rasters.rasterize(dsRoad.getFeatureSource(dsRoad.getTypeNames()[0]).getFeatures(),
//                new Dimension(resWeight,resHeight), bb);
//        Rasters.writeGeotiff(road,new File("/tmp/r"));
//
//        DataStore dsSidewalkOSM = Collec.getDataStore(new File(Util.getRootFolder(), "OSM/Vemevoirie_voirie_ea9979a8_eab4_46c2_8397_08e09ed84a8f.gpkg"));
//        GridCoverage2D sidewalkOSM = Rasters.rasterize(dsSidewalkOSM.getFeatureSource(dsSidewalkOSM.getTypeNames()[0]).getFeatures(),
//                new Dimension(resWeight,resHeight), bb)
//
//        DataStore dsSidewalk = Collec.getDataStore(new File(Util.getRootFolder(), "paris/outplan-de-voirie-trottoirs-emprisesVemr.gpkg"));
//        GridCoverage2D sidewalk = Rasters.rasterize(dsSidewalk.getFeatureSource(dsSidewalk.getTypeNames()[0]).getFeatures(),
//                new Dimension(resWeight,resHeight), bb);
//
//        //todo combine the two sidewalks grids
//
//
//        Rasters.writeGeotiff(,new File("/tmp/swOSM"));
//
//        //todo merge sidewalks
//
//        DataStore dsPedXing = Collec.getDataStore(new File(Util.getRootFolder(), ""));
//        GridCoverage2D pedXing = Rasters.rasterize(dsPedXing.getFeatureSource(dsPedXing.getTypeNames()[0]).getFeatures(),
//                new Dimension(resWeight,resHeight),bb);
//
////        DataStore dsBuilding = Collec.getDataStore(new File(Util.getRootFolder(), "IGN/batVeme.gpkg"));
////        SimpleFeatureCollection buildings = dsBuilding.getFeatureSource(dsBuilding.getTypeNames()[0]).getFeatures();
//
//        GridCoverage2D result = getPedestrianRepRaster(road, sidewalk, pedXing);
//        dsPedXing.dispose();
//        dsBBox.dispose();
//        dsRoad.dispose();
//        dsSidewalkOSM.dispose();
//        return result;
//        }
//
//        public static GridCoverage2D getPedestrianRepRaster(GridCoverage2D road, GridCoverage2D sidewalk, GridCoverage2D pedXing){
//
//     //       AddCoveragesProcess
//            Multiply mp = new Multiply();
////            mp.doOperation();
//        return null;
//    }

    public static SimpleFeatureCollection summarizeWP(SimpleFeatureCollection workingPlaceSFC, SimpleFeatureCollection addressesSFC) throws FactoryException {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        //builder
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        String geomName = workingPlaceSFC.getSchema().getGeometryDescriptor().getLocalName();
        sfTypeBuilder.add("WorkforceEstimate", Double.class);
        sfTypeBuilder.setName("SummarizedWorkingPlace");
        sfTypeBuilder.setCRS(CRS.decode("EPSG:2154", true));
        sfTypeBuilder.add(geomName, Point.class);
        sfTypeBuilder.setDefaultGeometry(geomName);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        Arrays.stream(addressesSFC.toArray(new SimpleFeature[0])).forEach(feat -> {
            Geometry addressGeom = (Geometry) feat.getDefaultGeometry();
            if (addressGeom != null || !addressGeom.isEmpty()) {
                SimpleFeatureCollection overlapping = workingPlaceSFC.subCollection(ff.dwithin(ff.property(geomName), ff.literal(addressGeom), 1, "m"));
                if (!overlapping.isEmpty()) {
                    builder.set(geomName, addressGeom);
                    builder.set("WorkforceEstimate", WorkingPlace.makeWorkforceAverage(overlapping));
                    result.add(builder.buildFeature(Attribute.makeUniqueId()));
                }
            }
        });
        return result;
    }

    public static SimpleFeatureCollection summarizePOI(SimpleFeatureCollection poiSFC, SimpleFeatureCollection addressesSFC) throws IOException {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        //builder
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        String geomName = poiSFC.getSchema().getGeometryDescriptor().getLocalName();
        sfTypeBuilder.add("POINumber", Double.class);
        sfTypeBuilder.add("POIimportance", Double.class);
        sfTypeBuilder.setName("SummarizedPOI");
        sfTypeBuilder.setCRS(poiSFC.getSchema().getCoordinateReferenceSystem());
        sfTypeBuilder.add(geomName, Point.class);
        sfTypeBuilder.setDefaultGeometry(geomName);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        try (SimpleFeatureIterator adIt = addressesSFC.features()) {
            while (adIt.hasNext()) {
                Geometry addressGeom = (Geometry) adIt.next().getDefaultGeometry();
                if (addressGeom != null && !addressGeom.isEmpty()) {
                    SimpleFeatureCollection overlapping = poiSFC.subCollection(ff.dwithin(ff.property(geomName), ff.literal(addressGeom), 1, "m"));
                    if (overlapping != null && !overlapping.isEmpty()) {
                        builder.set(geomName, addressGeom);
                        builder.set("POINumber", overlapping.size());
                        builder.set("POIimportance", SirenePOI.maxAffluenceIndicator(overlapping));
                        result.add(builder.buildFeature(Attribute.makeUniqueId()));
                    }
                }
            }
        }
        return result.collection();
    }
}