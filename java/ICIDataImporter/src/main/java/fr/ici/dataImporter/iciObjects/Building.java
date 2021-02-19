package fr.ici.dataImporter.iciObjects;

import fr.ici.dataImporter.util.Util;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecTransform;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.geom.Polygons;
import fr.ign.artiscales.tools.io.Csv;
import org.geotools.data.DataStore;
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
public class Building {
    static double[] probabilitiesBuildingSize;
    String ID, nature, usage1, usage2;
    boolean light;
    double height, area;
    int nbStairs, nbLgt;
    List<String> idPOI, idWorkingPlaces, idEntrances;
    List<Double> areaHousingLot;
    Polygon geomBuilding;
    boolean isTight;
    static double functionalRatio = 0.8;

    public Building(String ID, String nature, String usage1, String usage2, boolean light, double height, double area, int nbStairs, int nbLgt, List<Double> areaHousingLot, Polygon geom) {
        this.ID = ID;
        this.nature = nature;
        this.usage1 = usage1;
        this.usage2 = usage2;
        this.light = light;
        this.height = height;
        this.area = area;
        this.nbStairs = nbStairs;
        this.nbLgt = nbLgt;
        this.geomBuilding = geom;
        this.areaHousingLot = areaHousingLot;
    }

    public static void main(String[] args) throws Exception {
        File rootFolder = Util.getRootFolder();
        List<Building> lb = importBuilding(new File(rootFolder, "IGN/batVeme.gpkg"), new File(rootFolder, "INSEE/IRIS-logements.gpkg"), new File(rootFolder, "POI/SIRENE-WorkingPlace.gpkg"), new File(rootFolder, "ICI/POI.gpkg"));
        exportBuildings(lb, new File("/tmp/b.gpkg"));
        analyseDistribution(lb, new File(rootFolder, "INSEE/IRIS-logements.gpkg"));
    }

