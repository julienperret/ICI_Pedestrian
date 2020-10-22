package insee;

import java.io.File;
import java.io.IOException;

import org.locationtech.jts.geom.Geometry;

public class SirenePOI extends SireneEntry {

	public SirenePOI() {
		super();
	}

	public SirenePOI(String nAdresse, String adresse, String typeVoie, String codePos, String codeAmenite, String nomenclature,
			String denominationUniteLegale, String siret, String trancheEffectifsUniteLegale) throws IOException {
		super(nAdresse, adresse, typeVoie, codePos, codeAmenite, nomenclature, denominationUniteLegale, siret, trancheEffectifsUniteLegale);
		makeClassement();
		// geocodeIGN(adresseInfos);
	}

	String[] classement = new String[4];
	Geometry p;

	public void makeClassement() throws IOException {
		switch (nomenclature) {
		case "NAFRev2":
			classement = SireneImport.classSIRENEEntryNAFRev2(codeAmenite, new File("/home/ubuntu/Documents/INRIA/donnees/POI/NAFRev2POI.csv"));
			amenite = classement[3];
			break;
		case "NAF1993":
			classement = SireneImport.classSIRENEEntryNAF1993(codeAmenite, false,
					new File("/home/ubuntu/Documents/INRIA/donnees/POI/NAF93-retravailCERTU.csv"));
			amenite = classement[3];
			break;
		case "NAFRev1":
			classement = SireneImport.classSIRENEEntryNAF1993(codeAmenite, true,
					new File("/home/ubuntu/Documents/INRIA/donnees/POI/NAF93-retravailCERTU.csv"));
			amenite = classement[3];
			break;
		case "null":
		case "NAP":
			classement = SireneImport.classSIRENEEntryNAP(codeAmenite, new File("/home/ubuntu/Documents/INRIA/donnees/POI/NAP-POI.csv"));
			amenite = classement[3];
			break;
		}
		if (classement == null || classement[0] == null || classement[0] == "")
			valid = false;
	}

	public String[] getCSVFirstLine() {
		String[] firstCol = { "id", "siret", "numAdresse", "typeRue", "adresse", "codPostal", "codeAmenity", "nomenclature", "type", "cat", "freq",
				"tranche Effectifs", "name" };
		return firstCol;
	}

	public String[] getLineForCSV() {
		if (!valid)
			return null;
		String[] line = { siret, nAdresse, typeVoie, adresse, codePos, codeAmenite, nomenclature, classement[0], classement[1], classement[2],
				trancheEffectifsEtablissement, denominationUniteLegale };
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
				&& line[8].equals(classement[1]) && line[0].equals(siret) && line[10].equals(trancheEffectifsEtablissement))
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
