package fr.ici.dataImporter.iciObjects;

import com.opencsv.CSVReader;
import fr.ici.dataImporter.insee.BpePOI;
import fr.ici.dataImporter.insee.SirenePOI;
import fr.ici.dataImporter.osm.OsmPOI;
import fr.ici.dataImporter.paris.ApurPOI;
import fr.ici.dataImporter.util.Util;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * General class for point of interest in the ICI project
 */
public class POI extends ICIObject {
    public String openingHour, attendance, nAddress, address, typeRoad, codePos, codeIRIS, amenityCode, amenityTypeNameSource, amenityTypeNameICI, nomenclature, name;
    public int attendanceIndice;
    public String[] completeAddress = new String[4];
    public String[] idBuilding;
    public Point p;
    public double potentialArea;


    public POI(String ID, String nAddress, String address, String typeRoad, String codePos, String amenityCode, String amenityTypeNameSource, String amenityTypeNameICI, String nomenclature, Point p) {
        this.ID = ID;
        this.type = "POI";
        this.address = address;
        this.nAddress = nAddress;
        this.typeRoad = typeRoad;
        this.codePos = codePos;
        this.amenityCode = amenityCode;
        this.amenityTypeNameSource = amenityTypeNameSource;
        this.amenityTypeNameICI = amenityTypeNameICI;
        this.nomenclature = nomenclature;
        this.p = p;
    }

    public POI(String ID, String type, String nAddress, String address, String typeRoad, String codePos, String amenityCode, String amenityTypeNameSource, String amenityTypeNameICI, String nomenclature, String name) {
        this.ID = ID;
        this.type = type;
        this.address = address;
        this.nAddress = nAddress;
        this.typeRoad = typeRoad;
        this.codePos = codePos;
        this.amenityCode = amenityCode;
        this.amenityTypeNameSource = amenityTypeNameSource;
        this.amenityTypeNameICI = amenityTypeNameICI;
        this.nomenclature = nomenclature;
        this.name = name;
    }

    public POI(String ID, String nAddress, String address, String typeRoad, String codeIRIS, String amenityCode, String amenityTypeNameSource, String amenityTypeNameICI, String nomenclature, String name, String openingHour, Point p) {
        this.ID = ID;
        this.type = "POI";
        this.address = address;
        this.nAddress = nAddress;
        this.typeRoad = typeRoad;
        this.codeIRIS = codeIRIS;
        this.amenityCode = amenityCode;
        this.amenityTypeNameSource = amenityTypeNameSource;
        this.amenityTypeNameICI = amenityTypeNameICI;
        this.nomenclature = nomenclature;
        this.name = name;
        this.openingHour = openingHour;
        this.p = p;
    }

    public POI(String ID, String codeIRIS, String amenityCode, String amenityName, String amenityTypeNameICI, String nomenclature, String name, Point p) {
        this.ID = ID;
        this.type = "POI";
        this.codeIRIS = codeIRIS;
        this.amenityCode = amenityCode;
        this.amenityTypeNameSource = amenityName;
        this.amenityTypeNameICI = amenityTypeNameICI;
        this.nomenclature = nomenclature;
        this.name = name;
        this.p = p;
    }

    public POI() {
    }

    public POI(String ID, String address, String nAddress, String typeRoad, String codeIRIS, String amenityCode, String amenityTypeNameSource, String amenityTypeNameICI, String nomenclature, String name, String attendance, String openingHour, Point pt) {
        this.ID = ID;
        this.type = "POI";
        this.address = address;
        this.nAddress = nAddress;
        this.typeRoad = typeRoad;
        this.codeIRIS = codeIRIS;
        this.amenityCode = amenityCode;
        this.amenityTypeNameSource = amenityTypeNameSource;
        this.amenityTypeNameICI = amenityTypeNameICI;
        this.nomenclature = nomenclature;
        this.name = name;
        this.attendance = attendance;
        this.attendanceIndice = SirenePOI.generateAttendanceCode(attendance);
        this.openingHour = openingHour;
        this.p = pt;
    }

//    public static void main(String[] args) throws IOException {
//        File rootFolder = Util.getRootFolder();
//        List<POI> lPOI = importPOIFromUsualSources(rootFolder);
//        exportListPOI(lPOI, new File(rootFolder, "ICI/POI.gpkg"));
//        exportHighAttendancePOI(lPOI, new File(rootFolder, "ICI/POIHighAttendance.gpkg"));
//    }

