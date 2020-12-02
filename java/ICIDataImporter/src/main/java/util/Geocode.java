package util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import fr.ign.artiscales.tools.io.Json;

public class Geocode {

	public static void main(String[] args) throws IOException {
		String[] adresseInfos = { null, "MAR", "MONGE", "75005" };
		String[] geocode = geocodePelias(adresseInfos);
		System.out.println("score:" + geocode[0] + " x:" + geocode[1] + " y:" + geocode[2]);
		// geocodeCSVAdresseDataGouv(new File(rootFolder,"POI/SIRENE-POI-treated.csv"), new File("/tmp/out"));
	}

	/**
	 * Trouver un service de démo car search.mapzen.com n'est plus dispo
	 * 
	 * @param adresseInfos
	 * @return
	 * @throws IOException
	 */
	public static String[] geocodePelias(String[] adresseInfos) throws IOException {
		for (int i = 0; i < adresseInfos.length; i++)
			if (adresseInfos[i] == null || adresseInfos[i].equals("null"))
				adresseInfos[i] = "";
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String[] geoloc = new String[3];
		URI uri;
		try {
			uri = new URIBuilder().setScheme("https").setHost("search.mapzen.com").setPath("/v1/search/")
					.setParameter("texte", adresseInfos[0] + "+" + adresseInfos[1] + "+" + adresseInfos[2])
					.setParameter("boundary.gid", "whosonfirst:region:12652969").build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return geoloc;
		}
		HttpGet httppost = new HttpGet(uri);
		File out = new File("/tmp/tmpAdressGeocoded");
		try (CloseableHttpResponse response = httpclient.execute(httppost)) {
			// FIXME adapt to the answer
			// InputStream stream = response.getEntity().getContent();
			// java.nio.file.Files.copy(stream, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
			// HashMap<String, Object> answer = Json.getFirstObject(out);
			// geoloc[0] = (String) answer.get("score") != null ? (String) answer.get("score") : "0";
			// geoloc[1] = (String) answer.get("x") != null ? (String) answer.get("x") : "0";
			// geoloc[2] = (String) answer.get("y") != null ? (String) answer.get("y") : "0";
			// stream.close();
		}
		return geoloc;
	}

	/**
	 * https://geo.api.gouv.fr/adresse Utilise la BAN. Return [0] : score ; [1] long and [2] lat in WGS84 format
	 * 
	 * @param adresseInfos
	 * @return the geojson file with point/infos about the geocoding
	 * @throws IOException
	 */
	public static String[] geocodeAdresseDataGouv(String[] adresseInfos) throws IOException {
		// https://api-adresse.data.gouv.fr/search/?q=8+bd+du+port&postcode=44380
		// make "" instead of null
		for (int i = 0; i < adresseInfos.length; i++)
			if (adresseInfos[i] == null || adresseInfos[i].equals("null"))
				adresseInfos[i] = "";

		CloseableHttpClient httpclient = HttpClients.createDefault();
		String[] geoloc = new String[3];
		URI uri;
		try {
			uri = new URIBuilder().setScheme("https").setHost("api-adresse.data.gouv.fr").setPath("/search/")
					.setParameter("q", adresseInfos[0] + "+" + adresseInfos[1] + "+" + adresseInfos[2]).setParameter("postcode", adresseInfos[3])
					.build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return geoloc;
		}
		HttpGet httppost = new HttpGet(uri);
		File out = new File("/tmp/tmpAdressGeocoded");
		try (CloseableHttpResponse response = httpclient.execute(httppost)) {
			InputStream stream = response.getEntity().getContent();
			java.nio.file.Files.copy(stream, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
			HashMap<String, Object> answer = Json.getFirstObject(out);
			geoloc[0] = answer.get("score") != null ? (String) answer.get("score") : "0";
			geoloc[1] = answer.get("x") != null ? (String) answer.get("x") : "0";
			geoloc[2] = answer.get("y") != null ? (String) answer.get("y") : "0";
			stream.close();
		}
		return geoloc;
	}

	/**
	 * https://geo.api.gouv.fr/adresse . Géocode entièrement un fichier .csv. Plus rapide que le géocodage 1 by 1. Utilise la base adresse locale. \\FIXME not working (yet?). La
	 * requete suivante marche //curl -X POST -F data=@inFile -F columns=adresse -F columns=typeRue -F columns=numAdresse -F postcode=codPostal
	 * https://api-adresse.data.gouv.fr/search/csv/ > outFile
	 * 
	 * 
	 * @param outFile
	 * @return
	 * @throws IOException
	 */
	public static String geocodeCSVAdresseDataGouv(File inFile, File outFile) throws IOException {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		URI uri;
		try {
			uri = new URIBuilder().setScheme("https").setHost("api-adresse.data.gouv.fr").setPath("/search/csv/")
					.setParameter("data", "@" + inFile.getAbsolutePath()).setParameter("columns", "numAdresse").setParameter("columns", "typeRue")
					.setParameter("columns", "adresse").setParameter("postcode", "codPostal").build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
		HttpPost httppost = new HttpPost(uri);
		System.out.println(uri);
		try (CloseableHttpResponse response = httpclient.execute(httppost)) {
			System.out.println(response.getStatusLine().getStatusCode());
			InputStream stream = response.getEntity().getContent();
			java.nio.file.Files.copy(stream, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			stream.close();
		}
		return null;
	}

	/**
	 * Geocode with IGN services. Pour l'instant ça marche bien qu'ici https://adresse.data.gouv.fr/csv#preview
	 * 
	 * @param adresseInfos
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static String geocodeIGN(String[] adresseInfos) throws IOException, URISyntaxException {
		File out = new File("/tmp/out.dump");
		Request.Post("https://geocodage.ign.fr/look4/location/search/%3Fq=" + adresseInfos[0] + "%20" + adresseInfos[1] + "%20"
				+ adresseInfos[2].replace(" ", "%3F") + "%20" + adresseInfos[3] + "%3FreturnTrueGeometry=true").execute().saveContent(out);
		// FIXME 404 errors...
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String adress = adresseInfos[0] + " " + adresseInfos[1] + " " + adresseInfos[2] + " " + adresseInfos[3] + " " + "Paris";
		URI uri = new URIBuilder().setScheme("https").setHost("geocodage.ign.fr").setPath("/look4/address/search/")
				.setParameter("q", adress + "?returnTrueGeometry=true").build();
		HttpPost httppost = new HttpPost(uri);
		try (CloseableHttpResponse response = httpclient.execute(httppost)) {
			System.out.println(response.getStatusLine().getStatusCode());
			// for (Header e : response.getAllHeaders())
			// System.out.println(e); // https://geocodage.ign.fr/look4/location/search%3Fq=38%20RUE%20GAY%20LUSSAC%2075005%20Paris%3FreturnTrueGeometry=true/search/
		}
		return null;
	}

	public static void geocodeBANO(String[] adresseInfos) {
	}
}
