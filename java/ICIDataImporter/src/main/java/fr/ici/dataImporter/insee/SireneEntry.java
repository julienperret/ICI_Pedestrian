package fr.ici.dataImporter.insee;

import fr.ici.dataImporter.iciObjects.POI;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.feature.simple.SimpleFeature;

import java.util.InvalidPropertiesFormatException;
import java.util.jar.Attributes;

public abstract class SireneEntry extends POI {

    static GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
    String siret, trancheEffectifsEtablissementReadable, trancheEffectifsEtablissement, resteOuvertArrete0314, resteOuvertArrete1030;
    boolean valid = true;

    public SireneEntry(String type, String nAddress, String address, String typeRoad, String codePos, String amenityCode,
                       String amenityName, String nomenclature, String name, String siret, String trancheEffectifsEtablissement) throws InvalidPropertiesFormatException {
        super(type + "-" + Attribute.makeUniqueId(), nAddress, address, typeRoad, codePos, amenityCode, amenityName, getIciAmenity(amenityCode, "SIRENE"), nomenclature, name);
        this.nAddress = nAddress;
        completeAddress[0] = nAddress;
        this.address = address;
        completeAddress[2] = address;
        this.typeRoad = typeRoad;
        completeAddress[1] = typeRoad;
        this.codePos = codePos;
        completeAddress[3] = codePos;
        this.siret = siret;
        this.trancheEffectifsEtablissementReadable = transformWorkforce(trancheEffectifsEtablissement,true);
        this.trancheEffectifsEtablissement =  transformWorkforce(trancheEffectifsEtablissement,false);
    }

    public SireneEntry(String type, String nAddress, String address, String typeRoad, String codePos, String amenityCode,
                       String amenityName, String nomenclature, String name, String siret, String trancheEffectifsEtablissement, Point p) throws InvalidPropertiesFormatException {
        super(type + "-" + Attribute.makeUniqueId(),nAddress, address, typeRoad, codePos, amenityCode, amenityName, getIciAmenity(amenityCode, "SIRENE"), nomenclature, name);
        this.p = p;
        this.nAddress = nAddress;
        completeAddress[0] = nAddress;
        this.address = address;
        completeAddress[2] = address;
        this.typeRoad = typeRoad;
        completeAddress[1] = typeRoad;
        this.codePos = codePos;
        completeAddress[3] = codePos;
        this.siret = siret;
        this.trancheEffectifsEtablissementReadable = transformWorkforce(trancheEffectifsEtablissement, true);
        this.trancheEffectifsEtablissement =  transformWorkforce(trancheEffectifsEtablissement,false);
    }

    public SireneEntry() {
        super();
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

    /**
     * Workforce value is normalize in SIRENE data. Make those categories human readable.
     * @param workforceCode the corresponding code
     * @param readable if true, return the string in a readable way (bounds of people). If false, parse it to a castable int
     * @return the slice of workforce corresponding to the code
     */
    public static String transformWorkforce(String workforceCode, boolean readable) {
        if (!readable)
            switch (workforceCode) {
                case "":
                case "null":
                case "NULL":
                case "NN":
                    return "0";
                default:
                    return workforceCode.replaceFirst("^0+(?!$)", "");
        }
            else {
            switch (workforceCode) {
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
    }

    /**
     * In NAF ranking, ranks can have a letter as a last character that specifies their type. Remove if if exists and convert into a float for normalization
     * @param codeAmenity NAF code of the amenity
     * @return a proper float code
     */
    public static float normalizeCodeAmenity(String codeAmenity){
        if (!Character.isDigit(codeAmenity.charAt(codeAmenity.length()-1)))
            codeAmenity = codeAmenity.substring(0, codeAmenity.length()-1);
        return Float.parseFloat(codeAmenity);
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
