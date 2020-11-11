package transport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;

import com.fasterxml.jackson.core.JsonParseException;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;

public class SubwayStation extends Station {
	MultiPoint entrances;

	public SubwayStation(MultiPoint entrances, List<String[]> idsSTIF, String nameStation, List<String> lineName)
			throws JsonParseException, IOException {
		super();
		this.entrances = entrances;
		this.idsSTIF = idsSTIF;
		this.name = nameStation;
		this.lineNames.addAll(lineName);
	}

	public SubwayStation(File dailyPProfiles, File dailyVValidation, File folderOut) {
		super(dailyPProfiles, dailyVValidation, folderOut);
	}

	/**
	 * This compares a soon to be created subway station with other created subway station. ID is unique for the couple station/line. As we want to calculate the frequentation per
	 * stations, not necessarily per lines, we add the ids regarding to their names (which are also uniques).
	 * 
	 * @param mp
	 * @param codeSTIF
	 * @param nameStation
	 * @param alreadyProceededSubwayStation
	 * @return
	 * @throws JsonParseException
	 * @throws IOException
	 */
	public static List<SubwayStation> add(MultiPoint mp, String[] codeSTIF, String nameStation, String lineName,
			List<SubwayStation> alreadyProceededSubwayStation) throws JsonParseException, IOException {
		SubwayStation subwayStation = null;
		List<String[]> idsStif = new ArrayList<String[]>();
		idsStif.add(codeSTIF);
		for (int i = 0; i < alreadyProceededSubwayStation.size(); i++) {
			if (alreadyProceededSubwayStation.get(i).getName().equals(nameStation)) 
				subwayStation = mergeSubwayStations(
						Arrays.asList(alreadyProceededSubwayStation.remove(i), new SubwayStation(mp, idsStif, nameStation, Arrays.asList(lineName))));
		}
		if (subwayStation == null)
			subwayStation = new SubwayStation(mp, idsStif, nameStation, Arrays.asList(lineName));
		alreadyProceededSubwayStation.add(subwayStation);
		return alreadyProceededSubwayStation;
	}

	private static SubwayStation mergeSubwayStations(List<SubwayStation> subwayStationList) throws JsonParseException, IOException {
		// merge points
		List<Point> lP = new ArrayList<Point>();
		List<String[]> codesSTIF = new ArrayList<String[]>();
		List<String> lineNames = new ArrayList<String>();
		for (SubwayStation ss : subwayStationList) {
			for (int i = 0; i < ss.getEntrances().getNumGeometries(); i++)
				lP.add((Point) ss.getEntrances().getGeometryN(i));
			codesSTIF.addAll(ss.getIDsSTIF());
			lineNames.addAll(ss.getLineNames());
		}
		GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 2154);
		return new SubwayStation(gf.createMultiPoint(lP.toArray(Point[]::new)), codesSTIF, subwayStationList.get(0).getName(), lineNames);
	}

	public static SimpleFeatureBuilder getSubwayStationSFB() {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		try {
			sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sfTypeBuilder.setName("SubwayStation");
		sfTypeBuilder.add(Collec.getDefaultGeomName(), MultiPoint.class);
		sfTypeBuilder.add("name", String.class);
		sfTypeBuilder.add("route_ref", String.class);
		sfTypeBuilder.add("IdSTIF", String.class);
		sfTypeBuilder.add("JoursOuvre6_9H", String.class);
		sfTypeBuilder.add("JoursOuvre17_20H", String.class);
		sfTypeBuilder.add("PlusGrosseAffluenceHorsJoursOuvre", String.class);
		sfTypeBuilder.add("PeriodePlusGrosseAffluenceHorsJoursOuvre", String.class);
		sfTypeBuilder.add("numberOfEntrances", Integer.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		return new SimpleFeatureBuilder(sfTypeBuilder.buildFeatureType());
	}

	public MultiPoint getEntrances() {
		return entrances;
	}

	public void setEntrances(MultiPoint entrances) {
		this.entrances = entrances;
	}

	private static String iDsToId(List<String[]> idss) {
		String result = "";
		for (String[] ids : idss) {
			for (String id : ids)
				result = result + "-" + id;
			result = result + "/";
		}
		return result.substring(1, result.length() - 1);
	}

	public SimpleFeature generateFeature() throws IOException {
		SimpleFeatureBuilder sfb = getSubwayStationSFB();
		sfb.set(Collec.getDefaultGeomName(), this.getEntrances());
		sfb.set("name", this.getName());
		sfb.set("route_ref", this.lineNames);
		sfb.set("IdSTIF", iDsToId(this.getIDsSTIF()));
		sfb.set("JoursOuvre6_9H", this.getMorningWeekdayAffluence());
		sfb.set("JoursOuvre17_20H", this.getEveningWeekdayAffluence());
		Pair<String, Double> maxFreq = getMaxFrequency(getProfileDayWithoutWorkDay());
		sfb.set("PlusGrosseAffluenceHorsJoursOuvre", maxFreq.getRight());
		sfb.set("PeriodePlusGrosseAffluenceHorsJoursOuvre", maxFreq.getLeft());
		sfb.set("numberOfEntrances", entrances.getNumGeometries());
		return sfb.buildFeature(Attribute.makeUniqueId());
	}

}
