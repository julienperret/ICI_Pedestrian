package fr.ici.dataImporter.iciObjects;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import fr.ici.dataImporter.insee.SirenePOI;
import fr.ici.dataImporter.util.Util;
import fr.ign.artiscales.tools.io.Csv;

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
        File rootFolder = Util.getRootFolder();
        File SireneNomenclature = new File("src/main/resources/NAFRev2POI.csv");
        File OsmNomenclature = new File(rootFolder, "OSM/nomenclatureOSM.csv");
        File ApurNomenclature = new File(rootFolder, "paris/APUR/APURNomenclature.csv");
        File BPENomenclature = new File(rootFolder, "INSEE/descriptif/BPE/BPE-varTYPEQU.csv");
        File outFile = new File(rootFolder, "ICI/nomenclatureCommune.csv");
        new MakeCommonNomenclature(SireneNomenclature, OsmNomenclature, ApurNomenclature, BPENomenclature, outFile);
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
            case "alcohol":
            case "wine":
            case "beverages":
                return "Commerce de détail de boissons en magasin spécialisé";
            case "anime":
                return "manga-store";
            case "antiques":
                return "antiquités";
            case "art":
                return "galerie d'art";
            case "baby_goods":
                return "articles pour bébés";
            case "bakery":
            case "pastry":
                return "boulangerie - boulangerie pâtisserie";
            case "beauty":
                return "Soins de beauté";
            case "bathrooms":
                return "bains";
            case "bed":
                return "literie";
            case "bicycle":
                return "vélo";
            case "books":
                return "librairie";
            case "butcher":
                return "boucherie";
            case "car":
            case "car_repair":
                return "garagiste";
            case "carpenter":
                return "Menuisier, charpentier, serrurier";
            case "carpet":
                return "Commerce de détail de tapis - moquettes et revêtements de murs et de sols en magasin spécialisé";
            case "caterer":
                return "traiteur";
            case "cheese":
                return "fromagerie";
            case "chocolate":
                return "Chocolaterie - Confiserie";
            case "clothes":
                return "Magasin de vêtements";
            case "coffee":
                return "café";
            case "coffeemaker":
            case "tea":
                return "Torréfacteur - Commerce détail thé et café";
            case "computer":
                return "informatique";
            case "confectionery":
                return "Chocolaterie - Confiserie";
            case "convenience":
                return "épicerie";
            case "copyshop":
                return "imprimerie";
            case "deli":
                return "Chocolaterie - Confiserie";
            case "diplomatic":
                return "ambassade";
            case "doityourself":
                return "recyclerie";
            case "dressmaker":
                return "tailleur";
            case "dry_cleaning":
            case "laundry":
                return "blanchisserie";
            case "E-cigarette":
                return "Vente de cigarettes électroniques";
            case "educational_institution":
                return "institution éducative";
            case "electrical":
            case "electrician":
                return "Électricien";
            case "employment_agency":
                return "agence d'emploi";
            case "estate_agent":
                return "agence immobilière";
            case "fashion_accessories":
                return "Bijouterie fantaisie - Accessoire de mode";
            case "fitness_station":
                return "musculation";
            case "florist":
                return "fleuriste";
            case "frozen_food":
                return "Produits surgelés";
            case "furniture":
                return "Papeterie - Fournitures de bureau";
            case "games":
            case "toys":
                return "Vente de jouets et jeux";
            case "gift":
            case "variety_store":
                return "Bimbeloterie - Articles souvenirs";
            case "gold":
                return "Achat - Vente d'or";
            case "government":
                return "Bâtiment gouvernemental";
            case "greengrocer":
                return "Vente de fruits et légumes";
            case "hairdresser":
                return "Coiffeur";
            case "handicraft":
            case "craft":
                return "Artisanat";
            case "hifi":
            case "video":
            case "hardware":
                return "Magasin d'électroménager,  audio vidéo";
            case "hearing_aids":
                return "Vente de prothèses auditives";
            case "hostel":
            case "hotel":
            case "guest_house":
                return "Hôtels et hébergement similaire";
            case "insurance":
                return "assurance";
            case "interior_decoration":
                return "décoration d'intérieur";
            case "jewelry":
                return "Horlogerie Bijouterie";
            case "newsagent":
            case "kiosk":
                return "Kiosque à journaux";
            case "kitchen":
            case "tableware":
            case "houseware":
                return "Vente de meubles de cuisines et salle de bain";
            case "lamps":
            case "lighting":
                return "Vente de luminaires";
            case "locksmith":
                return "serrurerie";
            case "luthier":
                return "luthier";
            case "massage":
                return "salon de massage";
            case "mobile_phone":
                return "téléphonie";
            case "motorcycle":
                return "Commerce et réparation de motocycles";
            case "moving_company":
                return "Déménagement / Garde meuble";
            case "olive_oil":
                return "huile d'olive";
            case "optician":
                return "opticien";
            case "outdoor":
                return "exterieur";
            case "party":
                return "fête";
            case "perfumery":
                return "parfumerie";
            case "pet":
                return "Animalerie";
            case "pitch":
                return "terrain";
            case "plumber":
                return "plomberie";
            case "scuba_diving":
                return "plongée sous-marine";
            case "seafood":
                return "poissonnerie";
            case "shoemaker":
                return "Cordonnerie";
            case "shoes":
                return "Magasin de chaussures";
            case "sports_centre":
            case "sports":
                return "Centre sprotif";
            case "stationery":
                return "Papéterie";
            case "supermarket":
                return "supermarché";
            case "tailor":
                return "tailleur";
            case "tattoo":
                return "Tatouage - Piercing";
            case "ticket":
                return "Ticket de transport";
            case "tiler":
                return "Couvreur";
            case "tobacco":
                return "Tabac";
            case "toilettes_publiques":
                return "toilettes_publiques";
            case "travel_agency":
            case "travel_agent":
                return "Agence de voyage";
            case "playground":
                return "terrain de jeu";
            case "boutique":
                return "boutique";
            case"household_linen":
            return "Magasin de revêtements murs et sols";
            case"painter":
                return "Plâtrier peintre";
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
