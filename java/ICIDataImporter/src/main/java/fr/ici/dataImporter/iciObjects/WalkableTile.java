package fr.ici.dataImporter.iciObjects;

import fr.ign.artiscales.tools.geoToolsFunctions.Attribute;
import fr.ign.artiscales.tools.geoToolsFunctions.Schemas;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;

public class WalkableTile extends ICIObject {
    Geometry geom;

    public WalkableTile(String id, Geometry geom) {
        ID = id;
        type = "walkableTile";
        this.geom = geom;
    }

    private static SimpleFeatureBuilder getWalkableSFB() {
        SimpleFeatureTypeBuilder sfTypeBuilder = new SimpleFeatureTypeBuilder();
        try {
            sfTypeBuilder.setCRS(CRS.decode(Schemas.getEpsg()));
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        sfTypeBuilder.setName("WalkableTile");
        sfTypeBuilder.add(CollecMgmt.getDefaultGeomName(), Polygon.class);
        sfTypeBuilder.setDefaultGeometry(CollecMgmt.getDefaultGeomName());
        sfTypeBuilder.add("ID", String.class);
        SimpleFeatureType featureType = sfTypeBuilder.buildFeatureType();
        return new SimpleFeatureBuilder(featureType);
    }

    public Geometry getGeom() {
        return geom;
    }

    public SimpleFeature getWalkableSF() {
        SimpleFeatureBuilder sfb = getWalkableSFB();
        sfb.set("ID", ID);
        sfb.set(CollecMgmt.getDefaultGeomName(), geom);
        return sfb.buildFeature(Attribute.makeUniqueId());
    }
}
