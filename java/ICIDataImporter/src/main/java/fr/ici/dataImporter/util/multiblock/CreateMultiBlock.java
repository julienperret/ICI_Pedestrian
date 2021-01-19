package fr.ici.dataImporter.util.multiblock;

import fr.ign.artiscales.tools.carto.CountPointInPolygon;
import fr.ign.artiscales.tools.geoToolsFunctions.StatisticOperation;
import fr.ign.artiscales.tools.geoToolsFunctions.rasters.Rasters;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.Collec;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.feature.simple.SimpleFeature;
import fr.ici.dataImporter.util.Util;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class CreateMultiBlock {
    public static void main(String[] args) throws IOException {
        File rootFolder = Util.getRootFolder();
//        File blockFile = new File(rootFolder, "fr/ici/dataImporter/paris/ilotVeme.gpkg");
//        File buildingFile = new File(rootFolder, "IGN/batVeme.gpkg");
        DataStore ds = Collec.getDataStore(new File(rootFolder,"IGN/batVeme.gpkg"));
SimpleFeatureCollection sfc = ds.getFeatureSource(ds.getTypeNames()[0]).getFeatures();
        Rasters.rasterize(sfc,"NATURE",new Dimension(1000,1000), sfc.getBounds(),"");

//        createMultiBlock(blockFile, buildingFile, MergeGoal.LGT, 200, new File("/tmp/"));
    }


    public static File createMultiBlock(File blockFile, File goalFile, MergeGoal mergeGoal, double threshold, File resultFolder) throws IOException {
        DataStore dsBlock = Collec.getDataStore(blockFile);
        SimpleFeatureCollection blockSFC = dsBlock.getFeatureSource(dsBlock.getTypeNames()[0]).getFeatures();

        // see if the mergeGoal info is present or not in the block and add it if not
        switch (mergeGoal) {
            case LGT:
                if (!Collec.isCollecContainsAttribute(blockSFC, "NB_LOGTS-SUM")) {
                    DataStore buildingDS = Collec.getDataStore(goalFile);
                    blockSFC = CountPointInPolygon.countPointInPolygon(buildingDS.getFeatureSource(buildingDS.getTypeNames()[0]).getFeatures(), "LGT", blockSFC,
                            false, Arrays.asList("NB_LOGTS"), null, Arrays.asList(StatisticOperation.SUM));
                    buildingDS.dispose();
                }
                break;
            case POP:
                if (!Collec.isCollecContainsAttribute(blockSFC, "countPOP")) {
                    DataStore popDS = Collec.getDataStore(goalFile);
                    blockSFC = CountPointInPolygon.countPointInPolygon(popDS.getFeatureSource(popDS.getTypeNames()[0]).getFeatures(), "POP", blockSFC,
                            true);
                    popDS.dispose();
                }
                break;
        }
        Collec.exportSFC(blockSFC,new File("/tmp/exBock.gpkg"));
        try (SimpleFeatureIterator blockIt = blockSFC.features()) {
            while (blockIt.hasNext()) {
                SimpleFeature block = blockIt.next();
                //get the value
                switch (mergeGoal) {
                    case LGT:
                        if ( ((double) block.getAttribute("NB_LOGTS-SUM")) < (threshold * 1.1) || ((double)block.getAttribute("NB_LOGTS-SUM")) > (threshold *0.9)){

                        }
                        break;
                }
            }
        } catch (Exception problem) {
            problem.printStackTrace();
        }
        dsBlock.dispose();
        return resultFolder;
    }
}