    /**
     * Import POI from an already generated file (loose the OSM's capacity attribute).
     *
     * @param poiFile the geo file exported with the {@link #exportListPOI(List, File)}} method.
     * @return a fist of {@link POI} objects
     * @throws IOException
     */
    public static List<POI> importPOI(File poiFile) throws IOException {
        DataStore ds = CollecMgmt.getDataStore(poiFile);
        List<POI> lPOI = new ArrayList<>();
        try (SimpleFeatureIterator poiIt = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().features()) {
            while (poiIt.hasNext()) {
                SimpleFeature poi = poiIt.next();
                lPOI.add(new POI((String) poi.getAttribute("idICI"), (String) poi.getAttribute("address"),
                        (String) poi.getAttribute("nAddress"), (String) poi.getAttribute("typeRoad"),
                        (String) poi.getAttribute("codeIRIS"), (String) poi.getAttribute("amenityCode"),
                        (String) poi.getAttribute("amenitySourceName"), (String) poi.getAttribute("amenityIciTypeName"),
                        (String) poi.getAttribute("nomenclature"), (String) poi.getAttribute("name"),
                        (String) poi.getAttribute("attendance"), (String) poi.getAttribute("openingHour"),
                        (Point) poi.getDefaultGeometry()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ds.dispose();
        return lPOI;
    }

    public static List<POI> importPOIFromUsualSources(File rootFolder) throws IOException {
        List<POI> lPOI = new ArrayList<>();
        lPOI.addAll(SirenePOI.importSirenePOIEntry(new File(rootFolder, "INSEE/POI/SIRENE-POI.gpkg")));
        lPOI.addAll(OsmPOI.importOsmPOI(new File(rootFolder, "OSM/OSMamenities.gpkg")));
        lPOI.addAll(BpePOI.importBpePOI(new File(rootFolder, "INSEE/POI/bpe19Coded-Veme.gpkg")));
        lPOI.addAll(ApurPOI.importApurPOI(new File(rootFolder, "paris/APUR/commercesVeme.gpkg")));
        lPOI = delDouble(lPOI, Building.importBuilding(new File(rootFolder, "IGN/batVeme.gpkg"), new File(rootFolder, "INSEE/IRIS-logements.gpkg")));
        return lPOI;
    }

    /**
     * Get the ICI amenity type for any subclass's type. Generate a common nomenclature if not present
     *
     * @param amenityCode source amenity code
     * @param from        the name of the class TODO change in class would be more clean
     * @return the <i>Ici Amenity</i> nomenclature
     */
    public static String getIciAmenity(String amenityCode, String from) {
        try {
            CSVReader r = new CSVReader(new FileReader(new File(Util.getRootFolder(), "ICI/nomenclatureCommune.csv").exists() ? new File(Util.getRootFolder(), "ICI/nomenclatureCommune.csv") : new MakeCommonNomenclature(new File(Util.getRootFolder(), "nomenclatureCommune.csv")).getCommonNomenclatureFile()));
            switch (from) {
                case "APUR":
                    for (String[] l : r.readAll())
                        if (l[7].equals(amenityCode))
                            return l[0];
                case "OSM":
                    for (String[] l : r.readAll())
                        if (l[4].equals(MakeCommonNomenclature.translateOSMAmenityNames(amenityCode)))
                            return l[0];
                case "SIRENE":
                    for (String[] l : r.readAll())
                        if (l[1].equals(amenityCode))
                            return l[0];
                case "BPE":
                    for (String[] l : r.readAll())
                        if (l[5].equals(amenityCode))
                            return l[0];
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
//        throw new InvalidPropertiesFormatException("CommonNomenclature doesn't know " + amenityCode + " from " + from);
        return "";
    }

    /**
     * Check for every POI if they can be considered as a double. TODO add relations
     *
     * @param lPOI      {@link List} of {@link POI}s
     * @param lBuilding {@link List} of {@link Building}s
     * @return the list of POI with no double
     */
    public static List<POI> delDouble(List<POI> lPOI, List<Building> lBuilding) {
        List<POI> result = new ArrayList<>();
        for (Building b : lBuilding) {
            List<POI> poiInBuilding = selectIntersection(lPOI, b.geomBuilding.buffer(0.2));
            // Sort points by their ICI nomenclatures
            HashMap<String, List<POI>> poiByType = sortPOIByType(poiInBuilding);
            for (List<POI> lP : poiByType.values()) {
                if (lP.size() > 1) { // if types for various points are alike
                    List<POI> alreadyAdd = new ArrayList<>();
                    // Different occurrences of same type.
                    for (POI pObs : lP) {
                        if (alreadyAdd.contains(pObs))
                            continue;
                        // If the POI is from the same source as every other ones, they are not double and we add them all
                        if (lP.stream().filter(x -> x.nomenclature.equals(pObs.nomenclature)).count() == lP.size()) {
                            result.addAll(lP);
                            alreadyAdd.addAll(lP);
                            continue;
                        }
                        // Get POIs with same names
                        List<POI> lSameName = lP.stream().filter(x -> !(x == pObs) && x.name != null && pObs.name != null && !x.name.equals("null") && !pObs.name.equals("null") && !x.name.equals("") && !pObs.name.equals("") &&
                                (((float) Util.LevenshteinDistance(pObs.name.toLowerCase(), x.name.toLowerCase())) / ((double) x.name.toLowerCase().length())) < 0.2).collect(Collectors.toList());
                        if (lSameName.size() > 1) {
                            alreadyAdd.addAll(lSameName);
                            result.add(mergePOI(lSameName));
                            continue;
                        }
//                        // get the same types
//                        List<POI> lSameType = lP.stream().filter(x -> !(x == pObs) &&
//                                pObs.amenityIciName.equals(x.amenityIciName)).collect(Collectors.toList());
//                        if (lSameName.size() > 0) {
//                            if ()
//                        }
                        // case BPE has the same type of another - we merge them (this is most of the time in disfavour of the BPE)
                    }
                } else
                    result.add(lP.get(0));
            }
        }
        return result;
    }

    /**
     * Merge POI that should be alike. Different rules regarding their sources.
     *
     * @param lToMerge
     * @return
     */
    private static POI mergePOI(List<POI> lToMerge) {
        System.out.println("merge " + lToMerge);
        StringBuilder address = new StringBuilder();
        StringBuilder nAddress = new StringBuilder();
        StringBuilder typeRoad = new StringBuilder();
        StringBuilder codeIRIS = new StringBuilder();
        StringBuilder amenityCode = new StringBuilder();
        StringBuilder amenitySourceName = new StringBuilder();
        StringBuilder amenityIciName = new StringBuilder();
        StringBuilder nomenclature = new StringBuilder();
        StringBuilder name = new StringBuilder();
        StringBuilder attendance = new StringBuilder();
        StringBuilder openingHour = new StringBuilder();
        Point pt = null;
        for (POI p : lToMerge) {
            nomenclature.append("Merge");
            if (p instanceof ApurPOI) {
                // As they've been checked mannually, we guess that the addresses of APUR are the most precise
                address.delete(0, address.length()).append(p.address);
                nAddress.delete(0, nAddress.length()).append(nAddress);
                typeRoad.delete(0, typeRoad.length()).append(p.typeRoad);
                codeIRIS.delete(0, codeIRIS.length()).append(p.codeIRIS);
                amenityCode.append("_Apur:").append(p.amenityCode);
                amenitySourceName.append("_Apur:").append(p.amenityTypeNameSource);
                attendance.append("_Apur:").append(p.attendance);
                amenityIciName.delete(0, amenityIciName.length()).append(p.amenityTypeNameICI);
                nomenclature.append("_Apur:").append(p.nomenclature);
                pt = p.p;
            }
            if (p instanceof BpePOI) {
                //we set the address infos only if it hasn't been set before
                if (address.length() == 0) {
                    address.append(p.address);
                    nAddress.append(p.nAddress);
                    typeRoad.append(p.typeRoad);
                    codeIRIS.append(p.typeRoad);
                    amenityIciName.append(p.amenityTypeNameICI);
                }
                if (pt == null)
                    pt = p.p;
                nomenclature.append("_Bpe:").append(p.nomenclature);
                amenityCode.append("_Bpe:").append(p.amenityCode);
                amenitySourceName.append("_Bpe:").append(p.amenityTypeNameSource);
            }
            if (p instanceof OsmPOI) {
                // TODO add attendance in OsmPOI but almost null everytime
                openingHour.append(p.openingHour);
                // If no Sirene points have set name
                if (name.length() == 0)
                    name.append(p.name);
                if (amenityIciName.length() == 0)
                    amenityIciName.append(p.amenityTypeNameICI);
                if (pt == null)
                    pt = p.p;
                nomenclature.append("_Osm:").append(p.nomenclature);
                amenityCode.append("_Osm:").append(p.amenityCode);
            }
            if (p instanceof SirenePOI) {
                if (address.length() == 0) {
                    address.append(p.address);
                    nAddress.append(p.nAddress);
                    typeRoad.append(p.typeRoad);
                    codeIRIS.append(p.typeRoad);
                    amenityIciName.append(p.amenityTypeNameICI);
                }
                if (pt == null)
                    pt = p.p;
                nomenclature.append("_Sirene:").append(p.nomenclature);
                amenityCode.append("_Sirene:").append(p.amenityCode);
                amenitySourceName.append("_Sirene:").append(p.amenityTypeNameSource);
                attendance.append("_Sirene:").append(p.attendance);
                name.append(p.name);
            }
        }
        return new POI("POI-" + Attribute.makeUniqueId(), address.toString(),
                nAddress.toString(), typeRoad.toString(), codeIRIS.toString(), amenityCode.toString(), amenitySourceName.toString(), amenityIciName.toString(), nomenclature.toString(), name.toString(), attendance.toString(), openingHour.toString(), pt);
    }

    /**
     * Put POI in type categories regarding to the ICI type.
     *
     * @param lPOI
     * @return
     */
    private static HashMap<String, List<POI>> sortPOIByType(List<POI> lPOI) {
        HashMap<String, List<POI>> result = new HashMap<>();
        for (POI poi : lPOI) {
            List<POI> tmp;
            if (result.get(poi.amenityTypeNameICI) != null)
                tmp = result.get(poi.amenityTypeNameICI);
            else
                tmp = new ArrayList<>();
            tmp.add(poi);
            result.put(poi.amenityTypeNameICI, tmp);
        }
        return result;
    }

    public static List<POI> selectIntersection(List<POI> lPOI, Geometry g) {
        return lPOI.stream().filter(b -> g.intersects(b.p)).collect(Collectors.toList());
    }

    public static void exportListPOI(List<? extends POI> lPOI, File fileOut) throws IOException {
        DefaultFeatureCollection result = lPOI.stream().map(POI::generateSF).collect(Collectors.toCollection(DefaultFeatureCollection::new));
        CollecMgmt.exportSFC(result, fileOut);
    }

    public static void exportHighAttendancePOI(List<POI> lPOI, File fileOut) throws IOException {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        for (POI poi : lPOI)
            if (poi.attendance != null && (poi.attendance.equals("high") || poi.attendance.equals("veryHigh")))
                result.add(poi.generateSF());
        CollecMgmt.exportSFC(result, fileOut);
    }

    public static SimpleFeatureCollection getRestaurant(SimpleFeatureCollection poi) throws IOException {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        try (SimpleFeatureIterator it = poi.features()) {
            while (it.hasNext()) {
                SimpleFeature feat = it.next();
                if (feat.getAttribute("amenityIciName").equals("loisir.restauration") || feat.getAttribute("amenityIciName").equals("restauration"))
                    result.add(feat);
            }
        } catch (Error e) {
            e.printStackTrace();
            return result.collection();
        }
        return result.collection();
    }

    @Override
    public String toString() {
        return "POI{" +
                "idICI='" + ID + '\'' +
                ", openingHour='" + openingHour + '\'' +
                ", attendance='" + attendance + '\'' +
                ", attendanceIndice='" + attendanceIndice + '\'' +
                ", nAddress='" + nAddress + '\'' +
                ", address='" + address + '\'' +
                ", typeRoad='" + typeRoad + '\'' +
                ", codePos='" + codePos + '\'' +
                ", codeIRIS='" + codeIRIS + '\'' +
                ", amenityCode='" + amenityCode + '\'' +
                ", amenitySourceName='" + amenityTypeNameSource + '\'' +
                ", amenityIciName='" + amenityTypeNameICI + '\'' +
                ", nomenclature='" + nomenclature + '\'' +
                ", name='" + name + '\'' +
                ", idBuilding=" + Arrays.toString(idBuilding) +
                ", p=" + p +
                '}';
    }

    public SimpleFeature generateSF() {
        SimpleFeatureBuilder sfb = getPOISFB();
        sfb.set(CollecMgmt.getDefaultGeomName(), p);
        sfb.set("idICI", ID);
        sfb.set("nAddress", nAddress);
        sfb.set("address", address);
        sfb.set("typeRoad", typeRoad);
        sfb.set("codeIRIS", codePos);
        sfb.set("amenityCode", amenityCode);
        sfb.set("amenitySourceName", amenityTypeNameSource);
        sfb.set("amenityIciName", amenityTypeNameICI);
        sfb.set("nomenclature", nomenclature);
        sfb.set("name", name);
        sfb.set("attendance", attendance);
        sfb.set("attendanceIndice", attendanceIndice);
        sfb.set("openingHour", openingHour);
        if (this instanceof OsmPOI)
            sfb.set("capacity", ((OsmPOI) this).getCapacity());
        return sfb.buildFeature(Attribute.makeUniqueId());
    }

    public SimpleFeatureBuilder getPOISFB() {
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName("IciPOI");
        sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Point.class);
        sfTypeBuilder.add("idICI", String.class);
        sfTypeBuilder.add("nAddress", String.class);
        sfTypeBuilder.add("address", String.class);
        sfTypeBuilder.add("typeRoad", String.class);
        sfTypeBuilder.add("codeIRIS", String.class);
        sfTypeBuilder.add("amenityCode", String.class);
        sfTypeBuilder.add("amenitySourceName", String.class);
        sfTypeBuilder.add("amenityIciName", String.class);
        sfTypeBuilder.add("nomenclature", String.class);
        sfTypeBuilder.add("name", String.class);
        sfTypeBuilder.add("attendance", String.class);
        sfTypeBuilder.add("attendanceIndice", Integer.class);
        sfTypeBuilder.add("openingHour", String.class);
        sfTypeBuilder.add("capacity", String.class);
        sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(featureType);
    }
}


