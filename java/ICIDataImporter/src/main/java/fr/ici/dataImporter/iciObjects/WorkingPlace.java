package fr.ici.dataImporter.iciObjects;

import fr.ici.dataImporter.insee.SireneEntry;
import fr.ici.dataImporter.insee.SireneImport;
import fr.ici.dataImporter.util.Geocode;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
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
import java.util.List;

public class WorkingPlace extends SireneEntry {

    public WorkingPlace() {
        super();
    }

    public WorkingPlace(String nAdresse, String adresse, String typeVoie, String codePos, String amenityCode, String nomenclature,
                        String denominationUniteLegale, String siret, String workforceNormalized) throws IOException {
        super("WorkingPlace", nAdresse, adresse, typeVoie, codePos, amenityCode, getAmenitySourceName(amenityCode, nomenclature), nomenclature, denominationUniteLegale, siret, workforceNormalized);
        makeClassement();
    }
    public WorkingPlace(String ID, String address,
                       String nAddress,String typeRoad,String codePos,String amenityCode,String amenityTypeNameSource,String amenityTypeNameICI,
                       String    nomenclature,   String name ,String siret, String workforce,String workforceNormalized, String  rstOuv0314,String rstOuv1030,
                       Point p){
        super(ID,"WorkingPlace",address,nAddress, typeRoad, codePos, amenityCode, amenityTypeNameSource, amenityTypeNameICI,
                nomenclature, name , siret, workforce, workforceNormalized, p);
            this.resteOuvertArrete0314 = rstOuv0314;
            this.resteOuvertArrete1030 = rstOuv1030;
    }

