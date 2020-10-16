package insee;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.Header;
import org.apache.http.client.fluent.Request;
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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Geom;
import fr.ign.tools.dataImporter.SortAmenitiesCategories;

public class SIRENEImport {
	public static void main(String[] args)
			throws IOException, URISyntaxException, MismatchedDimensionException, NoSuchAuthorityCodeException, FactoryException, TransformException {
		// getSIRENEData();
		// parseSIRENEData(new File("/home/ubuntu/sirene.json"));
		MakePointOutOfGeocode(new File("/tmp/geocodage.ign.fr.json"));
	}

	public static void getSIRENEData() throws IOException, URISyntaxException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		URI uri = new URIBuilder().setScheme("https").setHost("api.insee.fr").setPath("/entreprises/sirene/V3/siret")
				.setParameter("q", "codePostalEtablissement:75005&nombre=100000").build();
		HttpPost httppost = new HttpPost(uri);
		// httppost.addHeader("Accept","application/csv");
		httppost.addHeader("Content-Type", "application/x-www-form-urlencoded");
		httppost.addHeader("Authorization", "Bearer a2887e94-5cbe-3e9d-94da-93af59eacfcf");
		System.out.println(httppost);
		for (Header h : httppost.getAllHeaders()) {
			System.out.println(h);
		}
		CloseableHttpResponse response = httpclient.execute(httppost);
		try {
			System.out.println(response.getStatusLine().getStatusCode());
			for (Header e : response.getAllHeaders())
				System.out.println(e);
		} finally {
			response.close();
		}

		// this works on a command line : curl -X POST --header 'Content-Type: application/x-www-form-urlencoded' --header 'Accept=application/csv' --header 'Authorization: Bearer
		// a2887e94-5cbe-3e9d-94da-93af59eacfcf' -d 'q=codePostalEtablissement%3A75005&nombre=100000' 'https://api.insee.fr/entreprises/sirene/V3/siret'
		// this code should work too - returns a 404...
	}

	public static void parseSIRENEData(File jSON) throws IOException, URISyntaxException {
		JsonFactory factory = new JsonFactory();
		JsonParser parser = factory.createParser(jSON);
		JsonToken token = parser.nextToken();
		String nAdresse = "";
		String adresse = "";
		String typeVoie = "";
		String codePos = "";
		String[] adresseInfos = new String[4];
		String[] classement = new String[3];
		int count = 0;
		while (!parser.isClosed()) {
			token = parser.nextToken();
			if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("numeroVoieEtablissement")) {
				token = parser.nextToken();
				nAdresse = parser.getText();
				adresseInfos[0] = nAdresse;
			}
			if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("libelleVoieEtablissement")) {
				token = parser.nextToken();
				adresse = parser.getText();
				adresseInfos[2] = adresse;
			}
			if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("typeVoieEtablissement")) {
				token = parser.nextToken();
				typeVoie = parser.getText();
				adresseInfos[1] = typeVoie;
			}
			if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("codePostalEtablissement")) {
				token = parser.nextToken();
				codePos = parser.getText();
				adresseInfos[3] = codePos;
			}

			// get the activity and sort it. If activity is unlisted,
			if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals("activitePrincipaleEtablissement")) {
				token = parser.nextToken();
				if (token == JsonToken.VALUE_STRING)
					classement = SortAmenitiesCategories.classSIRENEEntry(parser.getText());
			}
			if (token == JsonToken.END_OBJECT && (classement != null && classement[0] != null)) {
				// System.out.println("adresse : " + nAdresse + ", " + typeVoie + " " + adresse +" - " + codePos);
				for (String c : classement)
					System.out.println(c);
				geocodeIGN(adresseInfos);

				System.out.println();

				count++;
			}
		}
		System.out.println(count);
	}

	public static String geocodeIGN(String[] adresseInfos) throws IOException, URISyntaxException {
		File out = new File("/tmp/out.dump");
		Request.Post("https://geocodage.ign.fr/look4/location/search/%3Fq=" + adresseInfos[0] + "%20" + adresseInfos[1] + "%20"
				+ adresseInfos[2].replace(" ", "%3F") + "%20" + adresseInfos[3] + "%3FreturnTrueGeometry=true").execute().saveContent(out);

		// CloseableHttpClient httpclient = HttpClients.createDefault();
		// String adress = adresseInfos[0] + " " + adresseInfos[1]+ " " + adresseInfos[2]+ " "+ adresseInfos[3]+ " " + "Paris";
		// System.out.println(adress);
		// URI uri = new URIBuilder().setScheme("https").setHost("geocodage.ign.fr").setPath("/look4/address/search/")
		// .setParameter("q", adress+"?returnTrueGeometry=true").build();
		// HttpPost httppost = new HttpPost(uri);
		// CloseableHttpResponse response = httpclient.execute(httppost);
		// try {
		// System.out.println(response.getStatusLine().getStatusCode());
		//// for (Header e : response.getAllHeaders())
		//// System.out.println(e); // https://geocodage.ign.fr/look4/location/search%3Fq=38%20RUE%20GAY%20LUSSAC%2075005%20Paris%3FreturnTrueGeometry=true/search/
		// } finally {
		// response.close();
		// }
		return null;
	}

	public static void MakePointOutOfGeocode(File jsonFile)
			throws IOException, NoSuchAuthorityCodeException, FactoryException{
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

		//
	}
}