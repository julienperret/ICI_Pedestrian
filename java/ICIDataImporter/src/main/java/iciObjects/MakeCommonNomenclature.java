package iciObjects;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import util.Util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MakeCommonNomenclature {
	List<String> codeBPEUsed = new ArrayList<>();
	List<String> codeOSMUsed = new ArrayList<>();
	List<String> codeAPURUsed = new ArrayList<>();

	public static void main(String[] args) throws IOException {
		File rootFolder = Util.getRootFolder();
		File SireneNomenclature = new File("src/main/resources/NAFRev2POI.csv");
		File OsmNomenclature = new File(rootFolder, "OSM/nomenclatureOSM.csv");
		File ApurNomenclature = new File(rootFolder, "paris/APUR/APURNomenclature.csv");
		File BPENomenclature = new File(rootFolder, "INSEE/descriptif/BPE/BPE-varTYPEQU.csv");
		File outFile = new File(rootFolder, "ICI/nomenclatureCommune.csv");
		new MakeCommonNomenclature(SireneNomenclature, OsmNomenclature, ApurNomenclature, BPENomenclature, outFile);
		System.out.println(isSireneRealCategorie(new File("src/main/resources/NAFRev2POI.csv"), 0));
	}

	public static long isSireneRealCategorie(File f, int iAttr) throws IOException {
		CSVReader r = new CSVReader(new FileReader(f));
		r.readNext();
		long result = r.readAll().stream().filter(x -> (x[iAttr] == null || x[iAttr].equals("") || x[iAttr].length() == 0) ? false : !x[iAttr].startsWith("SECTION ") && Character.isAlphabetic(x[iAttr].charAt(x[iAttr].length() - 1))).count();
		r.close();
		return result;
	}

	public MakeCommonNomenclature(File SireneNomenclature, File OsmNomenclature, File ApurNomenclature,
			File BPENomenclature, File outFile) throws IOException {
		String[] fLineOut = { "NomenclatureICI", "codeNAF", "NameNAF", "freq", "NameAmenityOSM", "codeBPE", "NameBPE", "codeAPUR", "NameAPUR" };
		CSVWriter w = new CSVWriter(new FileWriter(outFile));
		w.writeNext(fLineOut);

		// process to sorted and unsorted NAF codes
		CSVReader rNAF = new CSVReader(new FileReader(SireneNomenclature));
		rNAF.readNext();
		for (String[] naf : rNAF.readAll()) {
			String[] line = new String[9];
			line[0] = naf[5];
			line[1] = naf[0];
			line[2] = naf[1];
			line[3] = naf[6];
			// BPE
			line = checkIfCorrespondances(line, codeBPEUsed, BPENomenclature, naf[1].toLowerCase(), 1, 6, 5);
			// APUR
			line = checkIfCorrespondances(line, codeAPURUsed, ApurNomenclature, naf[1].toLowerCase(), 2, 8, 7);
			// OSM
			line = checkIfCorrespondances(line, codeOSMUsed, OsmNomenclature, naf[1].toLowerCase(), 0, 0, 4, 4, true);
			w.writeNext(line);
		}

		// iterate over BPE's arguments
		CSVReader rBPE = new CSVReader(new FileReader(BPENomenclature));
		rBPE.readNext();
		for (String[] bpe : rBPE.readAll()) {
			String[] line = new String[9];
			if (!codeBPEUsed.contains(bpe[0])) {
				line[0] = bpe[2];
				line[5] = bpe[0];
				line[6] = bpe[1];
				// APUR
				line = checkIfCorrespondances(line, codeAPURUsed, ApurNomenclature, bpe[1].toLowerCase(), 2, 7, 8);
				// OSM
				line = checkIfCorrespondances(line, codeOSMUsed, OsmNomenclature, bpe[1].toLowerCase(), 0, 0, 4, 4, true);
				w.writeNext(line);
			}
		}

		// iterate over BPE's arguments
		CSVReader rAPUR = new CSVReader(new FileReader(ApurNomenclature));
		rAPUR.readNext();
		for (String[] apur : rAPUR.readAll()) {
			String[] line = new String[9];
			if (!codeAPURUsed.contains(apur[0])) {
				line[0] = apur[1];
				line[7] = apur[0];
				line[8] = apur[2];
				// OSM
				line = checkIfCorrespondances(line, codeOSMUsed, OsmNomenclature, apur[1].toLowerCase(), 0, 0, 4, 4, true);
				w.writeNext(line);
			}
		}
		// write rest of OSM Features
		CSVReader rOSM = new CSVReader(new FileReader(OsmNomenclature));
		rOSM.readNext();
		for (String[] osm : rOSM.readAll()) {
			String[] line = new String[9];
			String transOSM = translateOSMAmenityNames(osm[0]);
			if (!codeOSMUsed.contains(transOSM)) {
				line[0] = osm[1];
				line[4] = transOSM;
				w.writeNext(line);
			}
		}
		w.close();
	}

	private static String[] checkIfCorrespondances(String[] inputArray, List<String> usedTerms, File f, String nameNAF, int indiceNameComp,
												   int indiceNameOut, int indiceCodeOut) throws IOException {
		return checkIfCorrespondances(inputArray, usedTerms, f, nameNAF, indiceNameComp, 0, indiceNameOut, indiceCodeOut, false);
	}

	private static String[] checkIfCorrespondances(String[] inputArray, List<String> usedTerms, File f, String nameNAF, int indiceNameComp,
			int indiceCodeComp, int indiceNameOut, int indiceCodeOut, boolean translate) throws IOException {
		CSVReader rCorres = new CSVReader(new FileReader(f));
		rCorres.readNext();
		for (String[] c : rCorres.readAll()) {
			String nameComp = translate ? translateOSMAmenityNames(c[indiceNameComp]) : c[indiceNameComp].toLowerCase();
			if (((float) Util.LevenshteinDistance(nameNAF, nameComp) / nameComp.length()) < 0.2) { // || Util.jaro_distance(nameNAF, nameComp) > 0.85) {
				System.out.println(nameComp);
				System.out.println(nameNAF);
				System.out.println();
				usedTerms.add(c[indiceCodeComp]);
				inputArray[indiceCodeOut] = c[indiceCodeComp];
				inputArray[indiceNameOut] = nameComp;
			}
		}
		return inputArray;
	}

	public static String translateOSMAmenityNames(String enName) {
		switch (enName) {
		case "ferry_terminal":
			return "arrêt bateau";
		case "parking_entrance":
			return "entrée de parking";
		case "fountain":
			return "fontaine";
		case "post_office":
			return "bureau de poste";
		case "pub":
			return "bar";
		case "pharmacy":
			return "pharmacie";
		case "cafe":
			return "café";
		case "post_box":
			return "boîte aux lettres";
		case "toilets":
			return "toilettes";
		case "bicycle_rental":
			return "location de vélos";
		case "fuel":
			return "station service";
		case "library":
			return "bibliothèque";
		case "public_building":
			return "bâtiment public";
		case "recycling":
			return "recyclage";
		case "bank":
			return "banque";
		case "cinema":
			return "cinéma";
		case "atm":
			return "distributeur de billets";
		case "bicycle_parking":
			return "parking vélos";
		case "bench":
			return "banc";
		case "fast_food":
			return "restauration rapide";
		case "motorcycle_parking":
			return "parking motocycle";
		case "drinking_water":
		case "water_point":
		case "watering_place":
			return "fontaine potable";
		case "theatre":
			return "théatre";
		case "waste_basket":
			return "poubelle";
		case "ice_cream":
			return "glacerie";
		case "community_centre":
			return "centre communautaire";
		case "vending_machine":
			return "machine de vente";
		case "hospital":
			return "hôpital";
		case "nightclub":
			return "discothèque";
		case "university":
			return "université";
		case "shower":
			return "douche";
		case "marketplace":
			return "place de marché";
		case "dentist":
			return "dentiste";
		case "clinic":
			return "clinique";
		case "college":
			return "université";
		case "car_rental":
			return "location de véhicules automobiles";
		case "driving_school":
			return "enseignement de la conduite";
		case "school":
			return "école élémentaire";
		case "kindergarten":
			return "garderie";
		case "social_facility":
			return "accompagnement social";
		case "arts_centre":
			return "centre artistique";
		case "place_of_worship":
			return "lieu de culte";
		case "charging_station":
			return "station de rechargement";
		case "clock":
			return "horloge";
		case "ticket_validator":
			return "validation de tickets";
		case "photo_booth":
			return "photomaton";
		case "parking_space":
			return "espace de parking";
		case "childcare":
			return "soin pour la petite enfance";
		case "veterinary":
			return "vétérinaire";
		case "doctors":
			return "médecins";
		case "car_sharing":
			return "partage de voiture";
		case "music_school":
			return "école de musique";
		case "cafe;coworking_space":
			return "espace de coworking";
		case "bicycle_repair_station":
			return "réparation de vélos";
		case "barber":
			return "coiffeur";
		case "post_pickup":
			return "boîte postale";
		case "research_institute":
			return "institut de recherche";
		case "language_school":
			return "école de langue";
		case "townhall":
			return "mairie";
		}
		return enName;
	}
}
