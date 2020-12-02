package insee;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import iciObjects.POI;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class SireneEntry extends POI {

    static GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
    String siret, trancheEffectifsEtablissement, resteOuvertArrete0314, resteOuvertArrete1030;
    boolean valid = true;

    public SireneEntry(String nAddress, String address, String typeRoad, String codePos, String amenityCode,
                       String amenityName, String nomenclature, String name, String siret, String trancheEffectifsEtablissement) {
        super(nAddress, address, typeRoad, codePos, amenityCode, amenityName, getIciAmenity(amenityName, "SIRENE"), nomenclature, name);
        this.nAddress = nAddress;
        completeAddress[0] = nAddress;
        this.address = address;
        completeAddress[2] = address;
        this.typeRoad = typeRoad;
        completeAddress[1] = typeRoad;
        this.codePos = codePos;
        completeAddress[3] = codePos;
        this.siret = siret;
        this.trancheEffectifsEtablissement = transformEffectif(trancheEffectifsEtablissement);
    }

    public SireneEntry() {
        super();
    }

    public static List<SireneEntry> importSireneEntry(File apurSireneEntryFile) throws IOException {
        DataStore ds = Collec.getDataStore(apurSireneEntryFile);
        List<SireneEntry> lS = new ArrayList<>();
        try (SimpleFeatureIterator fIt = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().features()) {
            while (fIt.hasNext()) {
                SimpleFeature feat = fIt.next();
                SireneEntry ap = new SireneEntry((String) feat.getAttribute("nAdresse"), (String) feat.getAttribute("adresse"),
                        (String) feat.getAttribute("typeVoie"), (String) feat.getAttribute("codePos"),
                        (String) feat.getAttribute("codeAmenit"), (String) feat.getAttribute("amenite"),
                        (String) feat.getAttribute("nomenclatr"), (String) feat.getAttribute("name"), (String) feat.getAttribute("siret"),
                        feat.getAttribute("effectifs") == null ? "" : (String) feat.getAttribute("effectifs")) {
                    @Override
                    public SimpleFeatureBuilder getSireneSFB() {
                        return null;
                    }

                    @Override
                    public SimpleFeature generateSimpleFeature() {
                        return null;
                    }

                    @Override
                    public String[] getLineForCSV() {
                        return new String[0];
                    }

                    @Override
                    public boolean equals(String[] line) {
                        return false;
                    }

                    @Override
                    public String[] getCSVFirstLine() {
                        return new String[0];
                    }
                };
                lS.add(ap);
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        ds.dispose();
        return lS;
    }

    public static boolean isActive(String etatAdministratifEtablissement) {
        switch (etatAdministratifEtablissement) {
            case "A":
                return true;
            case "F":
                return false;
        }
        return false;
    }

    public static String transformEffectif(String sireneEntry) {
        switch (sireneEntry) {
            case "":
            case "null":
            case "NULL":
                return "";
            case "NN":
            case "00":
                return "0";
            case "01":
                return "1-2";
            case "02":
                return "3-5";
            case "03":
                return "6-9";
            case "11":
                return "10-19";
            case "12":
                return "20-49";
            case "21":
                return "50-99";
            case "22":
                return "100-199";
            case "31":
                return "200 -249";
            case "32":
                return "250-499";
            case "41":
                return "500-999";
            case "42":
                return "1000-1999";
            case "51":
                return "2000-4999";
            case "52":
                return "5000-9999";
            case "53":
                return "10000+";
        }
        return "";
    }

    public abstract SimpleFeatureBuilder getSireneSFB();

    public abstract SimpleFeature generateSimpleFeature();

    public abstract String[] getLineForCSV();

    public boolean isValid() {
        return valid;
    }

    public abstract boolean equals(String[] line);

    public abstract String[] getCSVFirstLine();

}
