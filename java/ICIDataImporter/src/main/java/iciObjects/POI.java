package iciObjects;

import com.opencsv.CSVReader;
import insee.SireneEntry;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import util.Util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class POI {
    public String nAddress, address, typeRoad, codePos, codeIRIS, amenityCode, amenitySourceName, amenityIciName, nomenclature, name;
    public String[] completeAddress = new String[4];
    public String outdoor;
    public String[] idBuilding;
    public Point p;

//    public POI(Geometry geom, String amenityCode, String nomenclature, String name) {
//        this.amenityCode = amenityCode;
//        this.nomenclature = nomenclature;
//        this.name = name;
//    }

    public POI(String nAddress, String address, String typeRoad, String codePos, String amenityCode, String amenitySourceName,
               String amenityIciName, String nomenclature, String name) {
        this.address = address;
        this.nAddress = nAddress;
        this.typeRoad = typeRoad;
        this.codePos = codePos;
        this.amenityCode = amenityCode;
        this.amenitySourceName = amenitySourceName;
        this.amenityIciName = amenityIciName;
        this.nomenclature = nomenclature;
        this.name = name;
    }

    public POI(String nAddress, String address, String typeRoad, String codeIRIS, String amenityCode, String amenitySourceName, String amenityIciName, String nomenclature, String name, Point p) {
        this.address = address;
        this.nAddress = nAddress;
        this.typeRoad = typeRoad;
        this.codeIRIS = codeIRIS;
        this.amenityCode = amenityCode;
        this.amenitySourceName = amenitySourceName;
        this.amenityIciName = amenityIciName;
        this.nomenclature = nomenclature;
        this.name = name;
        this.p = p;
    }

    public POI(String codeIRIS, String amenityCode, String amenityName, String amenityIciName, String nomenclature, String name, Point p) {
        this.codeIRIS = codeIRIS;
        this.amenityCode = amenityCode;
        this.amenitySourceName = amenityName;
        this.amenityIciName = amenityIciName;
        this.nomenclature = nomenclature;
        this.name = name;
        this.p = p;
    }

    public POI() {
    }

    public static String getIciAmenity(String amenityCode, String from) {
        //TODO
        try {
            CSVReader r = new CSVReader(new FileReader(new File(Util.getRootFolder(), "ICI/nomenclatureCommune.csv")));
            switch (from) {
                case "APUR":
                    for (String[] l : r.readAll())
                        if (l[7].equals(amenityCode))
                            return l[0];
                case "OSM":
                    for (String[] l : r.readAll())
                        if (l[4].equals(amenityCode))
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
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public static void delDouble(List<POI> lPOI, List<Building> lBuilding) {
        List<POI> result = new ArrayList<>();
        for (Building b : lBuilding) {
            List<POI> buildingPOI = selectIntersection(lPOI, b.geom);
            //look at names
            List<String> names = new ArrayList<>();
            for (POI poi1 : buildingPOI) {
                for (POI poi2 : buildingPOI) {
                    if (!(poi1 == poi2) && poi1.name != null && poi2.name != null) {
                        if (Util.LevenshteinDistance(poi1.name.toLowerCase(),poi2.name.toLowerCase())< 7) {
                            System.out.println(poi1.name +" and " + poi2 + " are the same !");

                        }
                    }
                }
            }
        }
    }

//public static POI mergePOI(POI poi1, POI poi2){
//        //case
//        if (poi1 instanceof SireneEntry || poi2 instanceof SireneEntry ){
//
//        }
//}

    public static List<POI> selectIntersection(List<POI> lPOI, Geometry g) {
        return lPOI.stream().filter(b -> g.intersects(b.p)).collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException {
        File rootFolder = Util.getRootFolder();
        List<POI> lPOI = new ArrayList<>();
        lPOI.addAll(SireneEntry.importSireneEntry(new File(rootFolder, "INSEE/POI/SIRENE-WorkingPlace.gpkg")));
        lPOI.addAll(SireneEntry.importSireneEntry(new File(rootFolder, "INSEE/POI/SIRENE-POI.gpkg")));
        lPOI.addAll(OsmPOI.importOsmPOI(new File(rootFolder, "OSM/OSMamenities.gpkg")));
        lPOI.addAll(BpePOI.importBpePOI(new File(rootFolder, "INSEE/POI/bpe19Coded-Veme.gpkg")));
        lPOI.addAll(ApurPOI.importApurPOI(new File(rootFolder, "paris/APUR/commercesVeme.gpkg")));
        lPOI.addAll(OsmPOI.importOsmPOI(new File(rootFolder, "OSM/OSMamenities.gpkg")));
        delDouble(lPOI, Building.importBuilding(new File(rootFolder, "IGN/batVeme.gpkg")));
    }

}
