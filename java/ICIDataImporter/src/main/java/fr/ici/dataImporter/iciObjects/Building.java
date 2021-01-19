package fr.ici.dataImporter.iciObjects;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.geom.Polygons;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import fr.ici.dataImporter.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Building {
    String ID, nature, usage1, usage2;
    boolean light;
    double height, area;
    int nbStairs, nbLgt;
    List<String> idAmenities;
    List<String> idEntrances;
    Polygon geom;

    public Building(String ID, String nature, String usage1, String usage2, boolean light, double height, double area, int nbStairs, int nbLgt, Polygon geom) {
        this.ID = ID;
        this.nature = nature;
        this.usage1 = usage1;
        this.usage2 = usage2;
        this.light = light;
        this.height = height;
        this.area = area;
        this.nbStairs = nbStairs;
        this.nbLgt = nbLgt;
        this.geom = geom;
    }

    static File exportBuildings(List<Building> lB, File fileOut) throws IOException {
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        for (Building b : lB)
            result.add(b.generateSimpleFeature());
        return Collec.exportSFC(result, fileOut);
    }

    public static List<Building> importBuilding(File buildingBDTopoFile) throws IOException {
        DataStore ds = Collec.getDataStore(buildingBDTopoFile);
        List<Building> lB = new ArrayList<>();
        try (SimpleFeatureIterator fIt = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures().features()) {
            while (fIt.hasNext()) {
                SimpleFeature feat = fIt.next();
                lB.add(new Building((String) feat.getAttribute("ID"), (String) feat.getAttribute("NATURE"),
                        (String) feat.getAttribute("USAGE1"), (String) feat.getAttribute("USAGE2"),
                        feat.getAttribute("LEGER").equals("OUI"), (double) feat.getAttribute("HAUTEUR"),
                        ((Geometry) feat.getDefaultGeometry()).getArea(),
                        feat.getAttribute("NB_ETAGES") == null ? 0 : (Integer) feat.getAttribute("NB_ETAGES"),
                        feat.getAttribute("NB_LGTS") == null ? 0 : (Integer) feat.getAttribute("NB_LGTS"),
                        Polygons.getPolygon((Geometry) feat.getDefaultGeometry())));
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        ds.dispose();
        return lB;
    }

    public static SimpleFeatureBuilder getBuildingSFB() {
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        sfTypeBuilder.setName("BuildingICI");
        try {
            sfTypeBuilder.setCRS(CRS.decode("EPSG:2154"));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.add(Collec.getDefaultGeomName(), Polygon.class);
        sfTypeBuilder.setDefaultGeometry(Collec.getDefaultGeomName());
        sfTypeBuilder.add("ID", String.class);
        sfTypeBuilder.add("nature", String.class);
        sfTypeBuilder.add("usage1", String.class);
        sfTypeBuilder.add("usage2", String.class);
        sfTypeBuilder.add("light", Boolean.class);
        sfTypeBuilder.add("height", Double.class);
        sfTypeBuilder.add("area", Double.class);
        sfTypeBuilder.add("nbStairs", Integer.class);
        sfTypeBuilder.add("nbLgt", Integer.class);
        sfTypeBuilder.add("idsAmenities", String.class);
        sfTypeBuilder.add("idsEntrances", String.class);
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(featureType);
    }

    public static void main(String[] args) throws Exception {
        File rootFolder = Util.getRootFolder();
        File buildingFile = new File(rootFolder, "IGN/batVeme.gpkg");
        List<Building> lb = importBuilding(buildingFile);
        exportBuildings(lb, new File("/tmp/b.gpkg"));
    }

    public SimpleFeature generateSimpleFeature() {
        SimpleFeatureBuilder sfb = getBuildingSFB();
        sfb.set(Collec.getDefaultGeomName(), geom);
        sfb.set("ID", ID);
        sfb.set("nature", nature);
        sfb.set("usage1", usage1);
        sfb.set("usage2", usage2);
        sfb.set("light", light);
        sfb.set("height", height);
        sfb.set("area", area);
        sfb.set("nbStairs", nbStairs);
        sfb.set("nbLgt", nbLgt);
        sfb.set("idsAmenities", idAmenities);
        sfb.set("idsEntrances", idEntrances);
        return sfb.buildFeature(Attribute.makeUniqueId());
    }
}
