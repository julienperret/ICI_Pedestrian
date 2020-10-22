package insee;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

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
			amenite = SireneImport.classSIRENEEntryNAFRev2(codeAmenite, new File("/home/ubuntu/Documents/INRIA/donnees/POI/NAFRev2.csv"))[3];
			break;
		case "NAF1993":
			amenite = SireneImport.classSIRENEEntryNAF1993(codeAmenite, false,
					new File("/home/ubuntu/Documents/INRIA/donnees/POI/NAF93and03.csv"))[3];
			break;
		case "NAFRev1":
			amenite = SireneImport.classSIRENEEntryNAF1993(codeAmenite, true, new File("/home/ubuntu/Documents/INRIA/donnees/POI/NAF93and03.csv"))[3];
			break;
		case "null":
		case "NAP":
			amenite = SireneImport.classSIRENEEntryNAP(codeAmenite, new File("/home/ubuntu/Documents/INRIA/donnees/POI/NAP.csv"))[3];
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

	public static void mergeNAF93andNAF03(File NAF93, File NAF03, File outFolder) throws IOException {
		CSVReader n93 = new CSVReader(new FileReader(NAF93));
		CSVWriter w = new CSVWriter(new FileWriter(new File(outFolder, "NAF93and03.csv")));
		String[] fLine = { "NAF 1993", "NAF 2003", "Intitulé - Niveau 700" };
		w.writeNext(fLine);
		loop93: for (String[] line93 : n93.readAll()) {
			CSVReader n03 = new CSVReader(new FileReader(NAF03));
			String[] line = new String[3];
			for (String[] line03 : n03.readAll()) {
				if (line93[0].equals(line03[0])) {
					line[0] = line93[0];
					line[1] = line93[0];
					line[2] = line93[1];
					w.writeNext(line);
					continue loop93;
				}
			}
			line[0] = line93[0];
			line[1] = "";
			line[2] = line93[1];
			w.writeNext(line);
			n03.close();

		}
		n93.close();

		CSVReader n03 = new CSVReader(new FileReader(NAF03));
		loop03: for (String[] line03 : n03.readAll()) {
			n93 = new CSVReader(new FileReader(NAF93));
			String[] line = new String[3];
			for (String[] line93 : n93.readAll())
				if (line93[0].equals(line03[0]))
					continue loop03;
			line[0] = "";
			line[1] = line03[0];
			line[2] = line03[1];
			w.writeNext(line);
			n93.close();
		}
		n03.close();
		w.close();
	}
	/// **
	// * from https://stackoverflow.com/questions/2344320/comparing-strings-with-tolerance
	// * @param source
	// * @param target
	// * @return
	// */
	// public static int LevenshteinDistance(String source, String target)
	// {
	// // degenerate cases
	// if (source == target) return 0;
	// if (source.length() == 0) return target.length();
	// if (target.length() == 0) return source.length();
	//
	// // create two work vectors of integer distances
	// int[] v0 = new int[target.length() + 1];
	// int[] v1 = new int[target.length() + 1];
	//
	// // initialize v0 (the previous row of distances)
	// // this row is A[0][i]: edit distance for an empty s
	// // the distance is just the number of characters to delete from t
	// for (int i = 0; i < v0.length; i++)
	// v0[i] = i;
	//
	// for (int i = 0; i < source.length(); i++)
	// {
	// // calculate v1 (current row distances) from the previous row v0
	//
	// // first element of v1 is A[i+1][0]
	// // edit distance is delete (i+1) chars from s to match empty t
	// v1[0] = i + 1;
	//
	// // use formula to fill in the rest of the row
	// for (int j = 0; j < target.length(); j++)
	// {
	// var cost = (source.charAt(i) == target.charAt(j)) ? 0 : 1;
	// v1[j + 1] = Math.Min(v1[j] + 1, Math.Min(v0[j + 1] + 1, v0[j] + cost));
	// }
	//
	// // copy v1 (current row) to v0 (previous row) for next iteration
	// for (int j = 0; j < v0.length; j++)
	// v0[j] = v1[j];
	// }
	//
	// return v1[target.length];
	// }
}
