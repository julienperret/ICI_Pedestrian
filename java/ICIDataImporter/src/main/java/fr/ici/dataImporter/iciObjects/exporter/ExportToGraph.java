package fr.ici.dataImporter.iciObjects.exporter;

import fr.ici.dataImporter.iciObjects.Address;
import fr.ici.dataImporter.iciObjects.Building;
import fr.ici.dataImporter.iciObjects.ICIObject;
import fr.ici.dataImporter.iciObjects.POI;
import fr.ici.dataImporter.util.Util;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.graphml.GraphMLExporter;
import org.jgrapht.traverse.DepthFirstIterator;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExportToGraph {
    public static void main(String[] args) throws IOException {

        Multigraph<ICIObject, DefaultEdge> mg = new Multigraph<>(DefaultEdge.class);
        File rootFolder = Util.getRootFolder();
        List<? extends ICIObject> lBuilding = Building.importBuilding(new File(rootFolder, "ICI/building.gpkg"));
        List<? extends ICIObject> lPOI = POI.importPOI(new File(rootFolder, "ICI/POI.gpkg"));

        // affect POI to buildings
        for (ICIObject b : lBuilding) {
            Polygon polygon = ((Building) b).getGeomBuilding();
            mg.addVertex(b);
            List<ICIObject> lPOIInBuilding = lPOI.stream().filter(x -> polygon.intersects(((POI) x).p)).collect(Collectors.toList());
            for (ICIObject poi : lPOIInBuilding) {
                mg.addVertex(poi);
                mg.addEdge(b, poi);
            }
        }

        // affect buildings to address
        DataStore dsAddress = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "IGN/outban_75_20201002Vemr.gpkg"));
        DataStore dsBuilding = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "ICI/building.gpkg"));

        SimpleFeatureCollection addresses = Address.affectFieldToAddress(dsBuilding.getFeatureSource(dsBuilding.getTypeNames()[0]).getFeatures(), dsAddress.getFeatureSource(dsAddress.getTypeNames()[0]).getFeatures(), "ID", null, null);
        try (SimpleFeatureIterator addIt = addresses.features()) {
            while (addIt.hasNext()) {
                SimpleFeature add = addIt.next();
                ICIObject iciAdd = Address.toICI(add);
                mg.addVertex(iciAdd);
                String[] idBuilding = ((String) add.getAttribute("IDsBuilding")).split(",");
                for (String idb :  idBuilding){
                    mg.addEdge(mg.vertexSet().stream().filter(x -> x.getID().equals(idb)).findAny().get(), iciAdd);
                }
            }
        }
        dsBuilding.dispose();
        dsAddress.dispose();

        //try to export only a building linked to its POIs
        ICIObject anyPoiInFirst = mg.vertexSet().stream().filter(x -> x.getID().equals("BATIMENT0000000241938792")).findAny()
                .get();
        Iterator<ICIObject> iterator = new DepthFirstIterator<>(mg, anyPoiInFirst);
        Multigraph<ICIObject, DefaultEdge> one = new Multigraph<>(DefaultEdge.class);
        one.addVertex(anyPoiInFirst);
        while (iterator.hasNext()) {
            ICIObject poi = iterator.next();
            if (poi.equals(anyPoiInFirst))
                continue;
            one.addVertex(poi);
            one.addEdge(anyPoiInFirst, poi);
        }


// we may have to overload equals, tostring and hashcode
        FileWriter w = new FileWriter(new File("/tmp/out.graphml"));
        GraphMLExporter<ICIObject, DefaultEdge> ex = new GraphMLExporter<>(v -> v.getID());
        ex.registerAttribute("ID", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        ex.registerAttribute("amenitySourceName", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        ex.registerAttribute("name", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        ex.registerAttribute("potentialArea", GraphMLExporter.AttributeCategory.NODE, AttributeType.DOUBLE);
        ex.registerAttribute("openingHour", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        ex.registerAttribute("attendanceIndice", GraphMLExporter.AttributeCategory.NODE, AttributeType.INT);
        ex.registerAttribute("housingDistribution", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);

        ex.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            if (v instanceof POI) {
                map.put("amenitySourceName", DefaultAttribute.createAttribute(((POI) v).amenitySourceName));
                map.put("name", DefaultAttribute.createAttribute(((POI) v).name));
                map.put("potentialArea", DefaultAttribute.createAttribute(((POI) v).potentialArea));
                map.put("openingHour", DefaultAttribute.createAttribute(((POI) v).openingHour));
                map.put("attendanceIndice", DefaultAttribute.createAttribute(((POI) v).attendanceIndice));
            } else if (v instanceof Building)
                map.put("housingDistribution", DefaultAttribute.createAttribute(((Building) v).getAreaHousingLot().stream().map(x -> String.valueOf(x)).collect(Collectors.joining(","))));
            return map;
        });
        ex.exportGraph(mg, w);
        ex.exportGraph(one, new FileWriter(new File("/tmp/one.graphml")));
    }


}