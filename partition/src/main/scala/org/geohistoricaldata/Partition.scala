package org.geohistoricaldata

import better.files.File
import org.locationtech.jts.geom.{Geometry, MultiPolygon, Polygon, PrecisionModel}
import org.locationtech.jts.precision.GeometryPrecisionReducer
import org.opengis.feature.simple.SimpleFeature

import java.io.{File => JFile}
import java.nio.file.Path

object Partition extends App {
  val pm = new PrecisionModel(100)

  def reduce(g: Geometry) = GeometryPrecisionReducer.reduce(g, pm)

  def getPolygons(geom: Geometry): Array[Polygon] = geom match {
    case p: Polygon => Array(p)
    case mp: MultiPolygon => Array.tabulate(mp.getNumGeometries)(mp.getGeometryN).map(_.asInstanceOf[Polygon])
  }

  def getFeaturePolygons(f: SimpleFeature): Array[Polygon] = Option(f.getDefaultGeometry) match {
    case Some(g) => getPolygons(reduce(g.asInstanceOf[Geometry]))
    case None => Array()
  }

  val inputDir = File("../input_l93")
  def path(name: String) = (inputDir / (name + ".shp")).path

  // get the geometry of the target area (5th arrondissement)
  val arrondissement = Utils.getShapefile(path("arrondissements")).filter(_.getAttribute("c_ar").toString.toInt == 5).head.getDefaultGeometry.asInstanceOf[Geometry]
  val arrondissementGeom = reduce(arrondissement)

  def get(path: Path) = Utils.getShapefile(path).flatMap(getFeaturePolygons).filter(_.intersects(arrondissementGeom))

  def getWithAttributes(path: Path, attributes: Array[String]) = Utils.getShapefile(path).flatMap(f=>getFeaturePolygons(f).map(p=>(p, attributes.map(a=>f.getAttribute(a))))).filter(_._1.intersects(arrondissementGeom))

  //  val chaussee = Utils.getShapefile(new JFile("/home/julien/devel/ICI_Pedestrian/input_l93/plan-de-voirie-chaussees.shp").toPath).flatMap(getFeaturePolygons).filter(_.intersects(arrondissementGeom))
  val chaussee = getWithAttributes(path("plan-de-voirie-chaussees"), Array("objectid"))
  val chausseeGeom = reduce(arrondissementGeom.getFactory.createGeometryCollection(chaussee.map(_._1).toArray).union())

  // check that a geometry does not intersect the street (remove the 'quais de seine')
  def notOnStreet(g: Geometry) = !g.intersects(chausseeGeom) || g.intersection(chausseeGeom).getArea < 1.0

  val trottoirs = getWithAttributes(path("plan-de-voirie-trottoirs-emprises"), Array("objectid")).filter(x=>notOnStreet(x._1))
  val passages = getWithAttributes(path("plan-de-voirie-passages-pietons"), Array("objectid"))
  val mixte = getWithAttributes(path("plan-de-voirie-aires-mixtes-vehicules-et-pietons"), Array("objectid"))
  val ilots = getWithAttributes(path("plan-de-voirie-ilots-directionnels"), Array("objectid"))
  val terre_pleins = getWithAttributes(path("plan-de-voirie-terre-pleins"), Array("objectid"))
  val escaliers = getWithAttributes(path("plan-de-voirie-voies-en-escalier"), Array("objectid"))
//  val batiments = getWithAttributes(path("volumesbatis"),Array("h_et_max"))
  val espacesVerts = getWithAttributes(path("plan-de-voirie-emprises-espaces-verts"), Array("objectid"))
  val batimentsIGN = getWithAttributes(path("BATIMENT_XY"), Array("HAUTEUR", "NB_ETAGES", "NB_LOGTS", "ID"))
  val inputFeatures = (chaussee ++ trottoirs ++ passages ++ mixte ++ ilots ++ terre_pleins ++ escaliers ++ batimentsIGN).toList.map(_._1)
//  val batimentsIGN_seq = batimentsIGN.map(_._1)
  val features = FeaturePolygonizer.getPolygons(inputFeatures).map {
    p =>
      val point = p.getInteriorPoint
      case class PartitionType(name: String, seq: Seq[(Polygon, Array[AnyRef])])
      val typeCollection = Array(
        PartitionType("batiment", batimentsIGN),
        PartitionType("trottoir", trottoirs),
        PartitionType("passage piéton", passages),
        PartitionType("aire mixte véhicules et piétons", mixte),
        PartitionType("ilot directionnel", ilots),
        PartitionType("terre-plein", terre_pleins),
        PartitionType("voie en escalier", escaliers),
        PartitionType("chaussée", chaussee),
        PartitionType("espace vert", espacesVerts)
      )
      // for each polygon, get the first partition type that matches
      typeCollection.find(_.seq.exists(_._1.contains(point))) match {
        case Some(pt) => pt.name match {
          case n if n.equals("batiment") =>
            // if it is a building, add the height, etc.
            val attributs = batimentsIGN.find(_._1.contains(point)).get._2

            def getAsInt(v: AnyRef) = Option(v) match {
              case Some(x) => x.toString.toInt
              case None => 0
            }
            (p, n, attributs.head.asInstanceOf[Double], getAsInt(attributs(1)), getAsInt(attributs(2)), attributs(3).toString)
          case n if n.equals("trottoir") || n.equals("ilot directionnel") || n.equals("terre-plein") =>
            val attributs = pt.seq.find(_._1.contains(point)).get._2
            (p, n, 0.1, 0, 0, attributs.head.toString) // arbitrary sidewalk height
          case n =>
            val attributs = pt.seq.find(_._1.contains(point)).get._2
            (p, n, 0.0, 0, 0, attributs.head.toString)
        }
        case None => (p, "autre", 0.0, 0, 0, "NONE")
      }
  }

  val outputDir = File("../output")
  Utils.createShapefile(
    new JFile(outputDir + "/partition.shp"),
    "geom:Polygon:srid=2154,label:String,HAUTEUR:Double,NB_ETAGES:Integer,NB_LOGTS:Integer,objectid:String",
    features.filter(_._1.intersects(arrondissementGeom)).map(g => Array[AnyRef](g._1, g._2, java.lang.Double.valueOf(g._3), java.lang.Integer.valueOf(g._4), java.lang.Integer.valueOf(g._5), g._6)))
  Utils.createShapefile(
    new JFile(outputDir + "/partition_points.shp"),
    "geom:Point:srid=2154,label:String,HAUTEUR:Double,NB_ETAGES:Integer,NB_LOGTS:Integer,objectid:String",
    features.filter(_._1.intersects(arrondissementGeom)).map(g => Array[AnyRef](g._1.getInteriorPoint, g._2, java.lang.Double.valueOf(g._3), java.lang.Integer.valueOf(g._4), java.lang.Integer.valueOf(g._5), g._6)))

}
