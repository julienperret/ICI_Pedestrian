package insee;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import util.Geocode;

import java.io.File;
import java.io.IOException;

public class SirenePOI extends SireneEntry {

    String[] classement = new String[5];

    public SirenePOI() {
        super();
    }

    public SirenePOI(String nAdresse, String adresse, String typeVoie, String codePos, String amenityCode, String nomenclature,
                     String name, String siret, String trancheEffectifsUniteLegale) throws IOException {
        super(nAdresse, adresse, typeVoie, codePos, amenityCode, getAmenitySourceName(amenityCode, nomenclature), nomenclature, name, siret, trancheEffectifsUniteLegale);
        makeClassement();
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
        sfTypeBuilder.add(Collec.getDefaultGeomName(), Point.class);
        sfTypeBuilder.add("nAdresse", String.class);
        sfTypeBuilder.add("adresse", String.class);
        sfTypeBuilder.add("typeVoie", String.class);
        sfTypeBuilder.add("codePos", String.class);
        sfTypeBuilder.add("codeAmenit", String.class);
        sfTypeBuilder.add("amenite", String.class);
        sfTypeBuilder.add("nomenclatr", String.class);
        sfTypeBuilder.add("name", String.class);
        sfTypeBuilder.add("siret", String.class);
        sfTypeBuilder.add("effectifs", String.class);
        sfTypeBuilder.add("type", String.class);
        sfTypeBuilder.add("categorie", String.class);
        sfTypeBuilder.add("frequence", String.class);
        sfTypeBuilder.add("scoreGeocd", Double.class);
        sfTypeBuilder.add("rstOuv0314", String.class);
        sfTypeBuilder.add("rstOuv1030", String.class);
        sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
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
        sfb.set(Collec.getDefaultGeomName(), gf.createPoint(new Coordinate(Double.parseDouble(geocode[1]), Double.parseDouble(geocode[2]))));
        sfb.set("nAdresse", nAddress);
        sfb.set("adresse", address);
        sfb.set("typeVoie", typeRoad);
        sfb.set("codePos", codePos);
        sfb.set("codeAmenit", amenityCode);
        sfb.set("amenite", classement[3]);
        sfb.set("nomenclatr", nomenclature);
        sfb.set("name", name);
        sfb.set("siret", siret);
        sfb.set("effectifs", trancheEffectifsEtablissement);
        sfb.set("type", classement[0]);
        sfb.set("categorie", classement[1]);
        sfb.set("frequence", classement[2]);
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
                classement[2], trancheEffectifsEtablissement, name, classement[4], classement[5]};
    }

    public boolean equals(SirenePOI in) {
		return in.getnAdresse().equals(nAddress) && in.getTypeVoie().equals(typeRoad) && in.getAdresse().equals(address)
				&& in.getCodePos().equals(codePos) && in.getClassement()[1].equals(classement[1])
				&& in.getTrancheEffectifsUniteLegale().equals(trancheEffectifsEtablissement) && in.getSiren().equals(siret);
	}

    public boolean equals(String[] line) {
		return line[1].equals(nAddress) && line[2].equals(typeRoad) && line[3].equals(address) && line[4].equals(codePos)
				&& line[9].equals(classement[1]) && line[0].equals(siret) && line[11].equals(trancheEffectifsEtablissement);
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
        return trancheEffectifsEtablissement;
    }

    public void setTrancheEffectifsUniteLegale(String trancheEffectifsUniteLegale) {
        this.trancheEffectifsEtablissement = trancheEffectifsUniteLegale;
    }

    public String[] getClassement() {
        return classement;
    }

    public void setClassement(String[] classement) {
        this.classement = classement;
    }
}
