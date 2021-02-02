import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.GeoPkgDataStoreFactory;
import org.geotools.process.vector.VectorToRasterProcess;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class Test {
    public static void main(String[] args) throws Exception {

        DataStore dsBBox = new ShapefileDataStore(new File("/tmp/5eme.shp").toURI().toURL());
        ReferencedEnvelope bb = dsBBox.getFeatureSource(dsBBox.getTypeNames()[0]).getFeatures().getBounds();
        System.out.println("bb.getCoordinateReferenceSystem() = " + bb.getCoordinateReferenceSystem());
        DataStore dsWF = getDataStoreGPKG(new File("/tmp/wp.gpkg"));
        SimpleFeatureCollection sfcWF = dsWF.getFeatureSource(dsWF.getTypeNames()[0]).getFeatures();
        System.out.println("sfcWF.getSchema().getCoordinateReferenceSystem() = " + sfcWF.getSchema().getCoordinateReferenceSystem());
        GridCoverage2D rasterWorkforce = VectorToRasterProcess.process(sfcWF, "WorkforceEstimate", new Dimension(1500, 1500), bb, "workforceCode",null);
        GridCoverage2D rasterBB = VectorToRasterProcess.process(dsBBox.getFeatureSource(dsBBox.getTypeNames()[0]).getFeatures(), "mask", new Dimension(1500, 1500), bb, "workforceCode",null);

        writeGeotiff(rasterBB, new File("/tmp/bb.geotiff"));
        writeGeotiff(rasterWorkforce, new File("/tmp/workforce.geotiff"));
        dsBBox.dispose();
        dsWF.dispose();
    }

    public static DataStore getDataStoreGPKG(File file) throws IOException {
        HashMap<String, Object> map = new HashMap<>();
        map.put(GeoPkgDataStoreFactory.DBTYPE.key, "geopkg");
        map.put(GeoPkgDataStoreFactory.DATABASE.key, file.getPath());
        return DataStoreFinder.getDataStore(map);
    }

    public static void writeGeotiff(GridCoverage2D coverage, File fileName) {
        try {
            GeoTiffWriteParams wp = new GeoTiffWriteParams();
            wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
            wp.setCompressionType("LZW");
            ParameterValueGroup params = new GeoTiffFormat().getWriteParameters();
            params.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);
            GeoTiffWriter writer = new GeoTiffWriter(fileName);
            writer.write(coverage, params.values().toArray(new GeneralParameterValue[1]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