    static File exportBuildings(List<Building> lB, File fileOut) throws IOException {
        return CollecMgmt.exportSFC(lB.stream().map(Building::generateSimpleFeature).collect(Collectors.toCollection(DefaultFeatureCollection::new)).collection(), fileOut);
    }
    public static List<Building> importBuilding(File buildingBDTopoFile, File inseeLogementStat) throws IOException {
        return importBuilding(buildingBDTopoFile, inseeLogementStat, null, null);
    }
    public static List<Building> importBuilding(File buildingBDTopoFile, File inseeLogementStat, File workingPlaceFile, File POIFile) throws IOException {
        // for every IRIS
        DataStore dsIRIS = CollecMgmt.getDataStore(inseeLogementStat);
        DataStore ds = CollecMgmt.getDataStore(buildingBDTopoFile);
        DefaultFeatureCollection unfit = new DefaultFeatureCollection();
        List<Building> lB = new ArrayList<>();
        try (SimpleFeatureIterator irisIt = dsIRIS.getFeatureSource(dsIRIS.getTypeNames()[0]).getFeatures().features()) {
            while (irisIt.hasNext()) {
                SimpleFeature iris = irisIt.next();
//                System.out.println();
//                System.out.println(iris.getAttribute("NOM_IRIS"));
//                System.out.println();
                // we calculate the distribution of housing sizes for that IRIS
                makeHousingProbabilities(iris);
                // iterate on the iris's buildings . We also sort them by their surfaces (might be better to process the smallest building faster)
                try (SimpleFeatureIterator buildingIt = CollecTransform.selectIntersection(CollecTransform.sortSFCWithArea(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures()), iris).features()) {
                    while (buildingIt.hasNext()) {
                        SimpleFeature building = buildingIt.next();
                        List<Double> distribBuilding = new ArrayList<>();
                        Object nbLgt = building.getAttribute("NB_LOGTS");
                        // for every buildings that has housings
                        if (nbLgt != null && !(nbLgt.equals("0") || nbLgt.equals("00") || nbLgt.equals(0))) {
                            int count = 0;
                            do {
                                distribBuilding = new ArrayList<>();
                                //for every housing unit, we get a size corresponding to the IRIS's probability
                                for (int i = 0; i < (int) nbLgt; i++)
                                    distribBuilding.add(whichAppartmentSize());
                                if (count++ == 5) {
                                    // Try to fit smaller households
                                    // new count
                                    int countSmall = 0;
                                    do {
                                        distribBuilding = new ArrayList<>();
                                        for (int j = 0; j < (int) nbLgt; j++)
                                            distribBuilding.add(whichAppartmentSize(smallHousingDistribution()));
                                        if (countSmall++ == 5) {
                                            System.out.println(("Even with a small distribution, impossible to distribute housing in " + building.getAttribute("ID")+". Making flat average"));
                                            double averageSize = ((Geometry) building.getDefaultGeometry()).getArea()*functionalRatio*(int) building.getAttribute("NB_ETAGES");
                                            for (int j = 0; j < (int) nbLgt; j++)
                                                distribBuilding.add( averageSize/(int) nbLgt );
                                            unfit.add(building);
                                        }
                                    } while (isDistribImpossible(distribBuilding, building) && countSmall <= 5);
                                }
                            } while (isDistribImpossible(distribBuilding, building) && count <= 5);
                            fitHousingProbabilities(distribBuilding);
                        }
                        lB.add(new Building((String) building.getAttribute("ID"), (String) building.getAttribute("NATURE"),
                                (String) building.getAttribute("USAGE1"), (String) building.getAttribute("USAGE2"),
                                building.getAttribute("LEGER").equals("OUI"), (double) building.getAttribute("HAUTEUR"),
                                ((Geometry) building.getDefaultGeometry()).getArea(),
                                building.getAttribute("NB_ETAGES") == null ? 0 : (Integer) building.getAttribute("NB_ETAGES"),
                                building.getAttribute("NB_LOGTS") == null ? 0 : (Integer) building.getAttribute("NB_LOGTS"),
                                distribBuilding,
                                Polygons.getPolygon((Geometry) building.getDefaultGeometry())));
                    }
                } catch (Exception problem) {
                    problem.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        CollecMgmt.exportSFC(unfit, new File("/tmp/unfitBuildings.gpkg"));
        ds.dispose();
        dsIRIS.dispose();
        return lB;
    }


    public static void analyseDistribution(List<Building> lb, File inseeLogementStat) throws IOException {
        DataStore dsIRIS = CollecMgmt.getDataStore(inseeLogementStat);
        HashMap<String, Object[]> stat = new HashMap<>();
        String[] fLine = {"IrisName", "totalHousing", "totalHousingSimulated", "inf30sqm", "inf30sqmSimulated",
                "30_40sqm", "30_40sqmSimulated", "40_60sqm", "40_60sqmSimulated", "60_80sqm", "60_80sqmSimulated", "80_100sqm", "80_100sqmSimulated"
                , "100_120sqm", "100_120sqmSimulated", "Sup120sqm", "Sup120sqmSimulated"};
        try (SimpleFeatureIterator irisIt = dsIRIS.getFeatureSource(dsIRIS.getTypeNames()[0]).getFeatures().features()) {
            while (irisIt.hasNext()) {
                SimpleFeature iris = irisIt.next();
                Object[] line = new Object[16];
                line[0] = getDoubleFromInseeFormatedString(iris, "P17_LOG");
                List<Building> lBuildingIris = lb.stream().filter(x -> x.geomBuilding.intersects((Geometry) iris.getDefaultGeometry())).collect(Collectors.toList());
                line[1] = lBuildingIris.stream().map(x -> (long) x.areaHousingLot.size()).mapToInt(Long::intValue).sum();
                line[2] = getDoubleFromInseeFormatedString(iris, "P17_RP_M30M2");
                line[3] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_M30M2")).mapToInt(num -> num).sum();
                line[4] = getDoubleFromInseeFormatedString(iris, "P17_RP_3040M2");
                line[5] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_3040M2")).mapToInt(num -> num).sum();
                line[6] = getDoubleFromInseeFormatedString(iris, "P17_RP_4060M2");
                line[7] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_4060M2")).mapToInt(num -> num).sum();
                line[8] = getDoubleFromInseeFormatedString(iris, "P17_RP_6080M2");
                line[9] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_6080M2")).mapToInt(num -> num).sum();
                line[10] = getDoubleFromInseeFormatedString(iris, "P17_RP_80100M2");
                line[11] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_80100M2")).mapToInt(num -> num).sum();
                line[12] = getDoubleFromInseeFormatedString(iris, "P17_RP_100120M2");
                line[13] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_100120M2")).mapToInt(num -> num).sum();
                line[14] = getDoubleFromInseeFormatedString(iris, "P17_RP_120M2P");
                line[15] = lBuildingIris.stream().map(x -> getHousingsCategorieOfBuilding(x, "RP_120M2P")).mapToInt(num -> num).sum();
                stat.put((String) iris.getAttribute("NOM_IRIS"), line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Csv.generateCsvFile(stat, new File("/tmp/"), "statBuilding", fLine, false);
    }

    /**
     * Check if the random distribution of housing can fit in the building. A ratio of 0.9 is applied to surface * (nbStairs +1) to represent common parts.
     *
     * @param list     distribution of average size of housing units.
     * @param building building from BDTopo
     * @return true if the number of housing units can fit
     */
    public static boolean isDistribImpossible(List<Double> list, SimpleFeature building) {
        if (list.isEmpty())
            return true;
        double sumDistribution = 0;
        for (double f : list)
            sumDistribution = sumDistribution + f;
        double volBuilding = ((int) building.getAttribute("NB_ETAGES") + 1) *
                ((Geometry) building.getDefaultGeometry()).getArea();
        return !(sumDistribution < volBuilding * functionalRatio);
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
     * substract
     *
     * @param changes
     * @throws ParseException
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
//        System.out.println(percentages[0] + "-" + percentages[1] + "-" + percentages[2] + "-" + percentages[3] + "-" + percentages[4] + "-" + percentages[5] + "-" + percentages[6]);
        probabilitiesBuildingSize = percentages;
    }

    /**
     * Create probability of housing classes for the input IRIS.
     * Get the information for every class of size and multiply them by the ratio of not principal housing (whe assume here that vacant and secondary housing have the same distribution of size as principal housing)
     *
     * @param iris INSEE's IRIS with <i>logement</i> information joined
     * @throws ParseException
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
        return new double[]{60, 30, 10, 0, 0, 0, 0};
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
        sfTypeBuilder.add("nbStairs", Integer.class);
        sfTypeBuilder.add("nbLgt", Integer.class);
        sfTypeBuilder.add("idsPOI", String.class);
        sfTypeBuilder.add("idsWorkingPlaces", String.class);
        sfTypeBuilder.add("idsEntrances", String.class);
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
        double[] percentages = new double[3];
        percentages[0] = getDoubleFromInseeFormatedString(iris, "P17_RSECOCC");
        percentages[1] = getDoubleFromInseeFormatedString(iris, "P17_RP");
        percentages[2] = getDoubleFromInseeFormatedString(iris, "P17_LOGVAC");
        return getDoubleFromInseeFormatedString(iris, "P17_RP") / (getDoubleFromInseeFormatedString(iris, "P17_RP") + getDoubleFromInseeFormatedString(iris, "P17_LOGVAC") + getDoubleFromInseeFormatedString(iris, "P17_RSECOCC"));
    }

    public double getRoomLeft() {
        if (nbLgt == 0)
            return 0;
        return functionalRatio * geomBuilding.getArea() * nbStairs - areaHousingLot.stream().mapToDouble(map -> map).sum() ;
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
        sfb.set("nbStairs", nbStairs);
        sfb.set("nbLgt", nbLgt);
        sfb.set("idsPOI", idPOI);
        sfb.set("idsWorkingPlaces", idWorkingPlaces);
        sfb.set("idsEntrances", idEntrances);
        sfb.set("nbHousingInf30sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_M30M2"));
        sfb.set("nbHousing3040sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_3040M2"));
        sfb.set("nbHousing4060sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_4060M2"));
        sfb.set("nbHousing6080sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_6080M2"));
        sfb.set("nbHousing80100sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_80100M2"));
        sfb.set("nbHousing100120sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_100120M2"));
        sfb.set("nbHousingSup120sqm", getHousingsCategorieOfBuilding(areaHousingLot, "RP_120M2P"));
        sfb.set("roomLeft",getRoomLeft() );
        return sfb.buildFeature(Attribute.makeUniqueId());
    }
}
