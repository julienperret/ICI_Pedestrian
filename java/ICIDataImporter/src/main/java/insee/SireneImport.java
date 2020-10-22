package insee;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.operation.TransformException;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import au.com.bytecode.opencsv.CSVReader;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import fr.ign.artiscales.tools.io.Csv;
import fr.ign.artiscales.tools.io.Json;

public class SireneImport {
	public static void main(String[] args)
			throws IOException, URISyntaxException, MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException, TransformException {
		int nbFile = getSIRENEMultipleData(new File("/tmp/"));
		for (int i = 0; i <= nbFile; i++)
			parseSireneEntry(new File("/tmp/sirene" + i + ".json"), new File("/home/ubuntu/Documents/INRIA/donnees/POI/"), "WorkingPlace");
		append = false;
		for (int i = 0; i <= nbFile; i++)
			parseSireneEntry(new File("/tmp/sirene" + i + ".json"), new File("/home/ubuntu/Documents/INRIA/donnees/POI/"), "POI");
		// MakePointOutOfGeocode(new File("/tmp/geocodage.ign.fr.json"));
		// convertJSONtoCSV(new File("/home/ubuntu/Documents/INRIA/donnees/POI/sirene.json"), new File("/home/ubuntu/Documents/INRIA/donnees/POI/sirene.csv"));
	}

	static boolean append = false;

	public static int getSIRENEMultipleData(File outFolder) throws IOException, URISyntaxException {
		int i = 0;
		String iniCursor = getSIRENEData("*", new File(outFolder, "sirene" + i++ + ".json"));
		String cursor = iniCursor;
		do {
			iniCursor = cursor;
			cursor = getSIRENEData(iniCursor, new File(outFolder, "sirene" + i++ + ".json"));
		} while (!iniCursor.equals(cursor));
		return i;
	}

	public static String getSIRENEData(String curseur, File outFile) throws IOException, URISyntaxException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		URI uri = new URIBuilder().setScheme("https").setHost("api.insee.fr").setPath("/entreprises/sirene/V3/siret")
				.setParameter("q", "codePostalEtablissement:75005 AND etatAdministratifUniteLegale:A").setParameter("nombre", "10000")
				.setParameter("curseur", curseur).build();
		HttpPost httppost = new HttpPost(uri);
		httppost.addHeader("Accept", "application/json");
		httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httppost.addHeader("Authorization", "Bearer a2887e94-5cbe-3e9d-94da-93af59eacfcf");
		CloseableHttpResponse response = httpclient.execute(httppost);
		try {
			System.out.println(response.getStatusLine().getStatusCode());
			InputStream stream = response.getEntity().getContent();
			java.nio.file.Files.copy(stream, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			stream.close();
		} finally {
			response.close();
		}

		// this works on a command line : curl -X POST --header 'Content-Type: application/x-www-form-urlencoded' --header 'Accept: application/json' --header 'Authorization:
		// Bearer a2887e94-5cbe-3e9d-94da-93af59eacfcf' -d 'q=etatAdministratifUniteLegale%3AA%20AND%20codePostalEtablissement%3A75005&nombre=100000'
		// 'https://api.insee.fr/entreprises/sirene/V3/siret'
		// this code should work too - returns a 404...
		return getNextCursorJson(outFile);
	}

	/**
	 * Get the next cursor to download multiple parts of the data with the SIRENE API (see <a href
	 * ="https://api.insee.fr/catalogue/site/themes/wso2/subthemes/insee/templates/api/documentation/download.jag?tenant=carbon.super&resourceUrl=/registry/resource/_system/governance/apimgt/applicationdata/provider/insee/Sirene/V3/documentation/files/INSEE%20Documentation%20API%20Sirene%20Services-V3.9.pdf">doc</a>
	 * p36/37)
	 * 
	 * @param f
	 *            the answered json file
	 * @return the next cursor value
	 * @throws JsonParseException
	 * @throws IOException
	 */
	public static String getNextCursorJson(File f) throws JsonParseException, IOException {
		return Json.getHeaderJson(f).get("curseurSuivant");
	}

