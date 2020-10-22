package insee;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.StandardCopyOption;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class Geocode {

	public static void main(String[] args) throws IOException, URISyntaxException {
		String[] adresseInfos = { "18", "RUE", "DE LA SORBONNE", "75005" };
		geocodeAdresseDataGouv(adresseInfos, new File("/tmp/out"));
		// geocodeCSVAdresseDataGouv(new File("/home/ubuntu/Documents/INRIA/donnees/POI/SIRENE-POI-treated.csv"), new File("/tmp/out"));
	}

	/**
	 * https://geo.api.gouv.fr/adresse Utilise la BAN
	 * 
	 * @param adresseInfos
	 * @param outFile
	 * @return the geojson file with point/infos about the geocoding
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static File geocodeAdresseDataGouv(String[] adresseInfos, File outFile) throws IOException, URISyntaxException {
		// https://api-adresse.data.gouv.fr/search/?q=8+bd+du+port&postcode=44380
		CloseableHttpClient httpclient = HttpClients.createDefault();
		URI uri = new URIBuilder().setScheme("https").setHost("api-adresse.data.gouv.fr").setPath("/search/")
				.setParameter("q", adresseInfos[0] + "+" + adresseInfos[1] + "+" + adresseInfos[2]).setParameter("postcode", adresseInfos[3]).build();
		HttpGet httppost = new HttpGet(uri);

		CloseableHttpResponse response = httpclient.execute(httppost);
		try {
			System.out.println(response.getStatusLine().getStatusCode());
			InputStream stream = response.getEntity().getContent();
			java.nio.file.Files.copy(stream, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			stream.close();
		} finally {
			response.close();
		}
		return outFile;
	}

	/**
	 * https://geo.api.gouv.fr/adresse . Utilise la BAN. \\FIXME not working (yet?)
	 * 
	 * @param adresseInfos
	 * @param outFile
	 * @return
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	public static String geocodeCSVAdresseDataGouv(File inFile, File outFile) throws IOException, URISyntaxException {

		// curl -X POST -F data=@search.csv -F columns=adresse -F columns=postcode -F result_columns=result_id -F result_columns=score https://api-adresse.data.gouv.fr/search/csv/

		CloseableHttpClient httpclient = HttpClients.createDefault();
		URI uri = new URIBuilder().setScheme("https").setHost("api-adresse.data.gouv.fr").setPath("/search/csv/")
				.setParameter("data", "@" + inFile.getAbsolutePath()).setParameter("columns", "numAdresse").setParameter("columns", "typeRue")
				.setParameter("columns", "adresse").setParameter("postcode", "codPostal").build();
		HttpPost httppost = new HttpPost(uri);

		CloseableHttpResponse response = httpclient.execute(httppost);
		try {
			System.out.println(response.getStatusLine().getStatusCode());
			InputStream stream = response.getEntity().getContent();
			java.nio.file.Files.copy(stream, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			stream.close();
		} finally {
			response.close();
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
		CloseableHttpResponse response = httpclient.execute(httppost);
		try {
			System.out.println(response.getStatusLine().getStatusCode());
			// for (Header e : response.getAllHeaders())
			// System.out.println(e); // https://geocodage.ign.fr/look4/location/search%3Fq=38%20RUE%20GAY%20LUSSAC%2075005%20Paris%3FreturnTrueGeometry=true/search/
		} finally {
			response.close();
		}
		return null;
	}

	public static void geocodeBANO(String[] adresseInfos) throws IOException, URISyntaxException {
	}
}
