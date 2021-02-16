package fr.ici.dataImporter.insee;

import fr.ici.dataImporter.util.Geocode;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.io.Csv;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

public class SirenePOI extends SireneEntry {

    public static File nomenclatureFile = new File("src/main/resources/NAFRev2POI.csv");
    String[] classement = new String[5];

    public SirenePOI() {
        super();
    }

    public SirenePOI(String nAdresse, String adresse, String typeVoie, String codePos, String amenityCode, String nomenclature,
                     String name, String siret, String trancheEffectifsUniteLegale) throws IOException {
        super("POI",nAdresse, adresse, typeVoie, codePos, amenityCode, getAmenitySourceName(amenityCode, nomenclature), nomenclature, name, siret, trancheEffectifsUniteLegale);
        makeClassement();
        if (this.isValid())
            this.attendance = generateAttendance(trancheEffectifsUniteLegale, amenityCode);
            this.attendanceIndice = SirenePOI.generateAttendanceCode(this.attendance);
    }

    public SirenePOI(String nAdresse, String adresse, String typeVoie, String codePos, String amenityCode, String amenityName, String nomenclature,
                     String name, String siret, String workforceNormalized, Point p) throws IOException {
        super("POI", nAdresse, adresse, typeVoie, codePos, amenityCode, amenityName, nomenclature, name, siret, workforceNormalized, p);
        if (this.isValid())
            this.attendance = generateAttendance(workforceNormalized, amenityCode);
            this.attendanceIndice = SirenePOI.generateAttendanceCode(this.attendance);
    }

