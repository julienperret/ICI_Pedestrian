package fr.ici.dataImporter;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.rasters.Rasters;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.FilterFactory2;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class ConvertToRaster {

    public static void main(String[] args) throws Exception {
        DataStore dsAdress = Collec.getDataStore(new File("/home/mc/Documents/inria/donnees/IGN/outban_75_20201002Vemr.gpkg"));
        DataStore dsBuilding = Collec.getDataStore(new File("/home/mc/Documents/inria/donnees/IGN/batVeme.gpkg"));

        SimpleFeatureCollection conv = affectHouseholdsToAdress(dsBuilding.getFeatureSource(dsBuilding.getTypeNames()[0]).getFeatures(), dsAdress.getFeatureSource(dsAdress.getTypeNames()[0]).getFeatures());
        Rasters.writeGeotiff(new File("/tmp/buildingPop"), Rasters.rasterize(conv, "NB_LOGTS", new Dimension(500, 500), conv.getBounds(), "lgt"));

        DataStore dsWP = Collec.getDataStore(new File("/home/mc/Documents/inria/donnees/IGN/batVeme.gpkg"));

        Object[] attributes = { };

//        Rasters.writeGeotiff(new File("/tmp/WorkingPlaces"),
//                Rasters.rasterize(dsWP.getFeatureSource(dsWP.getTypeNames()[0]).getFeatures(), , new Dimension(500, 500), conv.getBounds(), "lgt"));
        dsWP.dispose();
        dsAdress.dispose();
        dsBuilding.dispose();
    }

    public static SimpleFeatureCollection affectHouseholdsToAdress(SimpleFeatureCollection buildingSFC, SimpleFeatureCollection adress) throws IOException {
        DefaultFeatureCollection addresses = new DefaultFeatureCollection();
        SimpleFeatureBuilder sfb = Schemas.addFloatColToSFB(adress, "NB_LOGTS");
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
        try (SimpleFeatureIterator itBuilding = buildingSFC.features()) {
            while (itBuilding.hasNext()) {
                SimpleFeature building = itBuilding.next();
                if (building.getAttribute("NB_LOGTS") == null || (int) building.getAttribute("NB_LOGTS") == 0)
                    continue;
                SimpleFeatureCollection addressInBuiding = adress.subCollection(ff.within(ff.property(adress.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(building.getDefaultGeometry())));
                if (addressInBuiding.size() == 0) {// If no address have been found and the building have households, we look at the closest address and affect the building's households to that address
                    addresses.addAll(affectHouseholdPerAddress(getClosestAddress(adress, building, ff), sfb, building));
                } else {// If a single address has been found and they don't have attribute about entrance, we keep it
                    if (!Collec.isCollecContainsAttribute(addressInBuiding, "typ_loc"))
                        addresses.addAll(affectHouseholdPerAddress(addressInBuiding, sfb, building));
                        // if no entrance, we check for further entrances
                    else if (getEntrancesFromAddresses(addressInBuiding, ff).size() == 0)
                        addresses.addAll(affectHouseholdPerAddress(getClosestAddress(adress, building, ff), sfb, building));
                    else
                        addresses.addAll(affectHouseholdPerAddress(getEntrancesFromAddresses(addressInBuiding, ff), sfb, building));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Collec.exportSFC(addresses, new File("/tmp/entrances"));
        return addresses;
    }

    public static SimpleFeatureCollection getEntrancesFromAddresses(SimpleFeatureCollection addresses, FilterFactory2 ff) {
        if (!Collec.isCollecContainsAttribute(addresses, "typ_loc")) {
            System.out.println("getEntrancesFromAddresses: not BANO");
            return addresses;
        }
        return addresses.subCollection(ff.like(ff.property("typ_loc"), "entrance"));
    }

    public static SimpleFeatureCollection affectHouseholdPerAddress(SimpleFeatureCollection addressesSFC, SimpleFeatureBuilder addressBuilder, SimpleFeature building) {
        if (addressesSFC == null)
            return new DefaultFeatureCollection();
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        SimpleFeatureType addressSchema = addressesSFC.getSchema();
        try (SimpleFeatureIterator addressesIt = addressesSFC.features()) {
            while (addressesIt.hasNext()) {
                SimpleFeature address = addressesIt.next();
                int lgtSize = Math.round((int) building.getAttribute("NB_LOGTS") / addressesSFC.size());
                addressBuilder.set("NB_LOGTS", lgtSize);
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
     * Recursively look for an address meter by meter. Best if address has is a "entrance" type
     *
     * @param addressSFC
     * @return
     */
    public static SimpleFeatureCollection getClosestAddress(SimpleFeatureCollection addressSFC, SimpleFeature building, FilterFactory2 ff) {
        double x = 0;
        //we check meters by meters if there's entrances
        while (x < 75) {
            SimpleFeatureCollection closeAddress = addressSFC.subCollection(ff.dwithin(ff.property(addressSFC.getSchema().getGeometryDescriptor().getLocalName()), ff.literal(building.getDefaultGeometry()), x, "meters"));
            if (closeAddress.size() > 0)
                if (!Collec.isCollecContainsAttribute(addressSFC, "typ_loc"))
                    return closeAddress;
                else if (getEntrancesFromAddresses(closeAddress, ff).size() > 0)
                    return getEntrancesFromAddresses(closeAddress, ff);
            x++;
        }
        System.out.println("no addresses for building : " + building);
        return null;
    }
}
