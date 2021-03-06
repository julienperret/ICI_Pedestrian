package fr.ici.dataImporter.transport;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.io.Csv;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.opengis.feature.simple.SimpleFeature;
import fr.ici.dataImporter.util.Util;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public abstract class Station {
	List<String> lineNames = new ArrayList<>();
	boolean wheelchair;
	File dailyProfiles = new File(Util.getRootFolder(), "fr/ici/dataImporter/paris/mobilite/validations-sur-le-reseau-ferre-profils-horaires-par-jour-type-2e-sem.csv"),
			dailyValidation = new File(Util.getRootFolder(),
					"fr/ici/dataImporter/paris/mobilite/validations-sur-le-reseau-ferre-nombre-de-validations-par-jour-2e-sem.csv"),
			folderOut = new File(Util.getRootFolder(), "fr/ici/dataImporter/paris/fr.ici.dataImporter.transport");
	String name;
	List<String[]> idsSTIF;
	LinkedMap frequentation = new LinkedMap();

	public Station(File dailyPProfiles, File dailyVValidation, File folderOut) {
		this.dailyProfiles = dailyPProfiles;
		this.dailyValidation = dailyVValidation;
		this.folderOut = folderOut;
	}

	public abstract SimpleFeature generateFeature() throws IOException;

	public Station() {
	}

	/**
	 * Determine the frequencies and select their maximum and the moment they are at their maximum
	 * 
	 * @param typeProfileDay
	 *            types of day (see {@link #getEveryTypeProfileDay()} for every possible types and
	 *            <a href="https://data.iledefrance-mobilites.fr/explore/dataset/validations-sur-le-reseau-de-surface-profils-horaires-par-jour-type-1er-sem/information/">IdF
	 *            website</a> for its explainaiton
	 */
	public Pair<String, Double> getMaxFrequency(String[] typeProfileDay) {
		System.out.println("================= " + this.name + " =================");
		if (frequentation == null || frequentation.isEmpty())
			calculateFrequency();
		// look for the biggest frequentation per selected day
		double maxFreq = 0;
		String maxFreqPeriod = "";
		iniLoop: for (Object f : frequentation.keySet()) {
			String key = (String) f;
			String typeDay = key.split(",")[0];
			for (String tp : typeProfileDay)
				if (typeDay.equals(tp)) {
					maxFreq = (double) frequentation.get(f);
					maxFreqPeriod = key;
					break iniLoop;
				}
		}
		return new ImmutablePair<>(maxFreqPeriod, maxFreq);
	}

	public void calculateFrequency() {
		File statFolder = new File(folderOut, "stat");
		statFolder.mkdirs();
		try {
			this.frequentation = getAffluence(dailyProfiles, dailyValidation, statFolder, this.name, idsSTIF, getEveryTypeProfileDay());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String[] getEveryTypeProfileDay() {
		return new String[]{ "JOHV", "SAHV", "JOVS", "SAVS", "DIJFP" };
	}

	public static String[] getProfileDayWithoutWorkDay() {
		return new String[]{ "SAHV", "JOVS", "SAVS", "DIJFP" };
	}

	public double getMorningWeekdayAffluence() throws IOException {
		System.out.println("Morning Weekday Affluence");
		return getAffluence(dailyProfiles, dailyValidation, idsSTIF, Arrays.asList("7H-8H", "8H-9H", "9H-10H"), "JOHV");
	}

	/**
	 * Get the affluence from 7pm to 8pm
	 * @return the affluence
	 * @throws IOException
	 */
	public double getEveningWeekdayAffluence() throws IOException {
		System.out.println("Evening Weekday Affluence");
		return getAffluence(dailyProfiles, dailyValidation, idsSTIF, Arrays.asList("17H-18H", "18H-19H", "19H-20H"), "JOHV");
	}

	/**
	 * Seek for the period with the most affluence
	 * @param dailyProfiles File with the daily profile
	 * @param dailyValidation File with the daily validation
	 * @param folderOut folder to
	 * @param stationsIDSTIF
	 * @param profilesDay
	 * @return
	 * @throws IOException
	 */
	public static Pair<String, Double> getBestAffluence(File dailyProfiles, File dailyValidation, File folderOut, List<String[]> stationsIDSTIF,
			String[] profilesDay) throws IOException {
		LinkedMap affluence = getAffluence(dailyProfiles, dailyValidation, folderOut, "", stationsIDSTIF, profilesDay);
		return new ImmutablePair<>((String) affluence.lastKey(), (Double) affluence.get(affluence.lastKey()));
	}

	public static LinkedMap getAffluence(File dailyProfiles, File dailyValidation, File folderOut, String stationName, List<String[]> stationsIDSTIF,
			String[] profilesDay) throws IOException {
		return getAffluence(dailyProfiles, dailyValidation, folderOut, stationName, stationsIDSTIF, profilesDay, 1);
	}

	public static LinkedMap getAffluence(File dailyProfiles, File dailyValidation, File folderOut, String stationName, List<String[]> stationsIDSTIF,
			String[] profilesDay, int hoursSlices) throws IOException {
		HashMap<String, Double> h = new HashMap<>();
		for (String profileDay : profilesDay) {
			System.out.println("+++++++++" + profileDay + "+++++++++");
			for (int i = 0; i < 24; i++) {
				List<String> hours = new ArrayList<>(Collections.singletonList(i + "H-" + (createTime(i + 1) + "H")));
				for (int j = 1; j < hoursSlices; j++)
					hours.add((i + j) + "H-" + (createTime(i + j + 1)) + "H");
				h.put(profileDay + "," + hours.toString() + "," + i, getAffluence(dailyProfiles, dailyValidation, stationsIDSTIF, hours, profileDay));
			}
		}
		LinkedMap sortedMap = new LinkedMap();
		List<Map.Entry<String, Double>> list = new ArrayList<>(h.entrySet());

		// sort the entries based on the value by custom Comparator
		list.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));
		// put all sorted entries in LinkedHashMap
		for (Map.Entry<String, Double> entry : list)
			sortedMap.put(entry.getKey(), entry.getValue());

		// generate a .csv to generate a graph
		if (!stationName.equals("")) {
			Csv.generateCsvFile(stationName, folderOut, new String[]{ "TypeOfDay", "Hour", "StartHour", "NumberOfValidation" }, false, h);
		}
		return sortedMap;
	}

	public static double getAffluence(File dailyProfiles, File dailyValidation, List<String[]> stationsIDsSTIF, List<String> hours, String dayType)
			throws IOException {
		double result = 0;
		// for every single station id
		CSVParser parser = new CSVParserBuilder().withSeparator(';').build();
		for (String[] stationIDsSTIF : stationsIDsSTIF) {
			// Get percentage
			CSVReader csvDailyProfiles = new CSVReaderBuilder(new FileReader(dailyProfiles)).withCSVParser(parser).build();
			String[] fLine = csvDailyProfiles.readNext();
			int iIDCODE_STIF_TRNS = Attribute.getIndice(fLine, "CODE_STIF_TRNS");
			int iIDCODE_STIF_RES = Attribute.getIndice(fLine, "CODE_STIF_RES");
			int iIDCODE_STIF_ARRET = Attribute.getIndice(fLine, "CODE_STIF_ARRET");
			int iCatJour = Attribute.getIndice(fLine, "CAT_JOUR");
			int iHours = Attribute.getIndice(fLine, "TRNC_HORR_60");
			int ipourc_validations = Attribute.getIndice(fLine, "pourc_validations");
			double percentage = 0.0;
			for (String[] line : csvDailyProfiles.readAll()) {
				if (line[iCatJour].equals(dayType) && line[iIDCODE_STIF_TRNS].equals(stationIDsSTIF[0])
						&& line[iIDCODE_STIF_RES].equals(stationIDsSTIF[1]) && line[iIDCODE_STIF_ARRET].equals(stationIDsSTIF[2])
						&& hours.contains(line[iHours])) {
					percentage = percentage + Double.parseDouble(line[ipourc_validations]);
				}
			}
			System.out.println("hours: " + hours);
			System.out.println("percentage: " + percentage);
			csvDailyProfiles.close();

			// Get absolute value of validation
			CSVReader csvDailyValidation = new CSVReaderBuilder(new FileReader(dailyValidation)).withCSVParser(parser).build();
			String[] fLineValid = csvDailyValidation.readNext();
			int iNbValidation = Attribute.getIndice(fLineValid, "NB_VALD");
			iIDCODE_STIF_TRNS = Attribute.getIndice(fLineValid, "CODE_STIF_TRNS");
			iIDCODE_STIF_RES = Attribute.getIndice(fLineValid, "CODE_STIF_RES");
			iIDCODE_STIF_ARRET = Attribute.getIndice(fLineValid, "CODE_STIF_ARRET");

			DescriptiveStatistics validations = new DescriptiveStatistics();
			for (String[] line : csvDailyValidation.readAll())
				if (line[iIDCODE_STIF_TRNS].equals(stationIDsSTIF[0]) && line[iIDCODE_STIF_RES].equals(stationIDsSTIF[1])
						&& line[iIDCODE_STIF_ARRET].equals(stationIDsSTIF[2])) {
					double nb = 2;
					if (!line[iNbValidation].equals("Moins de 5"))
						nb = Double.parseDouble(line[iNbValidation]);
					validations.addValue(nb);
				}
			csvDailyValidation.close();
			if (validations.getN() != 0)
				result = result + validations.getMean() * percentage;
			else
				result = 0.0;
		}
		return result;
	}

	private static int createTime(int i) {
		return i >= 24 ? i - 24 : i;
	}

	private static String hoursToString(List<String> hours) {
		StringBuilder result = new StringBuilder();
		for (String h : hours)
			result.append("/").append(h);
		return result.substring(1, result.length() - 1);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<String[]> getIDsSTIF() {
		return idsSTIF;
	}

	public void setIdSTIF(List<String[]> idSTIF) {
		this.idsSTIF = idSTIF;
	}

	public List<String> getLineNames() {
		return lineNames;
	}
}
