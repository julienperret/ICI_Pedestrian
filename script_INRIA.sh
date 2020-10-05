#!/bin/bash

# get the data from opendata paris
BASE_URL='https://opendata.paris.fr/explore/dataset'
OPTS='download/?format=geojson&timezone=Europe/Berlin&lang=fr'
mkdir input
mkdir input_l93
mkdir output

for layer in plan-de-voirie-trottoirs-emprises plan-de-voirie-passages-pietons plan-de-voirie-aires-mixtes-vehicules-et-pietons plan-de-voirie-emprises-espaces-verts plan-de-voirie-ilots-directionnels plan-de-voirie-terre-pleins plan-de-voirie-voies-en-escalier arrondissements
do
    # get the data from opendata paris
    wget ${BASE_URL}/${layer}/${OPTS} -O input/${layer}.geojson
    # reproject everything to lambert93 (EPSG:2154)
    ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f GeoJSON input_l93/${layer}.geojson input/${layer}.geojson
    # rasterize
    gdal_rasterize -at -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/${layer}.geojson output/${layer}.tif
done

# merge the different layers in a single tif
# with all layers
gdal_merge.py -o all_layers.tif output/plan-de-voirie-ilots-directionnels.tif output/plan-de-voirie-aires-mixtes-vehicules-et-pietons.tif output/plan-de-voirie-emprises-espaces-verts.tif output/plan-de-voirie-passages-pietons.tif output/plan-de-voirie-terre-pleins.tif output/plan-de-voirie-trottoirs-emprises.tif output/plan-de-voirie-voies-en-escalier.tif
# without green spaces
gdal_merge.py -o all_except_green.tif output/plan-de-voirie-ilots-directionnels.tif output/plan-de-voirie-aires-mixtes-vehicules-et-pietons.tif output/plan-de-voirie-passages-pietons.tif output/plan-de-voirie-terre-pleins.tif output/plan-de-voirie-trottoirs-emprises.tif output/plan-de-voirie-voies-en-escalier.tif

# get the activity data
#https://geodatamine.fr/data/shop_craft_office/-71525?format=csv&aspoint=true&metadata=true
# transform to gpkg
#ogr2ogr -f GPKG shops.gpkg data.csv -oo X_POSSIBLE_NAMES=X -oo Y_POSSIBLE_NAMES=Y -s_srs 'EPSG:4326' -t_srs EPSG:2154
