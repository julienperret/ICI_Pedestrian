package iciObjects;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import fr.ign.artiscales.tools.io.Csv;
import insee.SirenePOI;
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
    File commonNomenclatureFile;

    public MakeCommonNomenclature(File SireneNomenclature, File OsmNomenclature, File ApurNomenclature,
                                  File BPENomenclature, File outFile) throws IOException {
        makeCommonNomenclature(SireneNomenclature, OsmNomenclature, ApurNomenclature, BPENomenclature, outFile);
    }

    public MakeCommonNomenclature(File outFile) throws IOException {
        makeCommonNomenclature(SirenePOI.nomenclatureFile, OsmPOI.nomenclatureFile, ApurPOI.nomenclatureFile, BpePOI.nomenclatureFile, outFile);
    }

    public static void main(String[] args) throws IOException {
        //TODO wrong field indices : check them all
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
        long result = r.readAll().stream().filter(x -> x[iAttr] != null && !x[iAttr].equals("") && x[iAttr].length() != 0 && !x[iAttr].startsWith("SECTION ") && Character.isAlphabetic(x[iAttr].charAt(x[iAttr].length() - 1))).count();
        r.close();
        return result;
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

    public void makeCommonNomenclature(File SireneNomenclature, File OsmNomenclature, File ApurNomenclature,
                                       File BPENomenclature, File outFile) throws IOException {
        String[] fLineOut = {"NomenclatureICI", "codeNAF", "NameNAF", "freq", "NameAmenityOSM", "codeBPE", "NameBPE", "codeAPUR", "NameAPUR"};
        CSVWriter w = new CSVWriter(new FileWriter(outFile));
        w.writeNext(fLineOut);

        //Get indices (better here than in the code)
        int iOsmIndice = Csv.getIndice(OsmPOI.nomenclatureFile, "amenity");
        int iOsmIci = Csv.getIndice(OsmPOI.nomenclatureFile, "NomenclatureICI");
        int iBpeCode = Csv.getIndice(BpePOI.nomenclatureFile, "TYPEQU");
        int iBpeName = Csv.getIndice(BpePOI.nomenclatureFile, "Type");
        int iBpeIci = Csv.getIndice(BpePOI.nomenclatureFile, "NomenclatureICI");
        int iApurCode = Csv.getIndice(ApurPOI.nomenclatureFile, "Code activité BDCom 2017");
        int iApurName = Csv.getIndice(ApurPOI.nomenclatureFile, "Libellé activité BDCom 2017");
        int iApurIci = Csv.getIndice(ApurPOI.nomenclatureFile, "NomenclatureICI");
        int iNafICI = Csv.getIndice(SirenePOI.nomenclatureFile, "categorie");
        int iNafCode = Csv.getIndice(SirenePOI.nomenclatureFile, "Code");
        int iNafName = Csv.getIndice(SirenePOI.nomenclatureFile, "NomNAF2");
        int iNafFreq = Csv.getIndice(SirenePOI.nomenclatureFile, "frequence");

        // process to sorted and unsorted NAF codes
        CSVReader rNAF = new CSVReader(new FileReader(SireneNomenclature));
        rNAF.readNext();
        for (String[] naf : rNAF.readAll()) {
            String[] line = new String[9];
            line[0] = naf[iNafICI];
            line[1] = naf[iNafCode];
            line[2] = naf[iNafName];
            line[3] = naf[iNafFreq];
            // BPE
            line = checkIfCorrespondances(line, codeBPEUsed, BPENomenclature, naf[iNafName].toLowerCase(), iBpeName, iBpeCode, 6, 5, false);
            // APUR
            line = checkIfCorrespondances(line, codeAPURUsed, ApurNomenclature, naf[iNafName].toLowerCase(), iApurName, iApurCode, 8, 7, false);
            // OSM
            line = checkIfCorrespondances(line, codeOSMUsed, OsmNomenclature, naf[iNafName].toLowerCase(), iOsmIndice, iOsmIndice, 4, 4, true);
            w.writeNext(line);
        }

        // iterate over BPE's arguments
        CSVReader rBPE = new CSVReader(new FileReader(BPENomenclature));
        rBPE.readNext();
        for (String[] bpe : rBPE.readAll()) {
            String[] line = new String[9];
            if (!codeBPEUsed.contains(bpe[iBpeCode])) {
                line[0] = bpe[iBpeIci];
                line[5] = bpe[iBpeCode];
                line[6] = bpe[iBpeName];
                // APUR
                line = checkIfCorrespondances(line, codeAPURUsed, ApurNomenclature, bpe[iBpeName].toLowerCase(), Csv.getIndice(ApurPOI.nomenclatureFile, "Libellé activité BDCom 2017"), Csv.getIndice(ApurPOI.nomenclatureFile, "Code activité BDCom 2017"), 8, 7, false);
                // OSM
                line = checkIfCorrespondances(line, codeOSMUsed, OsmNomenclature, bpe[iBpeName].toLowerCase(), iOsmIndice, iOsmIndice, 4, 4, true);
                w.writeNext(line);
            }
        }

        // iterate over APUR's arguments
        CSVReader rAPUR = new CSVReader(new FileReader(ApurNomenclature));
        rAPUR.readNext();
        for (String[] apur : rAPUR.readAll()) {
            String[] line = new String[9];
            if (!codeAPURUsed.contains(apur[iApurCode])) {
                line[0] = apur[iApurIci];
                line[7] = apur[iApurCode];
                line[8] = apur[iApurName];
                // OSM
                line = checkIfCorrespondances(line, codeOSMUsed, OsmNomenclature, apur[iApurName].toLowerCase(), iOsmIndice, iOsmIndice, 4, 4, true);
                w.writeNext(line);
            }
        }
        // write rest of OSM Features
        CSVReader rOSM = new CSVReader(new FileReader(OsmNomenclature));
        rOSM.readNext();
        for (String[] osm : rOSM.readAll()) {
            String[] line = new String[9];
            String transOSM = translateOSMAmenityNames(osm[iOsmIndice]);
            if (!codeOSMUsed.contains(transOSM)) {
                line[0] = osm[iOsmIci];
                line[4] = transOSM;
                w.writeNext(line);
            }
        }
        w.close();
        this.commonNomenclatureFile = outFile;
    }

    public File getCommonNomenclatureFile() {
        return commonNomenclatureFile;
    }
}
