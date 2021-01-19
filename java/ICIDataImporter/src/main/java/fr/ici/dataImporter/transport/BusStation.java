package fr.ici.dataImporter.transport;

import java.io.File;
import java.io.IOException;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ici.dataImporter.util.Util;

public class BusStation extends Station {

	boolean shelter;

	public BusStation() throws IOException {
		super();
		dailyProfiles = new File(Util.getRootFolder(),
				"fr/ici/dataImporter/paris/mobilite/validations-sur-le-reseau-de-surface-profils-horaires-par-jour-type-1er-sem.csv");
		dailyValidation = new File(Util.getRootFolder(),
				"fr/ici/dataImporter/paris/mobilite/validations-sur-le-reseau-de-surface-nombre-de-validations-par-jour-1er-sem.csv");
		getMaxFrequency(Station.getProfileDayWithoutWorkDay());
	}

	@Override
	public SimpleFeature generateFeature() throws IOException {
		SimpleFeatureBuilder sfb = getBusStationSFB();
		sfb.set("6-9HJoursOuvre", getMorningWeekdayAffluence());
		sfb.set("17-20HJoursOuvre", getEveningWeekdayAffluence());
		sfb.set("PlusGrosseAffluenceHorsJoursOuvre", this.getMorningWeekdayAffluence());
		sfb.set("PeriodePlusGrosseAffluenceHorsJoursOuvre", this.getEveningWeekdayAffluence());
		sfb.set("type", String.class);
		sfb.set("name", String.class);
		sfb.set("pub_trans", String.class);
		sfb.set("route_ref", String.class);
		sfb.set("wheelchair", String.class);
		sfb.set("shelter", String.class);
		return sfb.buildFeature(Attribute.makeUniqueId());
	}

	public static SimpleFeatureBuilder getBusStationSFB() {
		SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
		try {
			sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		sfTypeBuilder.setName("transportationOSM");
		sfTypeBuilder.add(Collec.getDefaultGeomName(), Point.class);
		sfTypeBuilder.add("type", String.class);
		sfTypeBuilder.add("name", String.class);
		sfTypeBuilder.add("pub_trans", String.class);
		sfTypeBuilder.add("route_ref", String.class);
		sfTypeBuilder.add("wheelchair", String.class);
		sfTypeBuilder.add("shelter", String.class);
		sfTypeBuilder.add("IdSTIF", String.class);
		sfTypeBuilder.add("6-9HJoursOuvre", String.class);
		sfTypeBuilder.add("17-20HJoursOuvre", String.class);
		sfTypeBuilder.add("PlusGrosseAffluenceHorsJoursOuvre", String.class);
		sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
		SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
		return new SimpleFeatureBuilder(featureType);
	}
}
