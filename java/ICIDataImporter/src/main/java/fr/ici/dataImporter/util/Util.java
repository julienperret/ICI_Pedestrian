package fr.ici.dataImporter.util;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class Util {

	public static void main(String[] args) {
		System.out.println(jaro_distance("Assurance tout risque", "inshallah"));
	}


  /**
   * Code found here https://www.geeksforgeeks.org/jaro-and-jaro-winkler-similarity/
   * This code is contributed by PrinciRaj1992
   * @return
   */
// Function to calculate the 
// Jaro Similarity of two Strings 
public static double jaro_distance(String s1, String s2) 
{ 
    // If the Strings are equal 
    if (s1.equals(s2))
        return 1.0; 
  
    // Length of two Strings 
    int len1 = s1.length(), 
        len2 = s2.length(); 
  
    // Maximum distance upto which matching 
    // is allowed 
    int max_dist = (int) (Math.floor(Math.max(len1, len2) / 2) - 1); 
  
    // Count of matches 
    int match = 0; 
  
    // Hash for matches 
    int[] hash_s1 = new int[s1.length()];
    int[] hash_s2 = new int[s2.length()];
  
    // Traverse through the first String 
    for (int i = 0; i < len1; i++)  
    { 
  
        // Check if there is any matches 
        for (int j = Math.max(0, i - max_dist); 
            j < Math.min(len2, i + max_dist + 1); j++) 
  
            // If there is a match 
            if (s1.charAt(i) == s2.charAt(j) && hash_s2[j] == 0)  
            { 
                hash_s1[i] = 1; 
                hash_s2[j] = 1; 
                match++; 
                break; 
            } 
    } 
  
    // If there is no match 
    if (match == 0) 
        return 0.0; 
  
    // Number of transpositions 
    double t = 0; 
  
    int point = 0; 
  
    // Count number of occurances 
    // where two characters match but 
    // there is a third matched character 
    // in between the indices 
    for (int i = 0; i < len1; i++) 
        if (hash_s1[i] == 1) 
        { 
  
            // Find the next matched character 
            // in second String 
            while (hash_s2[point] == 0) 
                point++; 
  
            if (s1.charAt(i) != s2.charAt(point++) ) 
                t++; 
        } 
  
    t /= 2; 
  
    // Return the Jaro Similarity 
    return (((double)match) / ((double)len1) 
            + ((double)match) / ((double)len2) 
            + ((double)match - t) / ((double)match)) 
        / 3.0; 
} 


	
	public static File getRootFolder() {
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

	public static String getToken(String serviceKey)  {
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

	/**
	 * from https://stackoverflow.com/questions/2344320/comparing-strings-with-tolerance
	 * 
	 * @param source
	 * @param target
	 * @return
	 */
	public static int LevenshteinDistance(String source, String target) {
		// degenerate cases
		if (source.equals(target))
			return 0;
		if (source.length() == 0)
			return target.length();
		if (target.length() == 0)
			return source.length();

		// create two work vectors of integer distances
		int[] v0 = new int[target.length() + 1];
		int[] v1 = new int[target.length() + 1];

		// initialize v0 (the previous row of distances)
		// this row is A[0][i]: edit distance for an empty s
		// the distance is just the number of characters to delete from t
		for (int i = 0; i < v0.length; i++)
			v0[i] = i;

		for (int i = 0; i < source.length(); i++) {
			// calculate v1 (current row distances) from the previous row v0

			// first element of v1 is A[i+1][0]
			// edit distance is delete (i+1) chars from s to match empty t
			v1[0] = i + 1;

			// use formula to fill in the rest of the row
			for (int j = 0; j < target.length(); j++) {
				int cost = (source.charAt(i) == target.charAt(j)) ? 0 : 1;
				v1[j + 1] = Math.min(v1[j] + 1, Math.min(v0[j + 1] + 1, v0[j] + cost));
			}

			// copy v1 (current row) to v0 (previous row) for next iteration
			System.arraycopy(v1, 0, v0, 0, v0.length);
		}

		return v1[target.length()];
	}
}
