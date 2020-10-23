package insee;

import java.io.File;
import java.io.IOException;

public class SireneWorkingPlace extends SireneEntry {
	public SireneWorkingPlace() {
		super();
	}

	public SireneWorkingPlace(String nAdresse, String adresse, String typeVoie, String codePos, String codeAmenite, String nomenclature,
			String denominationUniteLegale, String siret, String trancheEffectifsUniteLegale) throws IOException {
		super(nAdresse, adresse, typeVoie, codePos, codeAmenite, nomenclature, denominationUniteLegale, siret, trancheEffectifsUniteLegale);
		makeClassement();
		// geocodeIGN(adresseInfos);
	}

	public void makeClassement() throws IOException {
		switch (nomenclature) {
		case "NAFRev2":
			amenite = SireneImport.classSIRENEEntryNAFRev2(codeAmenite, new File("src/main/ressources/NAFRev2.csv"))[3];
			break;
		case "NAF1993":
			amenite = SireneImport.classSIRENEEntryNAF1993(codeAmenite, false, new File("src/main/ressources/NAF93and03.csv"))[3];
			break;
		case "NAFRev1":
			amenite = SireneImport.classSIRENEEntryNAF1993(codeAmenite, true, new File("src/main/ressources/NAF93and03.csv"))[3];
			break;
		case "null":
		case "NAP":
			amenite = SireneImport.classSIRENEEntryNAP(codeAmenite, new File("src/main/ressources/NAP.csv"))[3];
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
		String[] line = { siret, nAdresse, typeVoie, adresse, codePos, amenite, codeAmenite, nomenclature, denominationUniteLegale,
				trancheEffectifsEtablissement };
		return line;
	}

	public String[] getCSVFirstLine() {
		String[] firstCol = { "id", "siret", "numAdresse", "typeRue", "adresse", "codPostal", "amenité", "codeAmenity", "nomenclature", "name",
				"tranche Effectifs" };
		return firstCol;
	}


}
