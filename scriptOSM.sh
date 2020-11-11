#!/bin/bash
# make sure to install: npm install -g osmtogeojson
mkdir osm
cd osm
curl --globoff -o amenties.osm  "https://overpass-api.de/api/interpreter?data=[bbox:48.8374,2.3352,48.8539,2.3719];node[%22amenity%22];out;"
# transport related
curl --globoff -o voirie.osm "https://overpass-api.de/api/interpreter?data=[bbox:48.8374,2.3352,48.8539,2.3719];(nwr[%22highway%22][%22foot%22!=%22no%22];nwr[%22building%22=%22train_station%22];);(._;>;);out;"

osmtogeojson amenties.osm > amenites.geojson
osmtogeojson voirie.osm > voirie.geojson

# wrong column names
#ogr2ogr -f "GeoJSON" res.geojson voirie.geojson -dialect sqlite -sql "SELECT * FROM voirie polygon,'envelope.geojson'.envelope polygon WHERE ST_Intersects(voirie.geometry, envelope.geometry)"
