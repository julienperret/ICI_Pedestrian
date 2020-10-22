package insee;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class Geocode {
	
	
	 public static void main(String[] args) throws IOException {
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
		System.out.println(adress);
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
