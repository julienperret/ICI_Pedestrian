name := "partition"

version := "0.1"

scalaVersion := "2.13.4"

val geotoolsVersion = "24.1"

libraryDependencies ++= List(
  "org.locationtech.jts" % "jts-core" % "1.18.0",
  "org.apache.commons" % "commons-math3" % "3.6.1",
  "org.geotools" % "gt-referencing" % geotoolsVersion,
  "org.geotools" % "gt-shapefile" % geotoolsVersion,
  "org.geotools" % "gt-geopkg" % geotoolsVersion,
  "org.geotools" % "gt-epsg-hsql" % geotoolsVersion,
  "org.geotools" % "gt-cql" % geotoolsVersion,
  "org.geotools" % "gt-coverage" % geotoolsVersion,
  "org.geotools" % "gt-geopkg" % geotoolsVersion,
  "javax.media" % "jai_core" % "1.1.3",
  "javax.media" % "jai_codec" % "1.1.3",
  "javax.media" % "jai_imageio" % "1.1.1",
  "com.github.pathikrit" %% "better-files" % "3.9.1",
  "org.jgrapht" % "jgrapht-core" % "1.5.0",
  "com.github.scopt" %% "scopt" % "4.0.0"
 // "fr.ign.artiscales.tools" % "ArtiScales-tools" % "0.3-SNAPSHOT"
)

resolvers ++= List(
  "Scala Tools Snapshots" at "https://scala-tools.org/repo-snapshots/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "OpenGeo repo" at "https://repo.osgeo.org/repository/release/",
  "Bounless" at "https://repo.boundlessgeo.com/main/",
  "central" at "https://repo1.maven.org/maven2/",
  "geosolutions" at "https://maven.geo-solutions.it/",
  "geotoolkit" at "https://maven.geotoolkit.org/",
 // "Cogit Snapshots Repository" at "https://forge-cogit.ign.fr/nexus/content/repositories/snapshots/"
)
