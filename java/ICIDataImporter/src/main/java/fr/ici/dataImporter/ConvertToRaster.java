package fr.ici.dataImporter;

import fr.ici.dataImporter.insee.SirenePOI;
import fr.ici.dataImporter.insee.SireneWorkingPlace;
import fr.ici.dataImporter.util.Util;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.rasters.Rasters;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.process.raster.BandMergeProcess;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.GeoTools;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ConvertToRaster {

    public static void main(String[] args) throws Exception {
//        makeRasterWithCSV();
        makeUniqueRaster();
    }

    /**
     * Associate a raster with a csv file containing attribute values
     * todo to finish if needed
     *
     * @throws IOException
     */
    public static void makeRasterWithCSV() throws IOException {
        int width = 1500;
        int height = 1500;
        DataStore dsBBox = Collec.getDataStore(new File("/tmp/5eme.gpkg"));
        SimpleFeatureCollection bb = dsBBox.getFeatureSource(dsBBox.getTypeNames()[0]).getFeatures();
        GridCoverage2D bbRaster = Rasters.rasterize(bb, "mask", new Dimension(width, height), bb.getBounds(), "mask");
    }

    /**
     * This method
     *
     * @return
     */
    public static GridCoverage2D writeCoordinates() {
        return null;
    }

    public static void makeUniqueRaster() throws IOException, FactoryException {
        DataStore dsBBox = Collec.getDataStore(new File(Util.getRootFolder(), "5eme.gpkg"));
        ReferencedEnvelope bb = dsBBox.getFeatureSource(dsBBox.getTypeNames()[0]).getFeatures().getBounds();
        int width = 1500;
        int height = 1500;

        // Rasterizing buildings and households
        DataStore dsAddress = Collec.getDataStore(new File(Util.getRootFolder(), "IGN/outban_75_20201002Vemr.gpkg"));
        SimpleFeatureCollection address = dsAddress.getFeatureSource(dsAddress.getTypeNames()[0]).getFeatures();
        DataStore dsBuilding = Collec.getDataStore(new File(Util.getRootFolder(), "ICI/BuildingSyntheticPop2.shp"));
        GridCoverage2D batRaster = Rasters.rasterize(affectPopToAddress(dsBuilding.getFeatureSource(dsBuilding.getTypeNames()[0]).getFeatures(), address), "NB_INDIV", new Dimension(width, height), bb, "NB_INDIV");
//        Rasters.writeGeotiff(batRaster, new File(Util.getRootFolder(), "rasters/households.geotiff"));
        System.out.println("Done rasterizing addresses");
        System.out.println();
        dsBuilding.dispose();

        // Rasterizing Working Place
        DataStore dsWP = Collec.getDataStore(new File(Util.getRootFolder(), "POI/SIRENE-WorkingPlace.gpkg"));
        GridCoverage2D rasterWorkforce = Rasters.rasterize(Collec.convertAttributeToFloat(summarizeWP(dsWP.getFeatureSource(dsWP.getTypeNames()[0]).getFeatures(), address), "WorkforceEstimate"), "WorkforceEstimate", new Dimension(width, height), bb, "WorkforceEstimate");
//        Rasters.writeGeotiff(rasterWorkforce, new File(Util.getRootFolder(), "/rasters/workforce.geotiff"));
        System.out.println("Done rasterizing working places");
        dsWP.dispose();
        System.out.println();

        //Get the restaurants
        DataStore dsPOI = Collec.getDataStore(new File(Util.getRootFolder(), "ICI/POI.gpkg"));
//        String[] attrRestos = {"POINumber", "POIimportance"};
        String attrRestos = "POINumber";
        GridCoverage2D rasterResto = Rasters.rasterize(Collec.convertAttributeToFloat(summarizePOI(SirenePOI.getRestaurant(dsPOI.getFeatureSource(dsPOI.getTypeNames()[0]).getFeatures()),
                address), attrRestos), attrRestos, new Dimension(width, height), bb, attrRestos);
        System.out.println("done rasterizing retaurants");
        System.out.println();
//        Rasters.writeGeotiff(rasterResto, new File(Util.getRootFolder(), "/rasters/resto.geotiff"));
        dsPOI.dispose();

        //get the pedestrian infos (from QGIS, failed to do it with GeoTools yet)
        GridCoverage2D ped = Rasters.importRaster(new File(Util.getRootFolder(), "rasters/cheminementPieton.tif"));
        System.out.println("ped.getCoordinateReferenceSystem() = " + ped.getCoordinateReferenceSystem());
        // merge those rasters together


        BandMergeProcess bm = new BandMergeProcess();

        GridCoverage2D rastermerged = bm.execute(Arrays.asList(ped,
                rasterResto, rasterWorkforce, batRaster), null, null, null);
//                batRaster),null,null,null);

        Rasters.writeGeotiff(rastermerged, new File(Util.getRootFolder(), "rasters/out.geotiff"));
        dsAddress.dispose();
    }

//    public static GridCoverage2D generatePedestrianRepRaster() throws IOException {
//
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

    /**
     * @param workingPlaceSFC
     * @param addressesSFC
     * @return
     * @throws FactoryException
     */
    public static SimpleFeatureCollection summarizeWP(SimpleFeatureCollection workingPlaceSFC, SimpleFeatureCollection addressesSFC) throws FactoryException {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        //builder
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        String geomName = workingPlaceSFC.getSchema().getGeometryDescriptor().getLocalName();
        sfTypeBuilder.add("WorkforceEstimate", Double.class);
        sfTypeBuilder.setName("SummarizedWorkingPlace");
//        sfTypeBuilder.setCRS(workingPlaceSFC.getSchema().getCoordinateReferenceSystem());
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
                    builder.set("WorkforceEstimate", SireneWorkingPlace.makeWorkforceAverage(overlapping));
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
                if (addressGeom != null || !addressGeom.isEmpty()) {
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

    public static SimpleFeatureCollection affectHouseholdsToAddress(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection address) throws IOException {
        return affectFieldToAddress(buildingSFC, address, "NB_LOGTS");
    }

    public static SimpleFeatureCollection affectIdToAddress(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection address) throws IOException {
        return affectFieldToAddress(buildingSFC, address, "ID");
    }

    public static SimpleFeatureCollection affectFieldToAddress(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection address, String attrName) throws IOException {
        DefaultFeatureCollection addresses = new DefaultFeatureCollection();
        SimpleFeatureBuilder sfb = Schemas.addFloatColToSFB(address, attrName);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        try (SimpleFeatureIterator itBuilding = buildingSFC.features()) {
            while (itBuilding.hasNext()) {
                SimpleFeature building = itBuilding.next();
                if (building.getAttribute(attrName) == null || (int) building.getAttribute(attrName) == 0)
                    continue;
                SimpleFeatureCollection addressInBuilding = address.subCollection(ff.within(ff.property(address.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(building.getDefaultGeometry())));
                if (addressInBuilding.size() == 0) {// If no address have been found and the building have households, we look at the closest address and affect the building's households to that address
                    addresses.addAll(affectHouseholdPerAddress(getClosestAddress(address, building, ff), sfb, building));
                } else {// If a single address has been found and they don't have attribute about entrance, we keep it
                    if (!Collec.isCollecContainsAttribute(addressInBuilding, "typ_loc"))
                        addresses.addAll(affectHouseholdPerAddress(addressInBuilding, sfb, building));
                        // if no entrance, we check for further entrances
                    else if (getEntrancesFromAddresses(addressInBuilding, ff).size() == 0)
                        addresses.addAll(affectHouseholdPerAddress(getClosestAddress(address, building, ff), sfb, building));
                    else
                        addresses.addAll(affectHouseholdPerAddress(getEntrancesFromAddresses(addressInBuilding, ff), sfb, building));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collec.exportSFC(addresses, new File("/tmp/entrances"));
        return addresses;
    }

    public static SimpleFeatureCollection affectPopToAddress(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection address) throws IOException {
        DefaultFeatureCollection addresses = new DefaultFeatureCollection();
        SimpleFeatureBuilder sfb = Schemas.addFloatColToSFB(address, "NB_INDIV");
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        try (SimpleFeatureIterator itBuilding = buildingSFC.features()) {
            while (itBuilding.hasNext()) {
                SimpleFeature building = itBuilding.next();
                if ((building.getAttribute("sex-Homme") == null || (double) building.getAttribute("sex-Homme") == 0) &&
                        (building.getAttribute("sex_Femme") == null || (double) building.getAttribute("sex_Femme") == 0))
                    continue;
                SimpleFeatureCollection addressInBuilding = address.subCollection(ff.within(ff.property(address.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(building.getDefaultGeometry())));
                if (addressInBuilding.size() == 0) {// If no address have been found and the building have households, we look at the closest address and affect the building's households to that address
                    addresses.addAll(affectIndividualsPerAddress(getClosestAddress(address, building, ff), sfb, building));
                } else {// If a single address has been found and they don't have attribute about entrance, we keep it
                    if (!Collec.isCollecContainsAttribute(addressInBuilding, "typ_loc"))
                        addresses.addAll(affectIndividualsPerAddress(addressInBuilding, sfb, building));
                        // if no entrance, we check for further entrances
                    else if (getEntrancesFromAddresses(addressInBuilding, ff).size() == 0)
                        addresses.addAll(affectIndividualsPerAddress(getClosestAddress(address, building, ff), sfb, building));
                    else
                        addresses.addAll(affectIndividualsPerAddress(getEntrancesFromAddresses(addressInBuilding, ff), sfb, building));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collec.exportSFC(addresses, new File("/tmp/entrances"));
        return addresses;
    }

    public static SimpleFeatureCollection getEntrancesFromAddresses(SimpleFeatureCollection addresses, FilterFactory2 ff) {
        if (!Collec.isCollecContainsAttribute(addresses, "typ_loc")) {
            System.out.println("getEntrancesFromAddresses: not BANO");
            return addresses;
        }
        return addresses.subCollection(ff.like(ff.property("typ_loc"), "entrance"));
    }

    /**
     * Affect a number of households (logements from BDTopo) to the closest address.
     *
     * @param addressesSFC   list of addresses
     * @param addressBuilder builder to create new points
     * @param building       buildings containing <i>NB_LOGTS</i> attribute
     * @return a collection of {@link SimpleFeature} with addressesSFC geometries and number of households
     */
    public static SimpleFeatureCollection affectHouseholdPerAddress(SimpleFeatureCollection addressesSFC, SimpleFeatureBuilder addressBuilder, SimpleFeature building) {
        if (addressesSFC == null)
            return new DefaultFeatureCollection();
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        SimpleFeatureType addressSchema = addressesSFC.getSchema();
        try (SimpleFeatureIterator addressesIt = addressesSFC.features()) {
            while (addressesIt.hasNext()) {
                SimpleFeature address = addressesIt.next();
                int lgtSize = Math.round((int) building.getAttribute("NB_LOGTS") / addressesSFC.size());
                addressBuilder.set("NB_LOGTS", lgtSize);
                for (AttributeDescriptor attr : addressSchema.getAttributeDescriptors())
                    addressBuilder.set(new NameImpl(attr.getLocalName()), address.getAttribute(attr.getName()));
                addressBuilder.set(addressSchema.getGeometryDescriptor().getLocalName(), address.getDefaultGeometry());
                result.add(addressBuilder.buildFeature(Attribute.makeUniqueId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Affect a number of individuals (from H24 export) to the closest address.
     *
     * @param addressesSFC   list of addresses
     * @param addressBuilder builder to create new points
     * @param building       buildings containing <i>NB_INDIV</i> attribute
     * @return a collection of {@link SimpleFeature} with addressesSFC geometries and number of households
     */
    public static SimpleFeatureCollection affectIndividualsPerAddress(SimpleFeatureCollection addressesSFC, SimpleFeatureBuilder addressBuilder, SimpleFeature building) {
        if (addressesSFC == null)
            return new DefaultFeatureCollection();
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        SimpleFeatureType addressSchema = addressesSFC.getSchema();
        try (SimpleFeatureIterator addressesIt = addressesSFC.features()) {
            while (addressesIt.hasNext()) {
                SimpleFeature address = addressesIt.next();
                int nbIndiv = (int) Math.round(((double) building.getAttribute("sex_Femme") + (double) building.getAttribute("sex-Homme")) / addressesSFC.size());
                addressBuilder.set("NB_INDIV", nbIndiv);
                for (AttributeDescriptor attr : addressSchema.getAttributeDescriptors())
                    addressBuilder.set(new NameImpl(attr.getLocalName()), address.getAttribute(attr.getName()));
                addressBuilder.set(addressSchema.getGeometryDescriptor().getLocalName(), address.getDefaultGeometry());
                result.add(addressBuilder.buildFeature(Attribute.makeUniqueId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Recursively look for an address meter by meter. Best if address has is a "entrance" type
     *
     * @param addressSFC
     * @return
     */
    public static SimpleFeatureCollection getClosestAddress(SimpleFeatureCollection addressSFC, SimpleFeature building, FilterFactory2 ff) {
        double x = 0;
        //we check meters by meters if there's entrances
        while (x < 75) {
            SimpleFeatureCollection closeAddress = addressSFC.subCollection(ff.dwithin(ff.property(addressSFC.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(building.getDefaultGeometry()), x, "meters"));
            if (closeAddress.size() > 0)
                if (!Collec.isCollecContainsAttribute(addressSFC, "typ_loc"))
                    return closeAddress;
                else if (getEntrancesFromAddresses(closeAddress, ff).size() > 0)
                    return getEntrancesFromAddresses(closeAddress, ff);
            x++;
        }
        System.out.println("no addresses for building : " + building);
        return null;
    }
}
