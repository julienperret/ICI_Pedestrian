#!/bin/bash
# make sure to install: npm install -g osmtogeojson
mkdir osm
cd osm
curl --globoff -o amenties.osm  "https://overpass-api.de/api/interpreter?data=[bbox:48.8374,2.3352,48.8526,2.3719];node[%22amenity%22];out;"
curl --globoff -o voirie.osm "https://overpass-api.de/api/interpreter?data=[bbox:48.8374,2.3352,48.8526,2.3719];nwr[\"highway\"][\"foot\"!=\"no\"];(._;>;);out;"

osmtogeojson amenties.osm > amenites.geojson
osmtogeojson voirie.osm > voirie.geojson

# wrong column names
#ogr2ogr -f "GeoJSON" res.geojson voirie.geojson -dialect sqlite -sql "SELECT * FROM voirie polygon,'envelope.geojson'.envelope polygon WHERE ST_Intersects(voirie.geometry, envelope.geometry)"
