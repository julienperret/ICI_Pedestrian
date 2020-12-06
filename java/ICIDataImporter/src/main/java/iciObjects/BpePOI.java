package iciObjects;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
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

public class BpePOI extends POI {

    public static File nomenclatureFile = new File(Util.getRootFolder(), "INSEE/descriptif/BPE/BPE-varTYPEQU.csv");

    public BpePOI(String codeIRIS, String amenityCode, String amenityName, Point p) throws InvalidPropertiesFormatException {
        super(codeIRIS.replace("_", ""), amenityCode, amenityName, getIciAmenity(amenityCode, "BPE"), "BPE", null, p);
    }

    public static List<BpePOI> importBpePOI(File bpePOIFile) throws IOException {
        DataStore ds = Collec.getDataStore(bpePOIFile);
        List<BpePOI> lB = new ArrayList<>();
        try (SimpleFeatureIterator fIt = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().features()) {
            while (fIt.hasNext()) {
                SimpleFeature feat = fIt.next();
                BpePOI ap = new BpePOI((String) feat.getAttribute("DCIRIS"),
                        (String) feat.getAttribute("TYPEQU"),
                        (String) feat.getAttribute("Type"),
                        ((Point) feat.getDefaultGeometry()));
                lB.add(ap);
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        ds.dispose();
        return lB;
    }
}
