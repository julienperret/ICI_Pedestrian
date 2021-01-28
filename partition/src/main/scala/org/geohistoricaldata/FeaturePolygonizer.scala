package org.geohistoricaldata

import java.io.File

import org.locationtech.jts.geom.{Geometry, GeometryFactory, LineString, Point, Polygon}
import org.locationtech.jts.operation.polygonize.Polygonizer
import org.opengis.feature.simple.SimpleFeature

object FeaturePolygonizer {
  val fact = new GeometryFactory()
  val DEBUG = true

  import java.io.IOException
  import java.net.MalformedURLException
  import java.util
  import java.util.Calendar

  import org.geotools.data.shapefile.ShapefileDataStore
  import org.geotools.data.simple.SimpleFeatureCollection
  import org.geotools.feature.SchemaException
  import org.locationtech.jts.geom.{MultiLineString, PrecisionModel}
  import org.locationtech.jts.geom.util.LinearComponentExtracter
  import org.locationtech.jts.precision.GeometryPrecisionReducer
  import scala.jdk.CollectionConverters._

  private def getLines(inputFeatures: List[Geometry]) = {
    val linesList = new util.ArrayList[Geometry]
    val lineFilter = new LinearComponentExtracter(linesList)
    for (feature <- inputFeatures) {
      feature.apply(lineFilter)
    }
    linesList.asScala
  }

  private def extractPoint(lines: List[Geometry]): Point = {
    var point: Point = null
    // extract first point from first non-empty geometry
    for (geometry <- lines) {
      if (!geometry.isEmpty && point == null) {
        val p = geometry.getCoordinate
        point = geometry.getFactory.createPoint(p)
      }
    }
    point
  }

  private def nodeLines(lines: List[Geometry]) = {
    val linesGeom = fact.createMultiLineString(lines.toArray.map(_.asInstanceOf[LineString]))
    var unionInput: Geometry = fact.createMultiLineString(null)
    val point = extractPoint(lines)
    if (point != null) unionInput = point
    linesGeom.union(unionInput)
  }

  @throws[IOException]
  private def getFeatures(aFile: File, filter: Function[SimpleFeature, Boolean]) = {
    val store = new ShapefileDataStore(aFile.toURI.toURL)
    val array = new util.ArrayList[Geometry]
    val reader = store.getFeatureReader
    while (reader.hasNext) {
      val feature = reader.next
      if (filter.apply(feature)) array.add(feature.getDefaultGeometry.asInstanceOf[Geometry])
    }
    reader.close()
    store.dispose()
    array
  }

  @throws[MalformedURLException]
  @throws[IOException]
  @throws[SchemaException]
  private def addFeatures(p: Polygonizer, inputFeatures: List[Geometry]): Unit = {
    if (DEBUG) System.out.println(Calendar.getInstance.getTime + " node lines")
    //    val reduced = new util.ArrayList[Geometry]
    //    for (g <- inputFeatures) {
    //      reduced.add(GeometryPrecisionReducer.reduce(g, new PrecisionModel(100)))
    //    }
    val reduced = inputFeatures.map(g=>Option(g)).filter(_.isDefined).map(g=>GeometryPrecisionReducer.reduce(g.get, new PrecisionModel(100)))
    // extract linear components from input geometries
    val lines = getLines(reduced)
    // node all geometries together
    val nodedLines = nodeLines(lines.toList) match {
      case mls: MultiLineString => // noding a second time to be sure
        val geoms = (0 until mls.getNumGeometries).toList.map(i => mls.getGeometryN(i))
        nodeLines(geoms)
      case g: Geometry => g
    }
    if (DEBUG) System.out.println(Calendar.getInstance.getTime + " insert lines")
    p.add(nodedLines)
  }

  @SuppressWarnings(Array("unchecked"))
  @throws[IOException]
  @throws[SchemaException]
  def getPolygons(features: List[Geometry]): List[Polygon] = {
    val polygonizer = new Polygonizer
    addFeatures(polygonizer, features)
    if (DEBUG) System.out.println(Calendar.getInstance.getTime + " now with the real stuff")
    val result = polygonizer.getPolygons.asScala.map(_.asInstanceOf[Polygon])
    if (DEBUG) System.out.println(Calendar.getInstance.getTime + " all done now")
    result.toList
  }