    /**
     * Import WorkingPlace objects from an already generated file.
     *
     * @param poiFile the geo file exported with the {@link #exportListPOI(List, File)}} method.
     * @return a fist of {@link POI} objects
     * @throws IOException from getDataStore
     */
    public static List<WorkingPlace> importWorkingPlace(File poiFile) throws IOException {
        DataStore ds = CollecMgmt.getDataStore(poiFile);
        List<WorkingPlace> lPOI = new ArrayList<>();
        try (SimpleFeatureIterator poiIt = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().features()) {
            while (poiIt.hasNext()) {
                SimpleFeature poi = poiIt.next();
                lPOI.add(new WorkingPlace((String) poi.getAttribute("ID"), (String) poi.getAttribute("address"),
                        (String) poi.getAttribute("nAddress"), (String) poi.getAttribute("typeRoad"),
                        (String) poi.getAttribute("codePos"), (String) poi.getAttribute("amenityCode"),
                        (String) poi.getAttribute("amenityTypeNameSource"), (String) poi.getAttribute("amenityTypeNameICI"),
                        (String) poi.getAttribute("nomenclature"),(String) poi.getAttribute("name"),(String) poi.getAttribute("siret"),
                        (String) poi.getAttribute("workforce"),(String) poi.getAttribute("workforceNormalized"),
                        (String) poi.getAttribute("rstOuv0314"),(String) poi.getAttribute("rstOuv1030"),
                        (Point) poi.getDefaultGeometry()) {
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ds.dispose();
        return lPOI;
    }

    private static String getAmenitySourceName(String amenityCode, String nomenclature) throws IOException {
        switch (nomenclature) {
            case "NAFRev2":
                return SireneImport.classSIRENEEntryNAFRev2(amenityCode, new File("src/main/resources/NAFRev2.csv"))[3];
            case "NAF1993":
                return SireneImport.classSIRENEEntryNAF1993(amenityCode, false, new File("src/main/resources/NAF93and03.csv"))[3];
            case "NAFRev1":
                return SireneImport.classSIRENEEntryNAF1993(amenityCode, true, new File("src/main/resources/NAF93and03.csv"))[3];
            default:
                return SireneImport.classSIRENEEntryNAP(amenityCode, new File("src/main/resources/NAP.csv"))[3];
        }
    }

    public static double makeWorkforceAverage(SimpleFeatureCollection workingPlace) {
        double result = 0;
        try (SimpleFeatureIterator wpIt = workingPlace.features()) {
            while (wpIt.hasNext())
                result = result + getAverageWorkingForce(wpIt.next());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static double getAverageWorkingForce(SimpleFeature wp) {
        switch ((int) wp.getAttribute("workforceNormalized")) {
            case 0:
                return 0;
            case 1:
                return 1.5;
            case 2:
                return 4;
            case 3:
                return 7.5;
            case 4:
                return 14.5;
            case 12:
                return 34.5;
            case 21:
                return 74.5;
            case 22:
                return 149.5;
            case 31:
                return 224.5;
            case 32:
                return 374.5;
            case 41:
                return 749.5;
            case 42:
                return 1499.5;
            case 51:
                return 3499.5;
            case 52:
                return 7499.5;
            case 53:
                return 15000;
        }
//		throw new InvalidPropertiesFormatException("no workforce value found");
        return 0;
    }

    public void makeClassement() throws IOException {
        switch (nomenclature) {
            case "NAFRev2":
                String[] classement = SireneImport.classSIRENEEntryNAFRev2(amenityCode, new File("src/main/resources/NAFRev2.csv"));
                amenityTypeNameSource = classement[3];
                resteOuvertArrete0314 = classement[4];
                resteOuvertArrete1030 = classement[5];
                break;
            case "NAF1993":
                amenityTypeNameSource = SireneImport.classSIRENEEntryNAF1993(amenityCode, false, new File("src/main/resources/NAF93and03.csv"))[3];
                break;
            case "NAFRev1":
                amenityTypeNameSource = SireneImport.classSIRENEEntryNAF1993(amenityCode, true, new File("src/main/resources/NAF93and03.csv"))[3];
                break;
            case "null":
            case "NAP":
                amenityTypeNameSource = SireneImport.classSIRENEEntryNAP(amenityCode, new File("src/main/resources/NAP.csv"))[3];
                break;
        }
        if (amenityTypeNameSource == null || amenityTypeNameSource.equals("") || amenityTypeNameSource.equalsIgnoreCase("null"))
            valid = false;
    }

    public boolean equals(String[] line) {
        return line[0].equals(siret) && line[1].equals(nAddress) && line[2].equals(typeRoad) && line[3].equals(address) && line[4].equals(codePos)
                && line[9].equals(workforce);
    }

    @Override
    public String[] getLineForCSV() {
        return new String[]{siret, nAddress, typeRoad, address, codePos, amenityTypeNameSource, amenityCode, nomenclature, name,
                workforce, resteOuvertArrete0314, resteOuvertArrete1030};
    }

    public String[] getCSVFirstLine() {
        return new String[]{"id", "siret", "numAdresse", "typeRue", "adresse", "codPostal", "amenite", "codeAmenity", "nomenclature", "name",
                "tranche Effectifs", "reste ouvert selon arrete du 13 03", "reste ouvert selon arrete du 30 10"};
    }

    @Override
    public SimpleFeatureBuilder getSireneSFB() {
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName("SireneWorkingPlace");
        sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Point.class);
        sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
        sfTypeBuilder.add("ID", String.class);
        sfTypeBuilder.add("nAddress", String.class);
        sfTypeBuilder.add("address", String.class);
        sfTypeBuilder.add("typeRoad", String.class);
        sfTypeBuilder.add("codePos", String.class);
        sfTypeBuilder.add("amenityTypeNameSource", String.class);
        sfTypeBuilder.add("amenityTypeNameICI", String.class);
        sfTypeBuilder.add("amenityCode", String.class);
        sfTypeBuilder.add("amenityCodeNormalized", Float.class);
        sfTypeBuilder.add("nomenclature", String.class);
        sfTypeBuilder.add("name", String.class);
        sfTypeBuilder.add("siret", String.class);
        sfTypeBuilder.add("workforce", String.class);
        sfTypeBuilder.add("workforceNormalized", String.class);
        sfTypeBuilder.add("scoreGeocode", Double.class);
        sfTypeBuilder.add("rstOuv0314", String.class);
        sfTypeBuilder.add("rstOuv1030", String.class);
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(featureType);
    }

    @Override
    public SimpleFeature generateSimpleFeature() {
        SimpleFeatureBuilder sfb = getSireneSFB();
        String[] geocode;
        try {
            geocode = Geocode.geocodeAdresseDataGouv(completeAddress);
            try {
                sfb.set(CollecMgmt.getDefaultGeomName(),
                        gf.createPoint(new Coordinate(Double.parseDouble(geocode[1]), Double.parseDouble(geocode[2]))));
            } catch (NullPointerException np) {
                sfb.set(CollecMgmt.getDefaultGeomName(), gf.createPoint(new Coordinate(0, 0)));
            }
            sfb.set("ID", this.getID());
            sfb.set("nAddress", nAddress);
            sfb.set("address", address);
            sfb.set("typeRoad", typeRoad);
            sfb.set("codePos", codePos);
            sfb.set("amenityTypeNameSource", amenityTypeNameSource);
            sfb.set("amenityTypeNameICI", amenityTypeNameICI);
            sfb.set("amenityCode", amenityCode);
            sfb.set("amenityCodeNormalized", normalizeCodeAmenity(amenityCode));
            sfb.set("nomenclature", nomenclature);
            sfb.set("name", name);
            sfb.set("siret", siret);
            sfb.set("workforce", workforce);
            sfb.set("workforceNormalized", workforceNormalized);
            sfb.set("scoreGeocode", Double.valueOf(geocode[0]));
            sfb.set("rstOuv0314", resteOuvertArrete0314);
            sfb.set("rstOuv1030", resteOuvertArrete0314);
            return sfb.buildFeature(Attribute.makeUniqueId());
        } catch (IOException e) {
            e.printStackTrace();
            return sfb.buildFeature(Attribute.makeUniqueId());
        }
    }
    public String getWorkforce(){
        return this.workforce;
    }
}
