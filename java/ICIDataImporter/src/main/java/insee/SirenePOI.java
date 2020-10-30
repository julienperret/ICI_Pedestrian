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

public class SirenePOI extends SireneEntry {

	public SirenePOI() {
		super();
	}

	public SirenePOI(String nAdresse, String adresse, String typeVoie, String codePos, String codeAmenite, String nomenclature,
			String denominationUniteLegale, String siret, String trancheEffectifsUniteLegale) throws IOException {
		super(nAdresse, adresse, typeVoie, codePos, codeAmenite, nomenclature, denominationUniteLegale, siret, trancheEffectifsUniteLegale);
		makeClassement();
	}

	String[] classement = new String[5];

	public void makeClassement() throws IOException {
		switch (nomenclature) {
		case "NAFRev2":
			classement = SireneImport.classSIRENEEntryNAFRev2(codeAmenite, new File("src/main/resources/NAFRev2POI.csv"));
			break;
		case "NAF1993":
			classement = SireneImport.classSIRENEEntryNAF1993(codeAmenite, false, new File("src/main/resources/NAF93-retravailCERTU.csv"));
			break;
		case "NAFRev1":
			classement = SireneImport.classSIRENEEntryNAF1993(codeAmenite, true, new File("src/main/resources/NAF93-retravailCERTU.csv"));
			break;
		case "null":
		case "NAP":
			classement = SireneImport.classSIRENEEntryNAP(codeAmenite, new File("src/main/resources/NAP-POI.csv"));
			break;
		}
		if (classement == null || classement[0] == null || classement[0] == "")
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
		sfTypeBuilder.add("codeAmenite", String.class);
		sfTypeBuilder.add("nomenclature", String.class);
		sfTypeBuilder.add("denominationUniteLegale", String.class);
		sfTypeBuilder.add("siret", String.class);
		sfTypeBuilder.add("trancheEffectifsEtablissement", String.class);
		sfTypeBuilder.add("type", String.class);
		sfTypeBuilder.add("categorie", String.class);
		sfTypeBuilder.add("frequence", String.class);
		sfTypeBuilder.add("intituleAm", String.class);
		sfTypeBuilder.add("scoreGeocode", Double.class);
		sfTypeBuilder.add("rstOuv1403", String.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}

	public SimpleFeature generateSimpleFeature() {
		String[] geocode;
		try {
			geocode = Geocode.geocodeAdresseDataGouv(completeAdress);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		SimpleFeatureBuilder sfb = getSireneSFB();
		sfb.set(Collec.getDefaultGeomName(), (Geometry) gf.createPoint(new Coordinate(Double.valueOf(geocode[1]), Double.valueOf(geocode[2]))));
		sfb.set("nAdresse", nAdresse);
		sfb.set("adresse", adresse);
		sfb.set("typeVoie", typeVoie);
		sfb.set("codePos", codePos);
		sfb.set("codeAmenite", codeAmenite);
		sfb.set("nomenclature", nomenclature);
		sfb.set("denominationUniteLegale", denominationEtablissement);
		sfb.set("siret", siret);
		sfb.set("trancheEffectifsEtablissement", trancheEffectifsEtablissement);
		sfb.set("type", classement[0]);
		sfb.set("categorie", classement[1]);
		sfb.set("frequence", classement[2]);
		sfb.set("intituleAm", classement[3]);
		sfb.set("scoreGeocode", Double.valueOf(geocode[0]));
		sfb.set("rstOuv1403", classement[4]);
		return sfb.buildFeature(Attribute.makeUniqueId());
	}

	public String[] getCSVFirstLine() {
		String[] firstCol = { "id", "siret", "numAdresse", "typeRue", "adresse", "codPostal", "codeAmenity", "intituleAmenity", "nomenclature",
				"type", "cat", "freq", "tranche Effectifs", "name", "reste ouvert selon arrete du 13 03" };
		return firstCol;
	}

	public String[] getLineForCSV() {
		if (!valid)
			return null;
		String[] line = { siret, nAdresse, typeVoie, adresse, codePos, codeAmenite, classement[3], nomenclature, classement[0], classement[1],
				classement[2], trancheEffectifsEtablissement, denominationEtablissement, classement[4] };
		return line;
	}

	public boolean equals(SirenePOI in) {
		if (in.getnAdresse().equals(nAdresse) && in.getTypeVoie().equals(typeVoie) && in.getAdresse().equals(adresse)
				&& in.getCodePos().equals(codePos) && in.getClassement()[1].equals(classement[1])
				&& in.getTrancheEffectifsUniteLegale().equals(trancheEffectifsEtablissement) && in.getSiren().equals(siret))
			return true;
		return false;
	}

	public boolean equals(String[] line) {
		if (line[1].equals(nAdresse) && line[2].equals(typeVoie) && line[3].equals(adresse) && line[4].equals(codePos)
				&& line[9].equals(classement[1]) && line[0].equals(siret) && line[11].equals(trancheEffectifsEtablissement))
			return true;
		return false;
	}

	public String getnAdresse() {
		return nAdresse;
	}

	public void setnAdresse(String nAdresse) {
		this.nAdresse = nAdresse;
	}

	public String getAdresse() {
		return adresse;
	}

	public void setAdresse(String adresse) {
		this.adresse = adresse;
	}

	public String getTypeVoie() {
		return typeVoie;
	}

	public void setTypeVoie(String typeVoie) {
		this.typeVoie = typeVoie;
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
