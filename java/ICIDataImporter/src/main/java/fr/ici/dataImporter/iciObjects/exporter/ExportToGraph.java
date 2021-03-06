package fr.ici.dataImporter.iciObjects.exporter;

import fr.ici.dataImporter.iciObjects.Address;
import fr.ici.dataImporter.iciObjects.Building;
import fr.ici.dataImporter.iciObjects.ICIObject;
import fr.ici.dataImporter.iciObjects.POI;
import fr.ici.dataImporter.iciObjects.WalkableTile;
import fr.ici.dataImporter.iciObjects.WorkingPlace;
import fr.ici.dataImporter.insee.SireneEntry;
import fr.ici.dataImporter.util.Util;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecMgmt;
import fr.ign.artiscales.tools.geoToolsFunctions.vectors.collec.CollecTransform;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.util.factory.GeoTools;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.graphml.GraphMLExporter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExportToGraph {

    static boolean EXPORTINTERMEDIATE = false;

    public static void main(String[] args) throws IOException {
        File rootFolder = Util.getRootFolder();
        EXPORTINTERMEDIATE = true;
        exportToGraph(rootFolder, true);
    }

    public static void exportToGraph(File rootFolder, boolean importFeature) throws IOException {
        Multigraph<ICIObject, DefaultEdge> mg = new Multigraph<>(DefaultEdge.class);
        List<? extends ICIObject> lBuilding;
        List<? extends ICIObject> lPOI;
        List<? extends SireneEntry> lWP;

        //import or create feature (for now, working places has a type mismatch)
        if (importFeature) {
            lPOI = POI.importPOI(new File(rootFolder, "ICI/POI.gpkg"));
            lBuilding = Building.importBuilding(new File(rootFolder, "ICI/building.gpkg"));
            lWP = WorkingPlace.importWorkingPlace(new File(rootFolder, "ICI/SIRENE-WorkingPlace.gpkg"));
        } else {
            lPOI = POI.importPOIFromUsualSources(rootFolder);
            lBuilding = Building.importBuilding(new File(rootFolder, "IGN/batVeme.gpkg"), new File(rootFolder, "INSEE/IRIS-logements.gpkg"), new File(rootFolder, "paris/APUR/commercesVeme.gpkg"), new File(rootFolder, "INSEE/POI/SIRENE-WorkingPlace.gpkg"), new File(rootFolder, "ICI/POI.gpkg"));
            lWP = new ArrayList<>();
           /* for (int i = 0; i <= 42 ; i++) {
                for (SireneEntry s : SireneImport.parseSireneEntry(new File(rootFolder, "POI/sireneAPIOut/sirene" + i + ".json"), new File(rootFolder, "ICI/"), "WorkingPlace"))
                    lWP.add(s);
            }*/
        }

        // affect POI and Working Place to buildings
        for (ICIObject b : lBuilding) {
            Polygon polygon = ((Building) b).getGeomBuilding();
            mg.addVertex(b);
            for (ICIObject poi : lPOI.stream().filter(x -> polygon.intersects(((POI) x).p)).collect(Collectors.toList())) {
                mg.addVertex(poi);
                mg.addEdge(b, poi);
            }
            for (ICIObject wp : lWP.stream().filter(x -> polygon.intersects(x.p)).collect(Collectors.toList())) {
                mg.addVertex(wp);
                mg.addEdge(b, wp);
            }
        }

        // affect buildings to address
        DataStore dsAddress = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "IGN/outban_75_20201002Vemr.gpkg"));
        DataStore dsBuilding = CollecMgmt.getDataStore(new File(Util.getRootFolder(), "ICI/building.gpkg"));
        List<Address> lAddress = new ArrayList<>();
        try (SimpleFeatureIterator addIt = Address.affectFieldToAddress(dsBuilding.getFeatureSource(dsBuilding.getTypeNames()[0]).getFeatures(), dsAddress.getFeatureSource(dsAddress.getTypeNames()[0]).getFeatures(), "ID", null, null).features()) {
            while (addIt.hasNext()) {
                SimpleFeature add = addIt.next();
                Address iciAdd = Address.toICI(add);
                lAddress.add(iciAdd);
                mg.addVertex(iciAdd);
                for (String idb : ((String) add.getAttribute("IDsBuilding")).split(",")) {
                    mg.addEdge(mg.vertexSet().stream().filter(x -> x.getID().equals(idb)).findAny().get(), iciAdd);
                }
            }
        }
        dsBuilding.dispose();
        dsAddress.dispose();
        if (EXPORTINTERMEDIATE)
            CollecMgmt.exportSFC(lAddress.stream().map(Address::getAddressSF).collect(Collectors.toCollection(DefaultFeatureCollection::new)).collection(), new File("/tmp/address.gpkg"));

        System.out.println("Walkability tile");
        // export walkability nodes and link them to addresses
        DataStore dsPartition = CollecMgmt.getDataStore(new File(rootFolder, "ICI/partition.shp"));
        SimpleFeatureCollection wkbCollec = getDiscretizeWalkable(dsPartition.getFeatureSource(dsPartition.getTypeNames()[0]).getFeatures());
        int wkbId = 0;
        // add every walkable tiles
        List<WalkableTile> lWT = new ArrayList<>();
        try (SimpleFeatureIterator wkbIt = wkbCollec.features()) {
            while (wkbIt.hasNext()) {
                WalkableTile wkb = new WalkableTile("WalkableTile" + wkbId++, (Geometry) wkbIt.next().getDefaultGeometry());
                lWT.add(wkb);
                mg.addVertex(wkb);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (EXPORTINTERMEDIATE)
            CollecMgmt.exportSFC(lWT.stream().map(WalkableTile::getWalkableSF).collect(Collectors.toCollection(DefaultFeatureCollection::new)).collection(), new File("/tmp/wkl.gpkg"));
        System.out.println("link walkable tiles");
        // link tiles
        for (WalkableTile wbt : lWT) {
            // check if the edge between this tile and the tiles touching has been added yet. If not, add it
            for (WalkableTile wbtT : lWT.stream().filter(x -> x.getGeom().touches(wbt.getGeom())).collect(Collectors.toList()))
                if (!mg.containsEdge(wbt, wbtT))
                    mg.addEdge(wbt, wbtT);
            // add address if intersecting Walkable Tile
            for (Address add : lAddress.stream().filter(a -> a.getGeom().intersects(wbt.getGeom())).collect(Collectors.toList()))
                mg.addEdge(add, wbt);
        }
        System.out.println("affect unaffected addresses");
        //  if address unaffected, add address to the closest tile
        for (Address address : lAddress) {
            // if no Address linked
//            System.out.println(mg.outgoingEdgesOf(address).stream().filter(e -> (mg.getEdgeSource(e) instanceof WalkableTile || mg.getEdgeTarget(e) instanceof WalkableTile)).collect(Collectors.toList()));
            if (mg.outgoingEdgesOf(address).stream().noneMatch(e -> (mg.getEdgeSource(e) instanceof WalkableTile || mg.getEdgeTarget(e) instanceof WalkableTile))) {
                int searchZone = 1; // iterate meters by meters
                while (searchZone <= 10) {
//                    List<WalkableTile> l = lWT.stream().filter(a -> a.getGeom().isWithinDistance(address.getGeom(), len)).collect(Collectors.toList());
                    WalkableTile wtConnection = null;
                    for (WalkableTile wt : lWT)
                        if (wt.getGeom().isWithinDistance(address.getGeom(), searchZone)) {
                            wtConnection = wt;
                            break;
                        }
                    if (wtConnection != null) {
                        mg.addEdge(address, wtConnection);
                        break;
                    }
                    if (searchZone == 10)
                        System.out.println("Walkable Tile not found for " + address);
                    searchZone++;
                }
            }
        }

        System.out.println("Exporting schema");
        // Export in GraphML format
        FileWriter w = new FileWriter("/tmp/out.graphml");
        GraphMLExporter<ICIObject, DefaultEdge> ex = new GraphMLExporter<>(ICIObject::getID);
        ex.registerAttribute("ID", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        ex.registerAttribute("amenityTypeNameSource", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        ex.registerAttribute("name", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        ex.registerAttribute("potentialArea", GraphMLExporter.AttributeCategory.NODE, AttributeType.DOUBLE);
        ex.registerAttribute("openingHour", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        ex.registerAttribute("attendanceIndice", GraphMLExporter.AttributeCategory.NODE, AttributeType.INT);
        ex.registerAttribute("housingDistribution", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);
        ex.registerAttribute("workforce", GraphMLExporter.AttributeCategory.NODE, AttributeType.DOUBLE);
        ex.registerAttribute("type", GraphMLExporter.AttributeCategory.NODE, AttributeType.STRING);

        ex.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("type", DefaultAttribute.createAttribute(v.getType()));
            if (v instanceof POI) {
                map.put("amenityTypeNameSource", DefaultAttribute.createAttribute(((POI) v).amenityTypeNameSource));
                map.put("name", DefaultAttribute.createAttribute(((POI) v).name));
                map.put("potentialArea", DefaultAttribute.createAttribute(((POI) v).potentialArea));
                if (v instanceof WorkingPlace)
                    map.put("workforce", DefaultAttribute.createAttribute(((WorkingPlace) v).getWorkforce()));
                else {
                    map.put("openingHour", DefaultAttribute.createAttribute(((POI) v).openingHour));
                    map.put("attendanceIndice", DefaultAttribute.createAttribute(((POI) v).attendanceIndice));
                }
            } else if (v instanceof Building)
                map.put("housingDistribution", DefaultAttribute.createAttribute(((Building) v).getAreaHousingLot().stream().map(String::valueOf).collect(Collectors.joining(","))));
            return map;
        });
        ex.exportGraph(mg, w);

/*        //export in DOT format
        AtomicInteger i =new AtomicInteger();
        DOTExporter<ICIObject, DefaultEdge> exDOT = new DOTExporter<>(v -> String.valueOf(i.incrementAndGet()));
        exDOT.setVertexAttributeProvider((v) -> {
            Map<String, Attribute> map = new LinkedHashMap<>();
            map.put("type", DefaultAttribute.createAttribute(v.getType()));
            if (v instanceof POI || v instanceof WorkingPlace) {
                map.put("amenityTypeNameSource", DefaultAttribute.createAttribute(((POI) v).amenityTypeNameSource));
                map.put("name", DefaultAttribute.createAttribute(((POI) v).name));
                map.put("potentialArea", DefaultAttribute.createAttribute(((POI) v).potentialArea));
                if (v instanceof POI) {
                    map.put("openingHour", DefaultAttribute.createAttribute(((POI) v).openingHour));
                    map.put("attendanceIndice", DefaultAttribute.createAttribute(((POI) v).attendanceIndice));
                } else if (v instanceof WorkingPlace)
                    map.put("workforce", DefaultAttribute.createAttribute(((WorkingPlace) v).getWorkforce()));

            } else if (v instanceof Building)
                map.put("housingDistribution", DefaultAttribute.createAttribute(((Building) v).getAreaHousingLot().stream().map(x -> String.valueOf(x)).collect(Collectors.joining(","))));
            return map;
        });
        exDOT.exportGraph(mg,new File("/tmp/exDot.gv"));*/

    }

    public static SimpleFeatureCollection getDiscretizeWalkable(SimpleFeatureCollection spatialPart) throws IOException {
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        CollecMgmt.setDefaultGeomName("the_geom");//temporary switch to shp like geometry name (import is in shp)
        SimpleFeatureCollection grid = CollecTransform.gridDiscretize(spatialPart.subCollection(ff.not(ff.or(
                Arrays.asList(ff.like(ff.property("label"), "autre"), ff.like(ff.property("label"), "batiment"))))),
                2.774527634, true);
        CollecMgmt.setDefaultGeomName(null);
        return grid;
    }

    //        GraphGenerator gg = new BasicGraphGenerator();
//        FeatureGraphGenerator generator = new FeatureGraphGenerator(gg);
//
//        try (SimpleFeatureIterator it = fc.features()){
//            while (it.hasNext()){
//                generator.add(it.next());
//            }
//        }
//
//        Graph graph = generator.getGraph();
//        System.out.println("graph = " + graph);
}