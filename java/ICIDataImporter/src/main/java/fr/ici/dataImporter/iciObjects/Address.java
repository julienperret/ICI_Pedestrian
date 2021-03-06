package fr.ici.dataImporter.iciObjects;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecTransform;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;
import org.opengis.referencing.FactoryException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

public class Address extends ICIObject {
    int number, irisCode;
    String suffix, nameStreet, typ_loc, source;
    Point geom;

    public Address(int number, String suffix, String nameStreet, int irisCode, String typ_loc, String source, Point geom) {
        this.ID = "ADDRESS" + Attribute.makeUniqueId();
        this.type = "ADDRESS";
        this.number = number;
        this.suffix = suffix;
        this.nameStreet = nameStreet;
        this.irisCode = irisCode;
        this.typ_loc = typ_loc;
        this.source = source;
        this.geom = geom;
    }

    /**
     * Construct a ICI Address object from a {@link SimpleFeature} of the BAN
     *
     * @param add the address from the BAN
     * @return the ICI object
     */
    public static Address toICI(SimpleFeature add) {
        return new Address((int) add.getAttribute("numero"), (String) add.getAttribute("suffix"),
                (String) add.getAttribute("nom_voie"), (int) add.getAttribute("code_insee"),
                (String) add.getAttribute("typ_loc"), (String) add.getAttribute("source"), (Point) add.getDefaultGeometry());
    }

    /**
     * Affect the number of households in a building to (the closest) address.
     *
     * @param buildingSFC Building collection containing the fields to match
     * @param address     Address collection. Can optionnaly contain the information whether they are entrances or parcels in the <i>typ_loc</i> field (such as the French BAN).
     * @return Collection of addresses with summary of the nearby building's characteristics
     * @throws IOException
     */
    public static SimpleFeatureCollection affectHouseholdsToAddress(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection address) throws IOException {
        return affectFieldToAddress(buildingSFC, address, "household", "NB_LOGTS", null);
    }

    /**
     * Affect the number of individuals in a building to (the closest) address.
     *
     * @param buildingSFC Building collection containing the fields to match
     * @param address     Address collection. Can optionnaly contain the information whether they are entrances or parcels in the <i>typ_loc</i> field (such as the French BAN).
     * @return Collection of addresses with summary of the nearby building's characteristics
     * @throws IOException
     */
    public static SimpleFeatureCollection affectIndivToAddress(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection address) throws IOException {
        return affectFieldToAddress(buildingSFC, address, "individuals", "count", null);
    }

    /**
     * Affect demographic statistics in a building to (the closest) address.
     *
     * @param buildingSFC Building collection containing the fields to match
     * @param address     Address collection. Can optionnaly contain the information whether they are entrances or parcels in the <i>typ_loc</i> field (such as the French BAN).
     * @param demoColl    Array of attributes composing the demographic statistics
     * @return Collection of addresses with summary of the nearby building's characteristics
     * @throws IOException
     */
    public static SimpleFeatureCollection affectDemoStatToAddress(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection address, String[] demoColl) throws IOException {
        return affectFieldToAddress(buildingSFC, address, "demoStats", "count", demoColl);
    }