  def getPolygonsAndEdges(features: List[Geometry]): (List[Polygon],List[Geometry]) = {
    val polygonizer = new Polygonizer
    addFeatures(polygonizer, features)
    if (DEBUG) System.out.println(Calendar.getInstance.getTime + " now with the real stuff")
    val result = polygonizer.getPolygons.asScala.map(_.asInstanceOf[Polygon])
    val edges = result.filter(g=>features.count(_.contains(g.getInteriorPoint))==0).flatMap{p=>
      p.getCoordinates.dropRight(1).zipWithIndex.map{case (coord,i) =>
        val coord2 = p.getCoordinates()(i+1)
        val ls = fact.createLineString(Array(coord,coord2))
        val feat = features.filter{g=> g.distance(fact.createPoint(coord)) < 0.01 && g.distance(fact.createPoint(coord2)) < 0.01 }
        (ls, feat)
      }.groupBy(f=>f._2).map(x=>fact.createGeometryCollection(x._2.map(_._1)).union())
    }
    if (DEBUG) System.out.println(Calendar.getInstance.getTime + " all done now")
    (result.toList,edges.toList)
  }

  //  def getPolygonsAndCutEdges(features: List[Geometry]): (List[Polygon],List[LineString]) = {
  //    val graph = new PolygonizeGraph(fact)
  //    def addFeatures(inputFeatures: List[Geometry]) = {
  //      if (DEBUG) System.out.println(Calendar.getInstance.getTime + " node lines")
  //      //    val reduced = new util.ArrayList[Geometry]
  //      //    for (g <- inputFeatures) {
  //      //      reduced.add(GeometryPrecisionReducer.reduce(g, new PrecisionModel(100)))
  //      //    }
  //      val reduced = inputFeatures.map(g => GeometryPrecisionReducer.reduce(g, new PrecisionModel(100)))
  //      // extract linear components from input geometries
  //      val lines = getLines(reduced)
  //      // node all geometries together
  //      var nodedLines = nodeLines(lines)
  //      if (nodedLines.isInstanceOf[MultiLineString]) { // noding a second time to be sure
  //        val mls = nodedLines.asInstanceOf[MultiLineString]
  //        val geoms = (0 until mls.getNumGeometries).toList.map(i => mls.getGeometryN(i))
  //        nodedLines = nodeLines(geoms)
  //      }
  //      if (DEBUG) System.out.println(Calendar.getInstance.getTime + " insert lines")
  //      graph.addEdge(nodedLines.asInstanceOf[LineString])
  //    }
  //    addFeatures(features)
  //    if (DEBUG) System.out.println(Calendar.getInstance.getTime + " now with the real stuff")
  //    val result = graph.getEdges
  ////    getPolygons.toList.map(_.asInstanceOf[Polygon])
  ////    val cutEdges = polygonizer.getCutEdges.toList.map(_.asInstanceOf[LineString])
  //    if (DEBUG) System.out.println(Calendar.getInstance.getTime + " all done now")
  //    // for (Polygon p : result)
  //    // System.out.println(p);
  //    // System.out.println(Calendar.getInstance().getTime() + " all done now");
  //    (result,cutEdges)
  //  }

  @throws[IOException]
  @throws[SchemaException]
  def getPolygons(files: Array[File]): List[Polygon] = {
    val features = new util.ArrayList[Geometry]
    for (file <- files) {
      if (DEBUG) System.out.println(Calendar.getInstance.getTime + " handling " + file)
      features.addAll(getFeatures(file, _ => true))
    }
    if (DEBUG) System.out.println(Calendar.getInstance.getTime + " adding features")
    getPolygons(features.asScala.toList)
  }

  @throws[IOException]
  @throws[SchemaException]
  def getPolygons(sFC: SimpleFeatureCollection): List[Polygon] = {
    val features = new util.ArrayList[Geometry]
    val sFCit = sFC.features
    try while ( {
      sFCit.hasNext
    }) features.add(sFCit.next.getDefaultGeometry.asInstanceOf[Geometry])
    catch {
      case problem: Exception =>
        problem.printStackTrace()
    } finally sFCit.close()
    if (DEBUG) System.out.println(Calendar.getInstance.getTime + " adding features")
    getPolygons(features.asScala.toList)
  }
}
