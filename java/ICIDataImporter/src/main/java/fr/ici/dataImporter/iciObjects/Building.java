package fr.ici.dataImporter.iciObjects;

import fr.ici.dataImporter.util.Util;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecTransform;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.geom.Polygons;
import fr.ign.artiscales.tools.io.Csv;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Building integration in the ICI project.
 */
public class Building extends ICIObject {
    static double[] probabilitiesBuildingSize;
    static double functionalRatio = 0.85;
    static boolean DEBUG;


    String nature, usage1, usage2;
    boolean light;
    double height, area;
    int nbFloor, nbLgt, nbWorkingPlace, nbPOI;
    List<String> idPOI, idWorkingPlaces, idEntrances;
    List<Double> areaHousingLot;
    Polygon geomBuilding;

    public Building(String ID, String nature, String usage1, String usage2, boolean light, double height, double area, int nbStairs, int nbLgt, List<Double> areaHousingLot, int nbWorkingPlace, int nbPOI, Polygon geom) {
        this.ID = ID;
        this.type = "BUILDING";
        this.nature = nature;
        this.usage1 = usage1;
        this.usage2 = usage2;
        this.light = light;
        this.height = height;
        this.area = area;
        this.nbFloor = nbStairs;
        this.nbLgt = nbLgt;
        this.geomBuilding = geom;
        this.areaHousingLot = areaHousingLot;
        this.nbWorkingPlace = nbWorkingPlace;
        this.nbPOI = nbPOI;
    }

    public static void main(String[] args) throws Exception {
//        DEBUG = true;
//        File rootFolder = Util.getRootFolder();
//        List<Building> lb = importBuilding(new File(rootFolder, "IGN/batVeme.gpkg"), new File(rootFolder, "INSEE/IRIS-logements.gpkg"), new File(rootFolder, "paris/APUR/commercesVeme.gpkg"), new File(rootFolder,"INSEE/POI/SIRENE-WorkingPlace.gpkg"), new File(rootFolder,"ICI/POI.gpkg"));
//        exportBuildings(lb, new File(rootFolder, "ICI/building.gpkg"));
//        analyseDistribution(lb, new File(rootFolder, "INSEE/IRIS-logements.gpkg"), new File(rootFolder, "ICI/"));
        roomLeftRobustness();
    }