    /**
     * Affect fields of an input building collection to (the closest) address.
     *
     * @param buildingSFC       Building collection containing the fields to match
     * @param address           Address collection. Can optionnaly contain the information whether they are entrances or parcels in the <i>typ_loc</i> field (such as the French BAN).
     * @param methodToUse       Method (technique) of which field(s) to attach.
     * @param mandatoryAttrName An attribute name that value has to be a double, not null or 0 in order for the building to be counted. If empty, check's not done.
     * @param demoColl          (optional)
     * @return Collection of addresses with summary of the nearby building's characteristics
     * @throws IOException
     */
    public static SimpleFeatureCollection affectFieldToAddress(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection address, String methodToUse, String mandatoryAttrName, String[] demoColl) throws IOException {
        DefaultFeatureCollection addresses = new DefaultFeatureCollection();
        SimpleFeatureBuilder sfb = getSFBforAffection(methodToUse, address, demoColl);
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        // for every buildings
        try (SimpleFeatureIterator itBuilding = buildingSFC.features()) {
            while (itBuilding.hasNext()) {
                SimpleFeature building = itBuilding.next();
                if (mandatoryAttrName != null && (building.getAttribute(mandatoryAttrName) == null || (double) building.getAttribute(mandatoryAttrName) == 0))
                    continue;
                // We search for addresses in the building
                SimpleFeatureCollection addressInBuilding = address.subCollection(ff.within(ff.property(address.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(building.getDefaultGeometry())));
                if (addressInBuilding.size() == 0) {// If no address have been found and the building have households, we look at the closest address and affect the building's households to that address
                    addresses.addAll(affectMethodToAffection(methodToUse, getClosestAddress(address, building, ff), sfb, building, demoColl));
                } else {// If a single address has been found and they don't have attribute about entrance, we keep it
                    if (!CollecMgmt.isCollecContainsAttribute(addressInBuilding, "typ_loc"))
                        addresses.addAll(affectMethodToAffection(methodToUse, addressInBuilding, sfb, building, demoColl));
                        // if no entrance, we check for further entrances
                    else if (getEntrancesFromAddresses(addressInBuilding, ff).size() == 0)
                        addresses.addAll(affectMethodToAffection(methodToUse, getClosestAddress(address, building, ff), sfb, building, demoColl));
                    else
                        addresses.addAll(affectMethodToAffection(methodToUse, getEntrancesFromAddresses(addressInBuilding, ff), sfb, building, demoColl));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // if addresses has multiple occurrences, we merge them
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        List<String> fids = new ArrayList<>();
        try (SimpleFeatureIterator itAddressesTemp = addresses.features()) {
            while (itAddressesTemp.hasNext()) {
                SimpleFeature aT = itAddressesTemp.next();
                if (!fids.isEmpty() && fids.contains(aT.getAttribute("id_ban_adresse")))
                    continue;
                SimpleFeatureCollection addressDoubled = DataUtilities.collection(addresses.subCollection(ff.like(ff.property("id_ban_adresse"), (String) aT.getAttribute("id_ban_adresse"))));
                if (addressDoubled.size() == 1)
                    result.add(aT);
                else {
                    result.add(CollecTransform.unionAttributesOfAPoint(addressDoubled, StatisticOperation.SUM, Collections.singletonList("IDsBuilding")));
                    fids.add((String) aT.getAttribute("id_ban_adresse"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * @param addresses input addresses
     * @param ff        filter factory to use (that may fast the process)
     * @return entrance addressses (or the input addresses if no <i>typ_loc</i> field)
     */
    public static SimpleFeatureCollection getEntrancesFromAddresses(SimpleFeatureCollection addresses, FilterFactory2 ff) {
        if (!CollecMgmt.isCollecContainsAttribute(addresses, "typ_loc"))
            return addresses;
        return addresses.subCollection(ff.like(ff.property("typ_loc"), "entrance"));
    }

    private static SimpleFeatureCollection affectMethodToAffection(String method, SimpleFeatureCollection addressesSFC, SimpleFeatureBuilder addressBuilder, SimpleFeature building, String[] demoColl) throws InvalidPropertiesFormatException {
        switch (method) {
            case "ID":
                return affectBuildingIDsPerAddress(addressesSFC, addressBuilder, building);
            case "household":
                return affectHouseholdPerAddress(addressesSFC, addressBuilder, building);
            case "individuals":
                return affectIndividualsPerAddress(addressesSFC, addressBuilder, building);
            case "demoStats":
                return affectDemoStatPerAddress(addressesSFC, addressBuilder, building, demoColl);
        }
        throw new InvalidPropertiesFormatException("affectMethodToAffection : method " + method + " not implemented");

    }

    /**
     * Affect the ID of buildings (from ICI objects) to the closest address(es).
     *
     * @param addressesSFC   list of addresses
     * @param addressBuilder builder to create new points
     * @param building       building containing <i>NB_INDIV</i> attribute
     * @return a collection of {@link SimpleFeature} with addressesSFC geometries and number of households
     */
    public static SimpleFeatureCollection affectBuildingIDsPerAddress(SimpleFeatureCollection addressesSFC, SimpleFeatureBuilder addressBuilder, SimpleFeature building) {
        if (addressesSFC == null)
            return new DefaultFeatureCollection();
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        SimpleFeatureType addressSchema = addressesSFC.getSchema();
        try (SimpleFeatureIterator addressesIt = addressesSFC.features()) {
            while (addressesIt.hasNext()) {
                SimpleFeature address = addressesIt.next();
                addressBuilder.set("IDsBuilding", building.getAttribute("ID"));
                for (AttributeDescriptor attr : addressSchema.getAttributeDescriptors())
                    addressBuilder.set(new NameImpl(attr.getLocalName()), address.getAttribute(attr.getName()));
                addressBuilder.set(addressSchema.getGeometryDescriptor().getLocalName(), address.getDefaultGeometry());
                result.add(addressBuilder.buildFeature(Attribute.makeUniqueId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Sum affected households (<i>logements</i> from BDTopo) to the closest address.
     *
     * @param addressesSFC   list of addresses
     * @param addressBuilder builder to create new points
     * @param building       buildings containing <i>NB_LOGTS</i> attribute
     * @return a collection of {@link SimpleFeature} with addressesSFC geometries and number of households
     */
    public static SimpleFeatureCollection affectHouseholdPerAddress(SimpleFeatureCollection addressesSFC, SimpleFeatureBuilder addressBuilder, SimpleFeature building) {
        return affectAndSumSingleFieldPerAddress(addressesSFC, addressBuilder, building, "NB_LOGTS", "NB_LOGTS");
    }

    /**
     * Sum affected individuals (from <i>H24</i> export) to the closest address(es).
     *
     * @param addressesSFC   list of addresses
     * @param addressBuilder builder to create new points
     * @param building       building containing <i>NB_INDIV</i> attribute
     * @return a collection of {@link SimpleFeature} with addressesSFC geometries and number of households
     */
    public static SimpleFeatureCollection affectIndividualsPerAddress(SimpleFeatureCollection addressesSFC, SimpleFeatureBuilder addressBuilder, SimpleFeature building) {
        return affectAndSumSingleFieldPerAddress(addressesSFC, addressBuilder, building, "count", "NB_INDIV");
    }

    /**
     * Sum the affected (demographic) data of buildings to the closest address.
     *
     * @param addressesSFC   list of addresses
     * @param addressBuilder builder to create new points
     * @param building       buildings containing <i>NB_LOGTS</i> attribute
     * @param fields         the fields to affect
     * @return addresses with affection
     */
    public static SimpleFeatureCollection affectDemoStatPerAddress(SimpleFeatureCollection addressesSFC, SimpleFeatureBuilder addressBuilder, SimpleFeature building, String[] fields) {
        if (addressesSFC == null)
            return new DefaultFeatureCollection();
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        SimpleFeatureType addressSchema = addressesSFC.getSchema();
        try (SimpleFeatureIterator addressesIt = addressesSFC.features()) {
            while (addressesIt.hasNext()) {
                SimpleFeature address = addressesIt.next();
                for (String field : fields) {
                    double val = 0;
                    if (building.getAttribute(field) != null)
                        val = (double) building.getAttribute(field);
                    addressBuilder.set(field, val / addressesSFC.size());
                }
                for (AttributeDescriptor attr : addressSchema.getAttributeDescriptors())
                    addressBuilder.set(new NameImpl(attr.getLocalName()), address.getAttribute(attr.getName()));
                addressBuilder.set(addressSchema.getGeometryDescriptor().getLocalName(), address.getDefaultGeometry());
                result.add(addressBuilder.buildFeature(Attribute.makeUniqueId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Sum a field to the closest address.
     *
     * @param addressesSFC   list of addresses
     * @param addressBuilder builder to create new points
     * @param building       buildings containing <i>NB_LOGTS</i> attribute
     * @param fieldIn        name of the field to affect
     * @param fieldOut       name of the field to write in the builder
     * @return addresses with affection
     */
    public static SimpleFeatureCollection affectAndSumSingleFieldPerAddress(SimpleFeatureCollection addressesSFC, SimpleFeatureBuilder addressBuilder, SimpleFeature building, String fieldIn, String fieldOut) {
        if (addressesSFC == null)
            return new DefaultFeatureCollection();
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        SimpleFeatureType addressSchema = addressesSFC.getSchema();
        try (SimpleFeatureIterator addressesIt = addressesSFC.features()) {
            while (addressesIt.hasNext()) {
                SimpleFeature address = addressesIt.next();
                addressBuilder.set(fieldOut, (int) building.getAttribute(fieldIn) / addressesSFC.size());
                for (AttributeDescriptor attr : addressSchema.getAttributeDescriptors())
                    addressBuilder.set(new NameImpl(attr.getLocalName()), address.getAttribute(attr.getName()));
                addressBuilder.set(addressSchema.getGeometryDescriptor().getLocalName(), address.getDefaultGeometry());
                result.add(addressBuilder.buildFeature(Attribute.makeUniqueId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Recursively look for an address meter by meter. Understands if address has is a <i>entrance</i> type rather than a <i>parcel</i> in a <b>typ_loc</b> field.
     *
     * @param addressSFC input address collection
     * @return the closest entrance(s) address(es)
     */
    public static SimpleFeatureCollection getClosestAddress(SimpleFeatureCollection addressSFC, SimpleFeature building, FilterFactory2 ff) {
        double x = 0;
        //we check meters by meters if there's entrances
        while (x < 75) {
            SimpleFeatureCollection closeAddress = addressSFC.subCollection(ff.dwithin(ff.property(addressSFC.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(building.getDefaultGeometry()), x, "meters"));
            if (closeAddress.size() > 0)
                if (!CollecMgmt.isCollecContainsAttribute(addressSFC, "typ_loc"))
                    return closeAddress;
                else if (getEntrancesFromAddresses(closeAddress, ff).size() > 0)
                    return getEntrancesFromAddresses(closeAddress, ff);
            x++;
        }
        System.out.println("no addresses for building : " + building);
        return null;
    }

    /**
     * Select a predefined SimpleFeatureBuilder for the type of chosen summarization. Mostly add column(s) to address schema.
     *
     * @param method     Name of the type of affection to operate
     * @param addressSFC Collection of address
     * @param demoColl   optional array of fields to add
     * @return the builder with the address's schema + wanted schema
     * @throws InvalidPropertiesFormatException If method of affection not found
     */
    public static SimpleFeatureBuilder getSFBforAffection(String method, SimpleFeatureCollection addressSFC, String[] demoColl) throws InvalidPropertiesFormatException {
        switch (method) {
            case "ID":
                return Schemas.addColToSFB(addressSFC, "IDsBuilding", String.class);
            case "household":
                return Schemas.addFloatColToSFB(addressSFC, "NB_LOGTS");
            case "individuals":
                return Schemas.addFloatColToSFB(addressSFC, "NB_INDIV");
            case "demoStats":
                return Schemas.addColsToSFB(addressSFC, demoColl, Integer.class);
        }
        throw new InvalidPropertiesFormatException("getSFBforAffection : method " + method + " not implemented");
    }

    private static SimpleFeatureBuilder getAddressSFB() {
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode(Schemas.getEpsg()));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName("WalkableTile");
        sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Point.class);
        sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
        sfTypeBuilder.add("ID", String.class);
        sfTypeBuilder.add("type", String.class);
        sfTypeBuilder.add("number", Integer.class);
        sfTypeBuilder.add("suffix", String.class);
        sfTypeBuilder.add("nameStreet", String.class);
        sfTypeBuilder.add("irisCode", Integer.class);
        sfTypeBuilder.add("typ_loc", String.class);
        sfTypeBuilder.add("source", String.class);
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(featureType);
    }

    public String getNameStreet() {
        return nameStreet;
    }

    public Point getGeom() {
        return geom;
    }

    public SimpleFeature getAddressSF() {
        SimpleFeatureBuilder sfb = getAddressSFB();
        sfb.set("ID", ID);
        sfb.set("type", type);
        sfb.set("number", number);
        sfb.set("suffix", suffix);
        sfb.set("nameStreet", nameStreet);
        sfb.set("irisCode", irisCode);
        sfb.set("typ_loc", typ_loc);
        sfb.set("source", source);
        sfb.set(CollecMgmt.getDefaultGeomName(), geom);
        return sfb.buildFeature(Attribute.makeUniqueId());
    }
}