    public static List<SireneEntry> importSirenePOIEntry(File sireneEntryFile) throws IOException {
        DataStore ds = CollecMgmt.getDataStore(sireneEntryFile);
        List<SireneEntry> lS = new ArrayList<>();
        try (SimpleFeatureIterator fIt = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().features()) {
            while (fIt.hasNext()) {
                SimpleFeature feat = fIt.next();
                SirenePOI ap = new SirenePOI((String) feat.getAttribute("nAdresse"), (String) feat.getAttribute("adresse"),
                        (String) feat.getAttribute("typeVoie"), (String) feat.getAttribute("codePos"),
//                        (String) feat.getAttribute("amenityCode"), (String) feat.getAttribute("amenity"),
                        (String) feat.getAttribute("codeAmenit"), (String) feat.getAttribute("amenite"),
                        (String) feat.getAttribute("nomenclatr"), (String) feat.getAttribute("name"), (String) feat.getAttribute("siret"),
                        feat.getAttribute("workforceNormalized") == null ? "" : (String) feat.getAttribute("workforceNormalized"), (Point) feat.getDefaultGeometry());
                lS.add(ap);
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        ds.dispose();
        return lS;
    }

    public static int generateAttendanceCode(String attendance) {
        switch (attendance) {
            case "veryLow":
                return 1;
            case "low":
                return 2;
            case "moderate":
                return 3;
            case "high":
                return 4;
            case "veryHigh":
                return 5;
        }
        return 0;
    }

    public static String generateAttendance(String trancheEffectifs, String amenityCode) throws IOException {
        String ratio = Csv.getCell(nomenclatureFile, "Code", amenityCode, "rapportClient/NbEmployes");
        // todo change switch to real code when I have more time
        switch (trancheEffectifs) {
            case "":
            case "null":
            case "NULL":
            case "NN":
            case "00":
            case "0":
                return "0";
            case "01":
                switch (ratio) {
                    case "faible":
                    case "moyen":
                        return "veryLow";
                    case "fort":
                        return "low";
                }
            case "02":
                switch (ratio) {
                    case "faible":
                        return "veryLow";
                    case "moyen":
                        return "low";
                    case "fort":
                        return "moderate";
                }
            case "03":
                switch (ratio) {
                    case "faible":
                    case "moyen":
                        return "low";
                    case "fort":
                        return "high";
                }
            case "11":
                switch (ratio) {
                    case "faible":
                        return "low";
                    case "moyen":
                        return "high";
                    case "fort":
                        return "veryHigh";
                }
            case "12":
                switch (ratio) {
                    case "faible":
                        return "high";
                    case "moyen":
                    case "fort":
                        return "veryHigh";
                }
            case "21":
            case "22":
            case "31":
            case "32":
            case "41":
            case "42":
            case "51":
            case "52":
            case "53":
                return "veryHigh";
        }
        throw new InvalidPropertiesFormatException("SirenePOI.generateAttendance: haven't found correspondences for " + amenityCode + " and ratio " + ratio + " and tranche effectif " + trancheEffectifs);
    }

    public static String getAmenitySourceName(String amenityCode, String nomenclature) throws IOException {
        switch (nomenclature) {
            case "NAFRev2":
                return SireneImport.classSIRENEEntryNAFRev2(amenityCode, new File("src/main/resources/NAFRev2POI.csv"))[3];
            case "NAF1993":
                return SireneImport.classSIRENEEntryNAF1993(amenityCode, false, new File("src/main/resources/NAF93-retravailCERTU.csv"))[3];
            case "NAFRev1":
                return SireneImport.classSIRENEEntryNAF1993(amenityCode, true, new File("src/main/resources/NAF93-retravailCERTU.csv"))[3];
            default:
                return SireneImport.classSIRENEEntryNAP(amenityCode, new File("src/main/resources/NAP-POI.csv"))[3];
        }
    }

    /**
     * Get the maximum of the importance of the POI. Used to have an idea on the POI's importances when rasterization
     *
     * @param poiSFC
     * @return
     */
    public static int maxAffluenceIndicator(SimpleFeatureCollection poiSFC) {
        int level = 0;
        try (SimpleFeatureIterator wpIt = poiSFC.features()) {
            while (wpIt.hasNext()) {
                int levelSF = (int) wpIt.next().getAttribute("attendanceIndice");
                if (levelSF > level)
                    level = levelSF;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return level;
    }

    public void makeClassement() throws IOException {
        switch (nomenclature) {
            case "NAFRev2":
                classement = SireneImport.classSIRENEEntryNAFRev2(amenityCode, new File("src/main/resources/NAFRev2POI.csv"));
                break;
            case "NAF1993":
                classement = SireneImport.classSIRENEEntryNAF1993(amenityCode, false, new File("src/main/resources/NAF93-retravailCERTU.csv"));
                break;
            case "NAFRev1":
                classement = SireneImport.classSIRENEEntryNAF1993(amenityCode, true, new File("src/main/resources/NAF93-retravailCERTU.csv"));
                break;
            case "null":
            case "NAP":
                classement = SireneImport.classSIRENEEntryNAP(amenityCode, new File("src/main/resources/NAP-POI.csv"));
                break;
        }
        if (classement == null || classement[0] == null || classement[0].equals(""))
            valid = false;
    }

    public SimpleFeatureBuilder getSireneSFB() {
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName("SirenePOI");
        sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Point.class);
        sfTypeBuilder.add("nAdresse", String.class);
        sfTypeBuilder.add("adresse", String.class);
        sfTypeBuilder.add("typeVoie", String.class);
        sfTypeBuilder.add("codePos", String.class);
        sfTypeBuilder.add("amenity", String.class);
        sfTypeBuilder.add("amenityCode", String.class);
        sfTypeBuilder.add("amenityCodeNormalized", Float.class);
        sfTypeBuilder.add("nomenclatr", String.class);
        sfTypeBuilder.add("name", String.class);
        sfTypeBuilder.add("siret", String.class);
        sfTypeBuilder.add("workforce", String.class);
        sfTypeBuilder.add("workforceNormalized", Integer.class);
        sfTypeBuilder.add("type", String.class);
        sfTypeBuilder.add("categorie", String.class);
        sfTypeBuilder.add("frequence", String.class);
        sfTypeBuilder.add("frequenceCode", Integer.class);
        sfTypeBuilder.add("scoreGeocd", Double.class);
        sfTypeBuilder.add("rstOuv0314", String.class);
        sfTypeBuilder.add("rstOuv1030", String.class);
        sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(featureType);
    }

    public SimpleFeature generateSimpleFeature() {
        String[] geocode;
        try {
            geocode = Geocode.geocodeAdresseDataGouv(completeAddress);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        SimpleFeatureBuilder sfb = getSireneSFB();
        sfb.set(CollecMgmt.getDefaultGeomName(), gf.createPoint(new Coordinate(Double.parseDouble(geocode[1]), Double.parseDouble(geocode[2]))));
        sfb.set("nAdresse", nAddress);
        sfb.set("adresse", address);
        sfb.set("typeVoie", typeRoad);
        sfb.set("codePos", codePos);
        sfb.set("amenity", classement[3]);
        sfb.set("amenityCode", amenityCode);
        sfb.set("amenityCodeNormalized", normalizeCodeAmenity(amenityCode));
        sfb.set("nomenclatr", nomenclature);
        sfb.set("name", name);
        sfb.set("siret", siret);
        sfb.set("workforce", trancheEffectifsEtablissementReadable);
        sfb.set("workforceNormalized", trancheEffectifsEtablissement);
        sfb.set("type", classement[0]);
        sfb.set("categorie", classement[1]);
        sfb.set("frequence", classement[2]);
        sfb.set("frequenceCode", generateAttendanceCode(classement[2]));
        sfb.set("scoreGeocd", Double.valueOf(geocode[0]));
        sfb.set("rstOuv0314", classement[4]);
        sfb.set("rstOuv1030", classement[5]);
        return sfb.buildFeature(Attribute.makeUniqueId());
    }

    public String[] getCSVFirstLine() {
        return new String[]{"id", "siret", "numAdresse", "typeRue", "adresse", "codPostal", "codeAmenity", "intituleAmenity", "nomenclature",
                "type", "cat", "freq", "tranche Effectifs", "name", "reste ouvert selon arrete du 13 03", "reste ouvert selon arrete du 30 10"};
    }

    public String[] getLineForCSV() {
        if (!valid)
            return null;
        return new String[]{siret, nAddress, typeRoad, address, codePos, amenityCode, classement[3], nomenclature, classement[0], classement[1],
                classement[2], trancheEffectifsEtablissementReadable, name, classement[4], classement[5]};
    }

    public boolean equals(SirenePOI in) {
        return in.getnAdresse().equals(nAddress) && in.getTypeVoie().equals(typeRoad) && in.getAdresse().equals(address)
                && in.getCodePos().equals(codePos) && in.getClassement()[1].equals(classement[1])
                && in.getTrancheEffectifsUniteLegale().equals(trancheEffectifsEtablissementReadable) && in.getSiren().equals(siret);
    }

    public boolean equals(String[] line) {
        return line[1].equals(nAddress) && line[2].equals(typeRoad) && line[3].equals(address) && line[4].equals(codePos)
                && line[9].equals(classement[1]) && line[0].equals(siret) && line[11].equals(trancheEffectifsEtablissementReadable);
    }

    public String getnAdresse() {
        return nAddress;
    }

    public void setnAdresse(String nAdresse) {
        this.nAddress = nAdresse;
    }

    public String getAdresse() {
        return address;
    }

    public void setAdresse(String adresse) {
        this.address = adresse;
    }

    public String getTypeVoie() {
        return typeRoad;
    }

    public void setTypeVoie(String typeVoie) {
        this.typeRoad = typeVoie;
    }

    public String getCodePos() {
        return codePos;
    }

    public void setCodePos(String codePos) {
        this.codePos = codePos;
    }

    public String getSiren() {
        return siret;
    }

    public void setSiren(String siren) {
        this.siret = siren;
    }

    public String getTrancheEffectifsUniteLegale() {
        return trancheEffectifsEtablissementReadable;
    }

    public void setTrancheEffectifsUniteLegale(String trancheEffectifsUniteLegale) {
        this.trancheEffectifsEtablissementReadable = trancheEffectifsUniteLegale;
    }

    public String[] getClassement() {
        return classement;
    }

    public void setClassement(String[] classement) {
        this.classement = classement;
    }
}
