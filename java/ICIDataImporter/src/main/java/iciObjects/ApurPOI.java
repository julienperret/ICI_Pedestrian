package iciObjects;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.io.Csv;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

public class ApurPOI extends POI {
    public static File nomenclatureFile = new File(Util.getRootFolder(), "paris/APUR/APURNomenclature.csv");
    public String areaBound;

    public ApurPOI(int nAddress, String address, String typeRoad, String codeIRIS, String amenityCode, String amenityName, int areaBound, Point p) throws InvalidPropertiesFormatException {
        super(String.valueOf(nAddress), address, typeRoad, codeIRIS, amenityCode, amenityName, getIciAmenity(amenityCode, "APUR"), "APUR", p);
        this.areaBound = getAreaBound(areaBound);
        try {
            this.attendance = generateAttendance(areaBound, amenityCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String generateAttendance(int areaBound, String amenityCode) throws IOException {
        String ratio = Csv.getCell(nomenclatureFile, "Code activité BDCom 2017", amenityCode, "rapportClient/TailleEtablissement");
        switch (areaBound) {
            case 1:
                switch (ratio) {
                    case "trèsFaible":
                    case "faible":
                        return "veryLow";
                    case "moyen":
                        return "low";
                    case "fort":
                        return "moderate";
                    case "trèsFort":
                        return "high";
                }
            case 2:
                switch (ratio) {
                    case "trèsFaible":
                    case "faible":
                        return "moderate";
                    case "moyen":
                        return "high";
                    case "fort":
                    case "trèsFort":
                        return "veryHigh";
                }
            case 3:
                switch (ratio) {
                    case "trèsFaible":
                        return "moderate";
                    case "faible":
                        return "high";
                    case "moyen":
                    case "fort":
                    case "trèsFort":
                        return "veryHigh";
                }
        }
        throw new InvalidPropertiesFormatException("ApurPOI.generateAttendance: haven't found correspondences for "+amenityCode + " and ratio "+ ratio + " and areaBound" + areaBound);
    }

    private static String getAreaBound(int in) {
        switch (in) {
            case 1:
                return "<300m²";
            case 2:
                return "[300m2,1000m2]";
            case 3:
                return ">1000m2";
        }
        return "";
    }

    public static List<ApurPOI> importApurPOI(File apurPOIFile) throws IOException {
        DataStore ds = Collec.getDataStore(apurPOIFile);
        List<ApurPOI> lB = new ArrayList<>();
        try (SimpleFeatureIterator fIt = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().features()) {
            while (fIt.hasNext()) {
                SimpleFeature feat = fIt.next();
                ApurPOI ap = new ApurPOI((int) feat.getAttribute("NUM"), (String) feat.getAttribute("TYP_VOIE"),
                        (String) feat.getAttribute("LIB_VOIE"), (String) feat.getAttribute("IRIS"),
                        (String) feat.getAttribute("CODACT"), (String) feat.getAttribute("LIBACT"),
                        (int) feat.getAttribute("SURF"),
                        ((Point) feat.getDefaultGeometry()));
                lB.add(ap);
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        ds.dispose();
        return lB;
    }

//    private static SimpleFeatureBuilder getApurPOISFB() {
//
//
//    }
}
