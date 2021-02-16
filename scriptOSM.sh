#!/bin/bash
# make sure to install: npm install -g osmtogeojson
#mkdir osm
#cd osm
#curl --globoff -o amenties.osm  "https://overpass-api.de/api/interpreter?data=[bbox:48.8374,2.3352,48.8539,2.3719];node[%22amenity%22];out;"
# transport related
#curl --globoff -o voirie.osm "https://overpass-api.de/api/interpreter?data=[bbox:48.8374,2.3352,48.8539,2.3719];(nwr[%22highway%22][%22foot%22!=%22no%22];nwr[%22building%22=%22train_station%22];);(._;>;);out;"

osmtogeojson amenties.osm > amenites.geojson
#osmtogeojson voirie.osm > voirie.geojson

# wrong column names
#ogr2ogr -f "GeoJSON" res.geojson voirie.geojson -dialect sqlite -sql "SELECT * FROM voirie polygon,'envelope.geojson'.envelope polygon WHERE ST_Intersects(voirie.geometry, envelope.geometry)"

#https://geodatamine.fr/data/bank/-20873

#work with geodatamine

BASE_URL='https://geodatamine.fr/data'
OPTS='-20873?format=geojson'
mkdir osm
mkdir osm/input
#mkdir input_l93
mkdir osm/output

getData () {
list=$2[@]
l=("${!list}")
mkdir -p osm/output/$1
mkdir -p osm/input/$1
#mkdir -p input_l93/$1
for layer in "${l[@]}"
do
    # get the data from opendata paris
    wget ${BASE_URL}/${layer}/${OPTS} -O osm/input/$1/${layer}.geojson
    ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f "GPKG" osm/output/$1/${layer}.gpkg "osm/input/$1/${layer}.geojson"
 #   unzip input/$1/${layer} -d input/$1/ 
 #   rm input/$1/${layer}
    # reproject everything to lambert93 (EPSG:2154)
  #  ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f ESRI Shapefile input_l93/$1/${layer}.shp input/$1/${layer}.shp
    # rasterize
  #  gdal_rasterize -at -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/$1/${layer}.geojson output/$1/${layer}.tif
done
}


declare -a lType 

lType=(bank cemetery cinema drinking_water education fuel healthcare hosting library playground public_service shop_craft_office sports toilets)
getData "OSM-POI" lType

lType=(bicycle_parking carpool charging_station cycleway parking)
getData "transport" lType
