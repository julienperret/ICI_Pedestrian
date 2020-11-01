package util;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

public class Util {

//	public static void main(String[] args) throws JsonParseException, IOException {
//		for (File f : getRootFolder().listFiles())
//			System.out.println(f);
//	}

	public static File getRootFolder() throws JsonParseException, IOException {
		File result = new File("rootFolder/");
		if (!result.exists()) {
			Scanner myObj = new Scanner(System.in); // Create a Scanner object
			System.out.println(
					"No symbolic link pointing to your root folder. Please create one using 'ln -s {$/targetFolder/} rootFolder'. Meanwhile, you can type one right here in the console please");
			System.out.println("Path to root folder :");
			String key = myObj.nextLine(); // Read user input
			myObj.close();
			return new File(key);
		}
		return result;
	}

	public static String getToken(String serviceKey) throws JsonParseException, IOException {
		try {
			JsonFactory factory = new JsonFactory();
			JsonParser parser = factory.createParser(new File("src/main/resources/APIKeys.json"));
			JsonToken token = parser.nextToken();
			while (!parser.isClosed()) {
				token = parser.nextToken();
				if (token == JsonToken.FIELD_NAME && parser.getCurrentName().equals(serviceKey)) {
					token = parser.nextToken();
					String res = parser.getText();
					if (!res.equals(""))
						return res;
				}
			}
			throw new Exception();
		} catch (Exception e) {
			Scanner myObj = new Scanner(System.in); // Create a Scanner object
			System.out.println("Wrong API key for " + serviceKey
					+ " application. Possible to store it in the src/main/resources/APIKeys.json file. Meanwhile, you can type it right here in the console please");
			System.out.println(serviceKey + " key:");
			String key = myObj.nextLine(); // Read user input
			myObj.close();
			return key;
		}
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
