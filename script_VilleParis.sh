#!/bin/bash

# get the data from opendata paris
BASE_URL='https://opendata.paris.fr/explore/dataset'
OPTS='download/?format=geojson&timezone=Europe/Berlin&lang=fr'
mkdir input
mkdir input_l93
mkdir output

getData () {
list=$2[@]
l=("${!list}")
mkdir -p input_l93/$1
mkdir -p output/$1
mkdir -p input/$1
for layer in "${l[@]}"
do
    # get the data from opendata paris
    wget ${BASE_URL}/${layer}/${OPTS} -O input/$1/${layer}.geojson
    # reproject everything to lambert93 (EPSG:2154)
    ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f GeoJSON input_l93/$1/${layer}.geojson input/$1/${layer}.geojson
    # rasterize
    gdal_rasterize -at -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/$1/${layer}.geojson output/$1/${layer}.tif
done
}

declare -a lType 
lType=(plan-de-voirie-aires-mixtes-vehicules-et-pietons plan-de-voirie-chaussees plan-de-voirie-terre-pleins)
getData "voirie-voiture" lType
lType=(plan-de-voirie-aires-mixtes-vehicules-et-pietons plan-de-voirie-passages-pietons plan-de-voirie-emprises-espaces-verts plan-de-voirie-voies-privees-fermees plan-de-voirie-voies-en-escalier plan-de-voirie-trottoirs-emprises)
getData "voirie-pieton" lType
lType=(reseau-cyclable plan-de-voirie-pistes-cyclables-et-couloirs-de-bus  velib-emplacement-des-stations)
getData "voirie-cycle" lType
lType=(secteurs-des-bureaux-de-vote-en-2017 arrondissements)
getData "population" lType
lType=(espaces_verts plan-de-voirie-emprises-ferroviaires)
getData "occupationSol" lType
lType=(secteurs-scolaires-ecoles-elementaires secteurs-scolaires-maternelles secteurs-scolaires-colleges)
getData "ecole" lType
lType=(plan-de-voirie-acces-pietons-metro-et-parkings plan-de-voirie-mobiliers-urbains-abris-voyageurs-points-darrets-bus)
getData "transport" lType
# merge the different layers in a single tif
# with all layers
voirieFiles=(./output/voirie-pieton/*.tif)
gdal_merge.py -o all_layers_voirie.tif $voirieFiles

voirieFilesNoGreen=(./output/voirie-pieton/*.tif --ignore={*verts*})
# without green spaces
gdal_merge.py -o all_except_green.tif $voirieFilesNoGreen

# get the activity data
#https://geodatamine.fr/data/shop_craft_office/-71525?format=csv&aspoint=true&metadata=true
# transform to gpkg
#ogr2ogr -f GPKG shops.gpkg data.csv -oo X_POSSIBLE_NAMES=X -oo Y_POSSIBLE_NAMES=Y -s_srs 'EPSG:4326' -t_srs EPSG:2154
