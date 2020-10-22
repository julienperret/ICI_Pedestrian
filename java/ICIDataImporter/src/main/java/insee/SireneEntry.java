package insee;

import java.io.IOException;

public abstract class SireneEntry {

	String nAdresse, adresse, typeVoie, siret, codePos, codeAmenite, amenite, nomenclature, denominationUniteLegale, trancheEffectifsEtablissement;
	String[] completeAdress = new String[4];
	boolean valid = true;

	public SireneEntry() {
	}

	public SireneEntry(String nAdresse, String adresse, String typeVoie, String codePos, String codeAmenite, String nomenclature,
			String denominationUniteLegale, String siret, String trancheEffectifsEtablissement) throws IOException {
		this.nAdresse = nAdresse;
		completeAdress[0] = nAdresse;
		this.adresse = adresse;
		completeAdress[2] = adresse;
		this.typeVoie = typeVoie;
		completeAdress[1] = typeVoie;
		this.codePos = codePos;
		completeAdress[3] = codePos;
		this.codeAmenite = codeAmenite;
		this.nomenclature = nomenclature;
		this.denominationUniteLegale = denominationUniteLegale;
		this.siret = siret;
		this.trancheEffectifsEtablissement = transformEffectif(trancheEffectifsEtablissement);
	}

	public abstract String[] getLineForCSV();

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

	public boolean isValid() {
		return valid;
	}

	public abstract boolean equals(String[] line);

	public abstract String[] getCSVFirstLine();

}
