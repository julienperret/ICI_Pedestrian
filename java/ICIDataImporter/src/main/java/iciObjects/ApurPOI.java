package iciObjects;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApurPOI extends POI {
    public String areaBound;

    public ApurPOI(int nAddress, String address, String typeRoad, String codeIRIS, String amenityCode, String amenityName, int areaBound, Point p) {
        super(String.valueOf(nAddress), address, typeRoad, codeIRIS, amenityCode, amenityName, getIciAmenity(amenityName, "APUR"), "APUR", null, p);
        this.areaBound = getAreaBound(areaBound);
    }

    private static String getAreaBound(int in ){
        switch (in){
            case 1:
                return "<300m²";
            case 2 :
                return "[300m2,1000m2]";
            case 3 :
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