	public static void parseSireneEntry(File jSON, File folderOut, String entryType) throws IOException, URISyntaxException {
		JsonFactory factory = new JsonFactory();
		JsonParser parser = factory.createParser(jSON);
		JsonToken token = parser.nextToken();
		String nAdresse = "", adresse = "", typeVoie = "", codePos = "", codeAmeniteEtablissement = "", codeAmeniteUniteLegale = "",
				nomenclature = "", denominationEtablissement = "", siret = "", trancheEffectifsEtablissement = "",
				etatAdministratifEtablissement = "", dateFin = "", nomenclatureActivitePrincipaleUniteLegale = "";
		int count = 0;
		boolean arrayStarted = false;
		String[] fline;
		switch (entryType) {
		case "POI":
			fline = (new SirenePOI()).getCSVFirstLine();
			break;
		default: // Working Place
			fline = (new SireneWorkingPlace()).getCSVFirstLine();
			break;
		}
		HashMap<String, String[]> out = new HashMap<String, String[]>();
		while (!parser.isClosed()) {
			// if (token == JsonToken.START_OBJECT) {
			// System.out.println("new_OBJECT: " + parser.getCurrentName());
			// }
			// if (token == JsonToken.START_ARRAY) {
			// System.out.println("new_ARRAY: " + parser.getCurrentName());
			// }
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
			if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("denominationEtablissement")) {
				token = parser.nextToken();
				denominationEtablissement = parser.getText().replace(",", " -");
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
			// if (token == JsonToken.END_ARRAY) {
			// System.out.println("END_ARRAY: " + parser.getCurrentName());
			// }
			if (token == JsonToken.START_ARRAY && !parser.getCurrentName().equals("etablissements")) {
				arrayStarted = true;
			}
			if (token == JsonToken.END_OBJECT && arrayStarted) {
				// System.out.println("END_OBJECT: " + parser.getCurrentName());
				// if no code for the etablissement has been found, we get the one from the unite legale
				if (codeAmeniteEtablissement == "") {
					codeAmeniteEtablissement = codeAmeniteUniteLegale;
					nomenclature = nomenclatureActivitePrincipaleUniteLegale;
				}
				SireneEntry entry;
				switch (entryType) {
				case "POI":
					entry = new SirenePOI(nAdresse, adresse, typeVoie, codePos, codeAmeniteEtablissement, nomenclature, denominationEtablissement,
							siret, trancheEffectifsEtablissement);
					break;
				default: // Working Place
					entry = new SireneWorkingPlace(nAdresse, adresse, typeVoie, codePos, codeAmeniteEtablissement, nomenclature,
							denominationEtablissement, siret, trancheEffectifsEtablissement);
					break;
				}
				if (SireneEntry.isActive(etatAdministratifEtablissement) && (dateFin.equals("") || dateFin.equals("null")))
					if (entry.isValid()) {
						boolean add = true;
						String[] l = entry.getLineForCSV();
						for (String[] val : out.values())
							if (entry.equals(val)) {
								add = false;
								break;
							}
						if (add)
							out.put(siret + "-" + count++, l);
					}
			}
			if (token == JsonToken.END_ARRAY && arrayStarted) {
				// flush
				nAdresse = adresse = typeVoie = codePos = codeAmeniteEtablissement = codeAmeniteUniteLegale = nomenclature = nomenclatureActivitePrincipaleUniteLegale = denominationEtablissement = siret = trancheEffectifsEtablissement = etatAdministratifEtablissement = dateFin = "";
				arrayStarted = false;
			}
		}
		Csv.generateCsvFile(out, folderOut, "SIRENE-" + entryType + "-treated", append, fline);
		if (!append)
			append = true;
	}

	/**
	 * 
	 * @param jsonFile
	 * @throws IOException
	 * @throws NoSuchAuthorityCodeException
	 * @throws FactoryException
	 */
	public static void MakePointOutOfGeocode(File jsonFile) throws IOException, NoSuchAuthorityCodeException, FactoryException {
		JsonFactory factory = new JsonFactory();
		JsonParser parser = factory.createParser(jsonFile);
		JsonToken token = parser.nextToken();
		int i = 0;
		while (!parser.isClosed()) {
			token = parser.nextToken();
			// tab
			if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("geometry")) {
				token = parser.nextToken();
				double x = 0;
				double y = 0;
				while (token != JsonToken.END_OBJECT) {
					token = parser.nextToken();
					if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("coordinates")) {
						token = parser.nextToken();
						token = parser.nextToken();
						x = Double.parseDouble(parser.getText());
						token = parser.nextToken();
						y = Double.parseDouble(parser.getText());
					}
				}
				GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
				Point p = gf.createPoint(new Coordinate(x, y));

				// FIXME doesn't work - point is not where it should be and i don't see why. Input is rightly in WCS 84 (4326) ?! Export should arrive in Lambert93
				System.out.println(p);
				Geometry ptLambert;
				try {
					ptLambert = JTS.transform(p, CRS.findMathTransform(CRS.decode("EPSG:4326"), CRS.decode("EPSG:2154")));
				} catch (MismatchedDimensionException | TransformException | FactoryException e) {
					ptLambert = null;
					e.printStackTrace();
				}
				System.out.println(ptLambert);
				Geom.exportGeom(ptLambert, new File("/tmp/out" + i++));
			}
		}
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
				} catch (Exception e) {
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
				} catch (Exception e) {
				}
				classement[3] = line[4];
			}
		listCERTU.close();
		return classement;
	}

	public static String[] classSIRENEEntryNAFRev2(String codeAmenite, File modele) throws IOException {
		CSVReader listCERTU = new CSVReader(new FileReader(modele));
		String[] classement = new String[4];
		// codeAmenite = codeAmenite.replace(".", "");
		for (String[] line : listCERTU.readAll())
			if (codeAmenite.equals(line[0])) {
				try {
					classement[0] = line[2];
					classement[1] = line[3];
					classement[2] = line[4];
				} catch (Exception e) {
				}
				classement[3] = line[1];
			}
		listCERTU.close();
		return classement;
	}
}