    /**
     * Calculate the robustness of the roomLeft value
     * @throws IOException
     */
    static void roomLeftRobustness() throws IOException {
        File rootFolder = Util.getRootFolder();
        List<List<Building>> llBuilding = new ArrayList<>();
        int repl = 50;
        for (int i = 0; i < repl; i++)
            llBuilding.add(importBuilding(new File(rootFolder, "IGN/batVeme.gpkg"), new File(rootFolder, "INSEE/IRIS-logements.gpkg"), new File(rootFolder, "paris/APUR/commercesVeme.gpkg"), null, null));
        //export one
        exportBuildings(llBuilding.get(0), new File("/tmp/exBuilding"));
        //flat collection
        HashMap<String, List<Double>> distribBuilding = new HashMap<>();
        for (List<Building> lb : llBuilding) {
            for (Building b : lb) {
                List<Double> tmp = new ArrayList<>();
                if (distribBuilding.containsKey(b.ID))
                    tmp.addAll(distribBuilding.get(b.ID));
                tmp.add(b.getRoomLeft());
                distribBuilding.put(b.ID, tmp);
            }
        }

        //export
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName("BuildingRobustness");
        sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Polygon.class);
        sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
        sfTypeBuilder.add("ID", String.class);
        sfTypeBuilder.add("vals", String.class);
        sfTypeBuilder.add("variationCoefficient", Double.class);
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(featureType);
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        for (String id : distribBuilding.keySet()) {
            List<Double> values = distribBuilding.get(id);
            DescriptiveStatistics ds = new DescriptiveStatistics();
            for (double d : values)
                ds.addValue(d);
            sfb.set("variationCoefficient", ds.getStandardDeviation() / ds.getMean());

            Geometry g = null;
            for (Building b : llBuilding.get(0))
                if (b.ID.equals(id)) {
                    g = b.geomBuilding;
                    break;
                }
            sfb.set("ID", id);
            sfb.set("vals", Arrays.stream(ds.getValues()).mapToObj(x -> String.valueOf(Math.round(x))).collect(Collectors.joining(",")));
            sfb.set(CollecMgmt.getDefaultGeomName(), g);
            result.add(sfb.buildFeature(Attribute.makeUniqueId()));
        }
        CollecMgmt.exportSFC(result, new File("/tmp/bVar.gpkg"));
    }

    static void exportBuildings(List<Building> lB, File fileOut) throws IOException {
        CollecMgmt.exportSFC(lB.stream().map(Building::generateSimpleFeature).collect(Collectors.toCollection(DefaultFeatureCollection::new)).collection(), fileOut);
    }

    public List<Double> getAreaHousingLot() {
        return areaHousingLot;
    }

    /**
     * Import buildings and automatically generate a repartition for the housing sizes of buildings
     *
     * @param buildingICI containing the <b>BDTOPO</b> buildings
     * @return A list of ICI's building objects
     * @throws IOException reading data
     */
    public static List<Building> importBuilding(File buildingICI) throws IOException {
        DataStore ds = CollecMgmt.getDataStore(buildingICI);
        List<Building> lB = new ArrayList<>();
        try (SimpleFeatureIterator bIt = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().features()) {
            while (bIt.hasNext()) {
                SimpleFeature building = bIt.next();
                lB.add(new Building((String) building.getAttribute("ID"), (String) building.getAttribute("nature"),
                        (String) building.getAttribute("usage1"), (String) building.getAttribute("usage2"),
                        ((int) building.getAttribute("light"))==0 ? false : true, (double) building.getAttribute("height"),
                        (double) building.getAttribute("area"), (int) building.getAttribute("nbFloors"), (int) building.getAttribute("nbLgt"),
                        generateListDistribHousing(building), (int) building.getAttribute("nbWorkingPlace"), (int) building.getAttribute("nbPOI"),
                        Polygons.getPolygon((Geometry) building.getDefaultGeometry())));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ds.dispose();
        return lB;
    }

    private static List<Double> generateListDistribHousing(SimpleFeature b) {
        List<Double> distribBuilding = new ArrayList<>();
        for (int i = 0; i < (int) b.getAttribute("nbHousingInf30sqm"); i++)
            distribBuilding.add(19d);
        for (int i = 0; i < (int) b.getAttribute("nbHousing3040sqm"); i++)
            distribBuilding.add(35d);
        for (int i = 0; i < (int) b.getAttribute("nbHousing4060sqm"); i++)
            distribBuilding.add(50d);
        for (int i = 0; i < (int) b.getAttribute("nbHousing6080sqm"); i++)
            distribBuilding.add(70d);
        for (int i = 0; i < (int) b.getAttribute("nbHousing80100sqm"); i++)
            distribBuilding.add(90d);
        for (int i = 0; i < (int) b.getAttribute("nbHousing100120sqm"); i++)
            distribBuilding.add(110d);
        for (int i = 0; i < (int) b.getAttribute("nbHousingSup120sqm"); i++)
            distribBuilding.add(140d);
        return distribBuilding;
    }

    /**
     * Import ICI buildings from IGN BDTOPO buildings and automatically generate a repartition for the housing sizes of buildings
     *
     * @param buildingBDTopoFile File containing the <b>BDTOPO</b> buildings
     * @param inseeLogementStat  File containing the INSEE's IRIS enriched with housing statistics
     * @return A list of ICI's building objects
     * @throws IOException reading data
     */
    public static List<Building> importBuilding(File buildingBDTopoFile, File inseeLogementStat) throws IOException {
        return importBuilding(buildingBDTopoFile, inseeLogementStat, null, null, null);
    }

    /**
     * Import ICI buildings from IGN BDTOPO buildings and automatically generate a repartition for the housing sizes of buildings.
     * Adjust with optional manual census of shops. Optionally count working places and POIs.
     *
     * @param buildingBDTopoFile File containing the <b>BDTOPO</b> buildings
     * @param inseeLogementStat  File containing the INSEE's IRIS enriched with housing statistics
     * @param apurPOI            file containing shops (must have a <b>SURF</b> attribute which gives information about them sizes)
     * @param poiFile            file containing POI to be counted
     * @param workingPlacesFile  file containing working places to be counted
     * @return A list of ICI's building objects
     * @throws IOException reading data
     */
    public static List<Building> importBuilding(File buildingBDTopoFile, File inseeLogementStat, File apurPOI, File workingPlacesFile, File poiFile) throws IOException {
        // for every IRIS
        DataStore dsIRIS = CollecMgmt.getDataStore(inseeLogementStat);
        DataStore dsBuilding = CollecMgmt.getDataStore(buildingBDTopoFile);
        DataStore dsAPUR = null;
        if (apurPOI != null)
            dsAPUR = CollecMgmt.getDataStore(apurPOI);
        DataStore dsWP = null;
        if (workingPlacesFile != null)
            dsWP = CollecMgmt.getDataStore(workingPlacesFile);
        DataStore dsPOI = null;
        if (poiFile != null)
            dsPOI = CollecMgmt.getDataStore(poiFile);
        List<Building> lB = new ArrayList<>();
        try (SimpleFeatureIterator irisIt = dsIRIS.getFeatureSource(dsIRIS.getTypeNames()[0]).getFeatures().features()) {
            while (irisIt.hasNext()) {
                SimpleFeature iris = irisIt.next();
                if (DEBUG) {
                    System.out.println();
                    System.out.println(iris.getAttribute("NOM_IRIS"));
                    System.out.println();
                }
                makeHousingProbabilities(iris); // we calculate the distribution of housing sizes for that IRIS
                // iterate on the iris's buildings . We also sort them by their surfaces (might be better to process the smallest building faster)
                try (SimpleFeatureIterator buildingIt = CollecTransform.sortSFCWithField(CollecTransform.selectIntersectMost(addRatioHousingPerVolume(dsBuilding.getFeatureSource(dsBuilding.getTypeNames()[0]).getFeatures()), (Geometry) iris.getDefaultGeometry()), "RatioLGTVol", false).features()) {
                    while (buildingIt.hasNext()) {
                        SimpleFeature building = buildingIt.next();
                        List<Double> distribBuilding = new ArrayList<>();
                        Object nbLgt = building.getAttribute("NB_LOGTS");
                        // for every buildings that has housings
                        if (nbLgt != null && !(nbLgt.equals("0") || nbLgt.equals("00") || nbLgt.equals(0))) {
                            int count = 0;
                            int nbStairs = (int) building.getAttribute("NB_ETAGES") + 1;
                            double shoppingArea = 0;
                            if (apurPOI != null) {// We check if there are shops inside the building (and optionally their sizes)
                                List<SimpleFeature> shops = Arrays.stream(CollecTransform.selectIntersection(dsAPUR.getFeatureSource(dsAPUR.getTypeNames()[0]).getFeatures(), building).toArray(new SimpleFeature[0])).collect(Collectors.toList());
                                // possibility to get the position of the shop in the building (Cour intérieur, too rare to care)
                                if (!shops.isEmpty()) {
                                    nbStairs--; // either way, if there is shops, the 0 floor will be occupy by them
                                    for (SimpleFeature shop : shops)
                                        switch ((int) shop.getAttribute("SURF")) {
                                            case 2:
                                                shoppingArea = shoppingArea + 650;
                                            case 3:
                                                shoppingArea = shoppingArea + 1000;
                                        }
                                }
                            }
                            double floorArea = nbStairs * ((Geometry) building.getDefaultGeometry()).getArea() - shoppingArea;
                            do {
                                distribBuilding = new ArrayList<>();
                                for (int i = 0; i < (int) nbLgt; i++) //for every housing unit, we get a size corresponding to the IRIS's probability
                                    distribBuilding.add(whichAppartmentSize());
                                if (count++ == 10) {  // Try to fit smaller households
                                    int countSmall = 0;     // new count
                                    if (DEBUG)
                                        System.out.println("lil distribution");
                                    do {
                                        distribBuilding = new ArrayList<>();
                                        for (int i = 0; i < (int) nbLgt; i++)
                                            distribBuilding.add(whichAppartmentSize(smallHousingDistribution()));
                                        if (countSmall++ == 5) {
                                            if (DEBUG)
                                                System.out.println(("Even with a small distribution, impossible to distribute housing in " + building.getAttribute("ID") + ". Making flat average"));
                                            distribBuilding = new ArrayList<>();
                                            for (int i = 0; i < (int) nbLgt; i++)
                                                distribBuilding.add(floorArea * functionalRatio / (int) nbLgt);
                                        }
                                    } while (isDistribImpossible(distribBuilding, floorArea) && countSmall <= 5);
                                }
                            } while (isDistribImpossible(distribBuilding, floorArea) && count <= 10);
                            fitHousingProbabilities(distribBuilding);
                        }

                        // Calculation of number of Working Places
                        int nbWorkingPlace = 0;
                        if (workingPlacesFile != null)
                            nbWorkingPlace = CollecTransform.selectIntersection(dsWP.getFeatureSource(dsWP.getTypeNames()[0]).getFeatures(), building).size();

                        // Calculation of number of POI
                        int nbPOI = 0;
                        if (poiFile != null)
                            nbPOI = CollecTransform.selectIntersection(dsPOI.getFeatureSource(dsPOI.getTypeNames()[0]).getFeatures(), building).size();

                        lB.add(new Building((String) building.getAttribute("ID"), (String) building.getAttribute("NATURE"),
                                (String) building.getAttribute("USAGE1"), (String) building.getAttribute("USAGE2"),
                                building.getAttribute("LEGER").equals("OUI"), (double) building.getAttribute("HAUTEUR"),
                                ((Geometry) building.getDefaultGeometry()).getArea(),
                                building.getAttribute("NB_ETAGES") == null ? 0 : (Integer) building.getAttribute("NB_ETAGES") + 1,
                                building.getAttribute("NB_LOGTS") == null ? 0 : (Integer) building.getAttribute("NB_LOGTS"),
                                distribBuilding, nbWorkingPlace, nbPOI, Polygons.getPolygon((Geometry) building.getDefaultGeometry())));
                    }
                } catch (NullPointerException ignore) {
                    System.out.println("no features for " + iris.getAttribute("NOM_IRIS"));
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        dsBuilding.dispose();
        if (apurPOI != null)
            dsAPUR.dispose();
        if (workingPlacesFile != null)
            dsWP.dispose();
        if (poiFile != null)
            dsPOI.dispose();
        dsIRIS.dispose();
        return lB;
    }

    public static SimpleFeatureCollection addRatioHousingPerVolume(SimpleFeatureCollection buildingSFC) {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        String[] ratio = {"RatioLGTVol"};
        SimpleFeatureBuilder sfb = Schemas.addColsToSFB(buildingSFC, ratio, Double.class);
        try (SimpleFeatureIterator it = buildingSFC.features()) {
            while (it.hasNext()) {
                SimpleFeature sf = it.next();
                sfb = Schemas.setFieldsToSFB(sfb, sf);
                sfb.set("RatioLGTVol", ((Geometry) sf.getDefaultGeometry()).getArea() / ((sf.getAttribute("NB_LOGTS") != null && (int) sf.getAttribute("NB_LOGTS") != 0) ? (int) sf.getAttribute("NB_LOGTS") : Double.POSITIVE_INFINITY));
                result.add(sfb.buildFeature(Attribute.makeUniqueId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void analyseDistribution(List<Building> lb, File inseeLogementStat, File outFolder) throws IOException {
        DataStore dsIRIS = CollecMgmt.getDataStore(inseeLogementStat);

        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName("IRISstat");
        sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Polygon.class);
        sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
        sfTypeBuilder.add("nbSimulatedHousing", Integer.class);
        sfTypeBuilder.add("nbTotalHousing", Double.class);
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(featureType);
        DefaultFeatureCollection irisStat = new DefaultFeatureCollection();

        HashMap<String, Object[]> stat = new HashMap<>();
        String[] fLine = {"IrisName", "totalHousing", "totalHousingSimulated", "inf30sqm", "inf30sqmReadjusted", "inf30sqmSimulated",
                "30_40sqm", "30_40sqmReadjusted", "30_40sqmSimulated", "40_60sqm", "40_60sqmReadjusted", "40_60sqmSimulated", "60_80sqm",
                "60_80sqmReadjusted", "60_80sqmSimulated", "80_100sqm", "80_100sqmReadjusted", "80_100sqmSimulated", "100_120sqm",
                "100_120sqmReadjusted", "100_120sqmSimulated", "Sup120sqm", "Sup120sqmReadjusted", "Sup120sqmSimulated"};
        try (SimpleFeatureIterator irisIt = dsIRIS.getFeatureSource(dsIRIS.getTypeNames()[0]).getFeatures().features()) {
            while (irisIt.hasNext()) {
                SimpleFeature iris = irisIt.next();
                Object[] line = new Object[23];
                line[0] = getDoubleFromInseeFormatedString(iris, "P17_LOG");
                List<Building> lBuildingIris = lb.stream().filter(x -> x.geomBuilding.intersects((Geometry) iris.getDefaultGeometry())).collect(Collectors.toList());
                line[1] = lBuildingIris.stream().map(x -> (long) x.areaHousingLot.size()).mapToInt(Long::intValue).sum();
                line[2] = getDoubleFromInseeFormatedString(iris, "P17_RP_M30M2");
                line[3] = getDoubleFromInseeFormatedString(iris, "P17_RP_M30M2") / getPercentageOfPrincipalHouseholds(iris);
                line[4] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_M30M2")).mapToInt(num -> num).sum();
                line[5] = getDoubleFromInseeFormatedString(iris, "P17_RP_3040M2");
                line[6] = getDoubleFromInseeFormatedString(iris, "P17_RP_3040M2") / getPercentageOfPrincipalHouseholds(iris);
                line[7] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_3040M2")).mapToInt(num -> num).sum();
                line[8] = getDoubleFromInseeFormatedString(iris, "P17_RP_4060M2");
                line[9] = getDoubleFromInseeFormatedString(iris, "P17_RP_4060M2") / getPercentageOfPrincipalHouseholds(iris);
                line[10] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_4060M2")).mapToInt(num -> num).sum();
                line[11] = getDoubleFromInseeFormatedString(iris, "P17_RP_6080M2");
                line[12] = getDoubleFromInseeFormatedString(iris, "P17_RP_6080M2") / getPercentageOfPrincipalHouseholds(iris);
                line[13] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_6080M2")).mapToInt(num -> num).sum();
                line[14] = getDoubleFromInseeFormatedString(iris, "P17_RP_80100M2");
                line[15] = getDoubleFromInseeFormatedString(iris, "P17_RP_80100M2") / getPercentageOfPrincipalHouseholds(iris);
                line[16] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_80100M2")).mapToInt(num -> num).sum();
                line[17] = getDoubleFromInseeFormatedString(iris, "P17_RP_100120M2");
                line[18] = getDoubleFromInseeFormatedString(iris, "P17_RP_100120M2") / getPercentageOfPrincipalHouseholds(iris);
                line[19] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_100120M2")).mapToInt(num -> num).sum();
                line[20] = getDoubleFromInseeFormatedString(iris, "P17_RP_120M2P");
                line[21] = getDoubleFromInseeFormatedString(iris, "P17_RP_120M2P") / getPercentageOfPrincipalHouseholds(iris);
                line[22] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_120M2P")).mapToInt(num -> num).sum();
                stat.put((String) iris.getAttribute("NOM_IRIS"), line);

                // generation of an IRIS SFC
                sfb.set("nbSimulatedHousing", lBuildingIris.stream().map(x -> (long) x.areaHousingLot.size()).mapToInt(Long::intValue).sum());
                sfb.set("nbTotalHousing", getDoubleFromInseeFormatedString(iris, "P17_LOG"));
                sfb.set(CollecMgmt.getDefaultGeomName(), iris.getDefaultGeometry());
                irisStat.add(sfb.buildFeature(Attribute.makeUniqueId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CollecMgmt.exportSFC(irisStat, new File(outFolder, "IrisStat.gpkg"));
        Csv.generateCsvFile(stat, outFolder, "statBuilding", fLine, false);
    }

    /**
     * Check if the random distribution of housing can fit in the building. A ratio of 0.9 is applied to surface * (nbStairs +1) to represent common parts.
     *
     * @param list      distribution of average size of housing units.
     * @param floorArea Area of floors used for housing purposes
     * @return true if the number of housing units can fit
     */
    public static boolean isDistribImpossible(List<Double> list, double floorArea) {
        if (list.isEmpty())
            return true;
        return list.stream().mapToDouble(n -> n).sum() > floorArea * functionalRatio;
    }

    public static int getHousingsCategorieOfBuilding(Building b, String housingCategory) {
        return getHousingsCategorieOfBuilding(b.areaHousingLot, housingCategory);
    }

    public static int getHousingsCategorieOfBuilding(List<Double> areaHousingLot, String housingCategory) {

        switch (housingCategory) {
            case "RP_M30M2":
                return (int) areaHousingLot.stream().filter(x -> x < 30).count();
            case "RP_3040M2":
                return (int) areaHousingLot.stream().filter(x -> (x > 30 && x < 40)).count();
            case "RP_4060M2":
                return (int) areaHousingLot.stream().filter(x -> (x > 40 && x < 60)).count();
            case "RP_6080M2":
                return (int) areaHousingLot.stream().filter(x -> (x > 60 && x < 80)).count();
            case "RP_80100M2":
                return (int) areaHousingLot.stream().filter(x -> (x > 80 && x < 100)).count();
            case "RP_100120M2":
                return (int) areaHousingLot.stream().filter(x -> (x > 100 && x < 120)).count();
            case "RP_120M2P":
                return (int) areaHousingLot.stream().filter(x -> x > 120).count();
            default:
                throw new IllegalStateException("Unexpected value: " + housingCategory);
        }
    }

    public static void fitHousingProbabilities(List<Double> changes) throws ParseException {
        double[] changesNormalized = new double[7];
        changesNormalized[0] = getHousingsCategorieOfBuilding(changes, "RP_M30M2");
        changesNormalized[1] = getHousingsCategorieOfBuilding(changes, "RP_3040M2");
        changesNormalized[2] = getHousingsCategorieOfBuilding(changes, "RP_4060M2");
        changesNormalized[3] = getHousingsCategorieOfBuilding(changes, "RP_6080M2");
        changesNormalized[4] = getHousingsCategorieOfBuilding(changes, "RP_80100M2");
        changesNormalized[5] = getHousingsCategorieOfBuilding(changes, "RP_100120M2");
        changesNormalized[6] = getHousingsCategorieOfBuilding(changes, "RP_120M2P");
        fitHousingProbabilities(changesNormalized);
    }

    /**
     * substract affected housings ad recalculate housing probability
     *
     * @param changes values that has been set and needs to be removed from the propability of size affection
     */
    public static void fitHousingProbabilities(double[] changes) throws ParseException {
        double[] percentages = new double[7];
        percentages[0] = probabilitiesBuildingSize[0] - changes[0] > 0 ? probabilitiesBuildingSize[0] - changes[0] : 0;
        percentages[1] = probabilitiesBuildingSize[1] - changes[1] > 0 ? probabilitiesBuildingSize[1] - changes[1] : 0;
        percentages[2] = probabilitiesBuildingSize[2] - changes[2] > 0 ? probabilitiesBuildingSize[2] - changes[2] : 0;
        percentages[3] = probabilitiesBuildingSize[3] - changes[3] > 0 ? probabilitiesBuildingSize[3] - changes[3] : 0;
        percentages[4] = probabilitiesBuildingSize[4] - changes[4] > 0 ? probabilitiesBuildingSize[4] - changes[4] : 0;
        percentages[5] = probabilitiesBuildingSize[5] - changes[5] > 0 ? probabilitiesBuildingSize[5] - changes[5] : 0;
        percentages[6] = probabilitiesBuildingSize[6] - changes[6] > 0 ? probabilitiesBuildingSize[6] - changes[6] : 0;
        if (DEBUG)
            System.out.println(percentages[0] + "-" + percentages[1] + "-" + percentages[2] + "-" + percentages[3] + "-" + percentages[4] + "-" + percentages[5] + "-" + percentages[6]);
        probabilitiesBuildingSize = percentages;
    }

    /**
     * Create probability of housing classes for the input IRIS.
     * Get the information for every class of size and multiply them by the ratio of not principal housing (whe assume here that vacant and secondary housing have the same distribution of size as principal housing)
     *
     * @param iris INSEE's IRIS with <i>logement</i> information joined
     */
    public static void makeHousingProbabilities(SimpleFeature iris) throws ParseException {
        double percentagePrincipalHousehold = getPercentageOfPrincipalHouseholds(iris);
        double[] percentages = new double[7];
        percentages[0] = getDoubleFromInseeFormatedString(iris, "P17_RP_M30M2") / percentagePrincipalHousehold;
        percentages[1] = getDoubleFromInseeFormatedString(iris, "P17_RP_3040M2") / percentagePrincipalHousehold;
        percentages[2] = getDoubleFromInseeFormatedString(iris, "P17_RP_4060M2") / percentagePrincipalHousehold;
        percentages[3] = getDoubleFromInseeFormatedString(iris, "P17_RP_6080M2") / percentagePrincipalHousehold;
        percentages[4] = getDoubleFromInseeFormatedString(iris, "P17_RP_80100M2") / percentagePrincipalHousehold;
        percentages[5] = getDoubleFromInseeFormatedString(iris, "P17_RP_100120M2") / percentagePrincipalHousehold;
        percentages[6] = getDoubleFromInseeFormatedString(iris, "P17_RP_120M2P") / percentagePrincipalHousehold;
        probabilitiesBuildingSize = percentages;
    }

    private static double[] smallHousingDistribution() {
        return new double[]{40, 40, 10, 0, 0, 0, 0};
    }

    /**
     * Get the distribution of the appartment sizes and randomly affect a size to the household by its probability
     *
     * @return average size of the randomly attributed building
     */
    public static double whichAppartmentSize() {
        return whichAppartmentSize(probabilitiesBuildingSize);
    }

    public static double whichAppartmentSize(double[] probability) {
        double sumPer = Arrays.stream(probability).sum();
        if (sumPer == 0)
            return 19.5f;
        double rnd = Math.random();
        double sum = 0;
        int i = 0;
        while (rnd > sum) {
            sum = sum + (probability[i] / sumPer);
            i++;
        }
        switch (i) {
            case 1:
                return 19.5f;
            case 2:
                return 35f;
            case 3:
                return 50f;
            case 4:
                return 70f;
            case 5:
                return 90f;
            case 6:
                return 110f;
            case 7:
                return 140f;
            default:
                throw new IllegalStateException("Unexpected value on whichAppartmentSize()");

        }
    }

    private static double getDoubleFromInseeFormatedString(SimpleFeature sf, String field) throws ParseException {
        return sf.getAttribute(field) != null ? NumberFormat.getInstance(Locale.FRANCE).parse((String) sf.getAttribute(field)).doubleValue() : 0.0;
    }

    public static SimpleFeatureBuilder getBuildingSFB() {
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        sfTypeBuilder.setName("BuildingICI");
        try {
            sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Polygon.class);
        sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
        sfTypeBuilder.add("ID", String.class);
        sfTypeBuilder.add("nature", String.class);
        sfTypeBuilder.add("usage1", String.class);
        sfTypeBuilder.add("usage2", String.class);
        sfTypeBuilder.add("light", Boolean.class);
        sfTypeBuilder.add("height", Double.class);
        sfTypeBuilder.add("area", Double.class);
        sfTypeBuilder.add("nbFloors", Integer.class);
        sfTypeBuilder.add("nbLgt", Integer.class);
        sfTypeBuilder.add("idsPOI", String.class);
        sfTypeBuilder.add("idsWorkingPlace", String.class);
        sfTypeBuilder.add("idsEntrance", String.class);
        sfTypeBuilder.add("nbPOI", Integer.class);
        sfTypeBuilder.add("nbWorkingPlace", Integer.class);
        sfTypeBuilder.add("nbHousingInf30sqm", Integer.class);
        sfTypeBuilder.add("nbHousing3040sqm", Integer.class);
        sfTypeBuilder.add("nbHousing4060sqm", Integer.class);
        sfTypeBuilder.add("nbHousing6080sqm", Integer.class);
        sfTypeBuilder.add("nbHousing80100sqm", Integer.class);
        sfTypeBuilder.add("nbHousing100120sqm", Integer.class);
        sfTypeBuilder.add("nbHousingSup120sqm", Integer.class);
        sfTypeBuilder.add("roomLeft", Double.class);
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(featureType);
    }

    /**
     * return the percentages of the occupation of housing
     * <ul>
     * <li><b>indice 0</b> : percentage of principal housing</li>
     * <li><b>indice 1</b> : percentage of secondary housing</li>
     * <li><b>indice 2</b> : percentage of vacant housing</li>
     * </ul>
     *
     * @param iris IRIS
     * @return the type of housing
     */
    public static double getPercentageOfPrincipalHouseholds(SimpleFeature iris) throws ParseException {
        return getDoubleFromInseeFormatedString(iris, "P17_RP") / (getDoubleFromInseeFormatedString(iris, "P17_RP") + getDoubleFromInseeFormatedString(iris, "P17_LOGVAC") + getDoubleFromInseeFormatedString(iris, "P17_RSECOCC"));
    }

    public double getRoomLeft() {
        if (nbLgt == 0)
            return functionalRatio * geomBuilding.getArea() * nbFloor;
        return functionalRatio * geomBuilding.getArea() * nbFloor - areaHousingLot.stream().mapToDouble(map -> map).sum();
    }

    public SimpleFeature generateSimpleFeature() {
        SimpleFeatureBuilder sfb = getBuildingSFB();
        sfb.set(CollecMgmt.getDefaultGeomName(), geomBuilding);
        sfb.set("ID", ID);
        sfb.set("nature", nature);
        sfb.set("usage1", usage1);
        sfb.set("usage2", usage2);
        sfb.set("light", light);
        sfb.set("height", height);
        sfb.set("area", area);
        sfb.set("nbFloors", nbFloor);
        sfb.set("nbLgt", nbLgt);
        sfb.set("idsPOI", idPOI);
        sfb.set("idsWorkingPlace", idWorkingPlaces);
        sfb.set("idsEntrance", idEntrances);
        sfb.set("nbWorkingPlace", nbWorkingPlace);
        sfb.set("nbPOI", nbPOI);
        sfb.set("nbHousingInf30sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_M30M2"));
        sfb.set("nbHousing3040sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_3040M2"));
        sfb.set("nbHousing4060sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_4060M2"));
        sfb.set("nbHousing6080sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_6080M2"));
        sfb.set("nbHousing80100sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_80100M2"));
        sfb.set("nbHousing100120sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_100120M2"));
        sfb.set("nbHousingSup120sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_120M2P"));
        sfb.set("roomLeft", getRoomLeft());
        return sfb.buildFeature(Attribute.makeUniqueId());
    }

    public Polygon getGeomBuilding() {
        return geomBuilding;
    }
}
