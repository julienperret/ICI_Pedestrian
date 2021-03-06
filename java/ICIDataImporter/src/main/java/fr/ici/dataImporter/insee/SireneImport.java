package fr.ici.dataImporter.insee;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.opencsv.CSVReader;
import fr.ici.dataImporter.iciObjects.WorkingPlace;
import fr.ici.dataImporter.util.Util;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.io.Csv;
import fr.ign.artiscales.tools.io.Json;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.geotools.data.DataStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.opengis.geometry.MismatchedDimensionException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SireneImport {
    static boolean append = false;
    static Timer timer = new Timer();

    public SireneImport() {
    }

    public static void main(String[] args)
            throws IOException, MismatchedDimensionException {
        File rootFolder = Util.getRootFolder();

        ////////////////
        // Step 1: get the SIRENE infos with the API. Timer not synchronized so we need to wait for the be finished (prompt) before starting step2
        ////////////////
// TODO not working anymore, find out why
//		 (new SireneImport()).getSIRENEData(new File(rootFolder, "POI/sireneAPIOut/"));
//		System.out.println((new File(rootFolder, "POI/sireneAPIOut/")).getAbsoluteFile());
//		parseSireneEntry(new File(rootFolder, "POI/sireneAPIOut/sirene" + 0 + ".json"), new File(rootFolder,"POI/"), "POI");

        ////////////////
        // Step 2: Apply the pretreatments to the generated files. Put the total number of files on this next variable (last file should not contain anything)
        ////////////////
        int nbFile = 42;
		for (int i = 0; i <= nbFile; i++) {
			parseSireneEntry(new File(rootFolder, "POI/sireneAPIOut/sirene" + i + ".json"), new File(rootFolder, "ICI/"), "WorkingPlace");
			System.out.println("done : file" + i);
		}
        append = false;
        System.out.println("done working places");
//        for (int i = 0; i <= nbFile; i++) {
//            parseSireneEntry(new File(rootFolder, "POI/sireneAPIOut/sirene" + i + ".json"), new File(rootFolder, "POI/"), "POI");
//            System.out.println("done : file" + i);
//        }
    }

    public static String getSIRENEData(String curseur, File outFile) throws IOException, URISyntaxException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        URI uri = new URIBuilder().setScheme("https").setHost("api.fr.ici.dataImporter.insee.fr").setPath("/entreprises/sirene/V3/siret")
                .setParameter("q", "codePostalEtablissement:75005 AND etatAdministratifUniteLegale:A").setParameter("nombre", "10000")
                .setParameter("curseur", curseur).build();
        HttpPost httppost = new HttpPost(uri);
        httppost.addHeader("Accept", "application/json");
        httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");
        httppost.addHeader("Authorization", "Bearer " + Util.getToken("fr.ici.dataImporter.insee:SIRENE"));
        try (CloseableHttpResponse response = httpclient.execute(httppost)) {
            // System.out.println(response.getStatusLine().getStatusCode());
            InputStream stream = response.getEntity().getContent();
            java.nio.file.Files.copy(stream, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            stream.close();
        }
        return getNextCursorJson(outFile);
    }

    /**
     * Get the next cursor to download multiple parts of the data with the SIRENE API (see <a href
     * ="https://api.insee.fr/catalogue/site/themes/wso2/subthemes/insee/templates/api/documentation/download.jag?tenant=carbon.super&resourceUrl=/registry/resource/_system/governance/apimgt/applicationdata/provider/insee/Sirene/V3/documentation/files/INSEE%20Documentation%20API%20Sirene%20Services-V3.9.pdf">doc</a>
     * p36/37)
     *
     * @param f the answered json file
     * @return the next cursor value
     * @throws JsonParseException
     * @throws IOException
     */
    public static String getNextCursorJson(File f) throws IOException {
        return Json.getHeaderJson(f).get("curseurSuivant");
    }

    public static List<SireneEntry> parseSireneEntry(File jSON, File folderOut, String entryType) throws IOException {
        List<SireneEntry> lSireneEntry = new ArrayList<>();
        // Logger logger = Logging.getLogger("org.geotools.feature.DefaultFeatureCollection");
        // logger.setLevel(Level.SEVERE);
        DefaultFeatureCollection result = new DefaultFeatureCollection();
//		JsonFactory factory = new JsonFactory(CharacterEscapes.ESCAPE_NONE());
        JsonFactory factory = new JsonFactory();
//		factory.setCharacterEscapes();
        JsonParser parser = factory.createParser(jSON);
        JsonToken token;
        String nAdresse = "", adresse = "", typeVoie = "", codePos = "", codeAmeniteEtablissement = "", codeAmeniteUniteLegale = "",
                nomenclature = "", denominationUniteLegale = "", denominationUsuelle1UniteLegale = "", siret = "", trancheEffectifsEtablissement = "",
                etatAdministratifEtablissement = "", dateFin = "", nomenclatureActivitePrincipaleUniteLegale = "";
        int count = 0;
        boolean arrayStarted = false;
        String[] fline;
        if ("POI".equals(entryType)) {
            fline = (new SirenePOI()).getCSVFirstLine();
        } else { // Working place
            fline = (new WorkingPlace()).getCSVFirstLine();
        }
        HashMap<String, String[]> out = new HashMap<>();
        while (!parser.isClosed()) {
            token = parser.nextToken();
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("numeroVoieEtablissement")) {
                token = parser.nextToken();
                nAdresse = parser.getText();
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("typeVoieEtablissement")) {
                token = parser.nextToken();
                typeVoie = parser.getText();
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("libelleVoieEtablissement")) {
                token = parser.nextToken();
                adresse = parser.getText();
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("codePostalEtablissement")) {
                token = parser.nextToken();
                codePos = parser.getText();
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("nomenclatureActivitePrincipaleEtablissement")) {
                token = parser.nextToken();
                nomenclature = parser.getText();
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("siret")) {
                token = parser.nextToken();
                siret = parser.getText();
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("denominationUniteLegale")) {
                token = parser.nextToken();
                denominationUniteLegale = parser.getText().replace(",", " -");
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("denominationUsuelle1UniteLegale")) {
                token = parser.nextToken();
                denominationUsuelle1UniteLegale = parser.getText().replace(",", " -");
            }
            // get the activity number
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("activitePrincipaleEtablissement")) {
                token = parser.nextToken();
                codeAmeniteEtablissement = parser.getText();
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("activitePrincipaleUniteLegale")) {
                token = parser.nextToken();
                codeAmeniteUniteLegale = parser.getText();
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("nomenclatureActivitePrincipaleUniteLegale")) {
                token = parser.nextToken();
                nomenclatureActivitePrincipaleUniteLegale = parser.getText();
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("trancheEffectifsEtablissement")) {
                token = parser.nextToken();
                trancheEffectifsEtablissement = parser.getText();
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("etatAdministratifEtablissement")) {
                token = parser.nextToken();
                etatAdministratifEtablissement = parser.getText();
            }
            if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("dateFin")) {
                token = parser.nextToken();
                dateFin = parser.getText();
            }
            if (token == JsonToken.START_ARRAY && !parser.getCurrentName().equals("etablissements")) {
                arrayStarted = true;
            }
            if (token == JsonToken.END_OBJECT && arrayStarted) {
                if (codeAmeniteEtablissement.equals("")) {
                    codeAmeniteEtablissement = codeAmeniteUniteLegale;
                    nomenclature = nomenclatureActivitePrincipaleUniteLegale;
                }
                if (denominationUniteLegale.equals("") && !denominationUsuelle1UniteLegale.equals(""))
                    denominationUniteLegale = denominationUsuelle1UniteLegale;
                SireneEntry entry;
                if (SireneEntry.isActive(etatAdministratifEtablissement) && (dateFin.equals("") || dateFin.equals("null"))) {
                    if ("POI".equals(entryType))
                        entry = new SirenePOI(nAdresse, adresse, typeVoie, codePos, codeAmeniteEtablissement, nomenclature, denominationUniteLegale,
                                siret, trancheEffectifsEtablissement);
                    else  //Working place
                        entry = new WorkingPlace(nAdresse, adresse, typeVoie, codePos, codeAmeniteEtablissement, nomenclature,
                                denominationUniteLegale, siret, trancheEffectifsEtablissement);
                    if (entry.isValid()) {
                        // Geopackage export
                        result.add(entry.generateSimpleFeature());
                        // add in list
                        lSireneEntry.add(entry);
                        // CSV export
                        String[] l = entry.getLineForCSV();
                        out.put(siret + "-" + count++, l);
                    }
                }
            }
            if (token == JsonToken.END_ARRAY && arrayStarted) {
                // flush
                nAdresse = adresse = typeVoie = codePos = codeAmeniteEtablissement = codeAmeniteUniteLegale = nomenclature = nomenclatureActivitePrincipaleUniteLegale = denominationUniteLegale = siret = trancheEffectifsEtablissement = etatAdministratifEtablissement = dateFin = "";
                arrayStarted = false;
            }
        }
        // CSV export
        Csv.generateCsvFile(out, folderOut, "SIRENE-" + entryType + "-treated", append, fline);

        // GPKG export
        File geomFile = new File(folderOut, "SIRENE-" + entryType + ".gpkg");
        if (geomFile.exists() && append) {
            DataStore ds = CollecMgmt.getDataStore(geomFile);
            result.addAll(ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures());
            ds.dispose();
        }
        if (!append)
            append = true;
        CollecMgmt.exportSFC(result, geomFile);
        return lSireneEntry;
    }

    public static String[] classSIRENEEntryNAF1993(String amen, boolean revised, File modele) throws IOException {
        CSVReader listCERTU = new CSVReader(new FileReader(modele));
        String[] classement = new String[4];
        int nomenclature = 0;
        amen = amen.replace(".", "");
        if (revised)
            nomenclature = 1;
        for (String[] line : listCERTU.readAll())
            if (amen.equals(line[nomenclature])) {
                try {
                    classement[0] = line[3];
                    classement[1] = line[4];
                    classement[2] = line[5];
                } catch (Exception ignored) {
                }
                classement[3] = line[2];
            }
        listCERTU.close();
        return classement;
    }

    public static String[] classSIRENEEntryNAP(String codeAmenite, File modele) throws IOException {
        CSVReader listCERTU = new CSVReader(new FileReader(modele));
        String[] classement = new String[4];
        // codeAmenite = codeAmenite.replace(".", "");
        for (String[] line : listCERTU.readAll())
            if (codeAmenite.equals(line[3])) {
                try {
                    classement[0] = line[6];
                    classement[1] = line[7];
                    classement[2] = line[8];
                } catch (Exception ignored) {
                }
                classement[3] = line[4];
            }
        listCERTU.close();
        return classement;
    }

    public static String[] classSIRENEEntryNAFRev2(String codeAmenite, File modele) throws IOException {
        CSVReader listNavRef2 = new CSVReader(new FileReader(modele));
        String[] classement = new String[6];
        // codeAmenite = codeAmenite.replace(".", "");
        for (String[] line : listNavRef2.readAll())
            if (codeAmenite.equals(line[0])) {
                try {
                    classement[0] = line[4];
                    classement[1] = line[5];
                    classement[2] = line[6];

                } catch (Exception ignored) {
                }
                classement[3] = line[1];
                if (line[2].equals("1"))
                    classement[4] = "true";
                if (line[3].equals("1"))
                    classement[5] = "true";
            }
        listNavRef2.close();
        return classement;
    }

    public void getSIRENEData(File outFolder) {
        String initialToken = "*";
        TemporizeSIRENECursoredData tSI = new TemporizeSIRENECursoredData(outFolder, initialToken);
        timer.scheduleAtFixedRate(tSI, Calendar.getInstance().getTime(), 120000);
        // TODO fix that timer/Thread issue if we want to sync everything
        // if (finish)
        // return tSI.getPastNumbers();
        // System.out.println("too early");
        // return 0;
    }

    static class TemporizeSIRENECursoredData extends TimerTask {
        String lastCursor, preLastCursor, iniCursor;
        File outFolder;
        int pastNumbers = 0;

        TemporizeSIRENECursoredData(File outFolder, String iniCursor) {
            this.outFolder = outFolder;
            this.iniCursor = iniCursor;
        }

        public boolean isFinished() {
            if (lastCursor.equals(preLastCursor)) {
                timer.cancel();
                System.out.println("finish time task");
                return true;
            }
            return false;
        }

        public String getLastCursor() {
            return lastCursor;
        }

        @Override
        public void run() {
            int i = 0;
            try {
                iniCursor = SireneImport.getSIRENEData(iniCursor, new File(outFolder, "sirene" + (i++ + pastNumbers) + ".json"));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                System.out.println("If no cursor found, maybe everything's in the first request");
                return;
            }
            String cursor = iniCursor;
            do {
                iniCursor = cursor;
                try {
                    cursor = SireneImport.getSIRENEData(iniCursor, new File(outFolder, "sirene" + (i++ + pastNumbers) + ".json"));
                } catch (IOException | URISyntaxException e) {
                    e.printStackTrace();
                    System.out.println("If no cursor found, maybe everything's in the first request");
                    return;
                }
            } while (!iniCursor.equals(cursor) && i < 30);
            pastNumbers = pastNumbers + i;
            lastCursor = cursor;
            preLastCursor = iniCursor;
            iniCursor = cursor;
            this.isFinished();
        }

        public int getPastNumbers() {
            return pastNumbers;
        }
    }
}