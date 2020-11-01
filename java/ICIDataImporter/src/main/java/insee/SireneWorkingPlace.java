package insee;

import java.io.File;
import java.io.IOException;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import util.Geocode;

public class SireneWorkingPlace extends SireneEntry {

	public SireneWorkingPlace() {
		super();
	}

	public SireneWorkingPlace(String nAdresse, String adresse, String typeVoie, String codePos, String codeAmenite, String nomenclature,
			String denominationUniteLegale, String siret, String trancheEffectifsEtablissement) throws IOException {
		super(nAdresse, adresse, typeVoie, codePos, codeAmenite, nomenclature, denominationUniteLegale, siret, trancheEffectifsEtablissement);
		makeClassement();
	}

	public void makeClassement() throws IOException {
		switch (nomenclature) {
		case "NAFRev2":
			String[] classement = SireneImport.classSIRENEEntryNAFRev2(codeAmenite, new File("src/main/resources/NAFRev2.csv"));
			amenite = classement[3];
			resteOuvertArrete0314 = classement[4];
			resteOuvertArrete1030 = classement[5];
			break;
		case "NAF1993":
			amenite = SireneImport.classSIRENEEntryNAF1993(codeAmenite, false, new File("src/main/resources/NAF93and03.csv"))[3];
			break;
		case "NAFRev1":
			amenite = SireneImport.classSIRENEEntryNAF1993(codeAmenite, true, new File("src/main/resources/NAF93and03.csv"))[3];
			break;
		case "null":
		case "NAP":
			amenite = SireneImport.classSIRENEEntryNAP(codeAmenite, new File("src/main/resources/NAP.csv"))[3];
			break;
		}
		if (amenite == null || amenite.equals("") || amenite.toLowerCase().equals("null"))
			valid = false;
	}

	public boolean equals(String[] line) {
		if (line[0].equals(siret) && line[1].equals(nAdresse) && line[2].equals(typeVoie) && line[3].equals(adresse) && line[4].equals(codePos)
				&& line[9].equals(trancheEffectifsEtablissement))
			return true;
		return false;
	}

	@Override
	public String[] getLineForCSV() {
		String[] line = { siret, nAdresse, typeVoie, adresse, codePos, amenite, codeAmenite, nomenclature, denominationEtablissement,
				trancheEffectifsEtablissement, resteOuvertArrete0314, resteOuvertArrete1030 };
		return line;
	}

	public String[] getCSVFirstLine() {
		String[] firstCol = { "id", "siret", "numAdresse", "typeRue", "adresse", "codPostal", "amenite", "codeAmenity", "nomenclature", "name",
				"tranche Effectifs", "reste ouvert selon arrete du 13 03", "reste ouvert selon arrete du 30 10" };
		return firstCol;
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
		sfTypeBuilder.add(Collec.getDefaultGeomName(), Point.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		sfTypeBuilder.add("nAdresse", String.class);
		sfTypeBuilder.add("adresse", String.class);
		sfTypeBuilder.add("typeVoie", String.class);
		sfTypeBuilder.add("codePos", String.class);
		sfTypeBuilder.add("amenite", String.class);
		sfTypeBuilder.add("codeAmenite", String.class);
		sfTypeBuilder.add("name", String.class);
		sfTypeBuilder.add("siret", String.class);
		sfTypeBuilder.add("trancheEffectifsEtablissement", String.class);
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
			geocode = Geocode.geocodeAdresseDataGouv(completeAdress);
			try {
				sfb.set(Collec.getDefaultGeomName(),
						(Geometry) gf.createPoint(new Coordinate(Double.valueOf(geocode[1]), Double.valueOf(geocode[2]))));
			} catch (NullPointerException np) {
				sfb.set(Collec.getDefaultGeomName(), (Geometry) gf.createPoint(new Coordinate(0, 0)));
			}
			sfb.set("nAdresse", nAdresse);
			sfb.set("adresse", adresse);
			sfb.set("typeVoie", typeVoie);
			sfb.set("codePos", codePos);
			sfb.set("amenite", amenite);
			sfb.set("codeAmenite", codeAmenite);
			sfb.set("name", denominationEtablissement);
			sfb.set("siret", siret);
			sfb.set("trancheEffectifsEtablissement", trancheEffectifsEtablissement);
			sfb.set("scoreGeocode", Double.valueOf(geocode[0]));
			sfb.set("rstOuv0314", resteOuvertArrete0314);
			sfb.set("rstOuv1030", resteOuvertArrete0314);
			return sfb.buildFeature(Attribute.makeUniqueId());
		} catch (IOException e) {
			e.printStackTrace();
			return sfb.buildFeature(Attribute.makeUniqueId());
		}
	}
}
