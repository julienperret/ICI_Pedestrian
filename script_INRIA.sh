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

gdal_merge.py -o all_layers.tif output/plan-de-voirie-ilots-directionnels.tif output/plan-de-voirie-aires-mixtes-vehicules-et-pietons.tif output/plan-de-voirie-emprises-espaces-verts.tif output/plan-de-voirie-passages-pietons.tif output/plan-de-voirie-terre-pleins.tif output/plan-de-voirie-trottoirs-emprises.tif output/plan-de-voirie-voies-en-escalier.tif
gdal_merge.py -o all_except_green.tif output/plan-de-voirie-ilots-directionnels.tif output/plan-de-voirie-aires-mixtes-vehicules-et-pietons.tif output/plan-de-voirie-passages-pietons.tif output/plan-de-voirie-terre-pleins.tif output/plan-de-voirie-trottoirs-emprises.tif output/plan-de-voirie-voies-en-escalier.tif

exit 0
# trottoirs
wget ${BASE_URL}/plan-de-voirie-trottoirs-emprises/${OPTS} -O input/plan-de-voirie-trottoirs-emprises.geojson
# passages piétons
wget ${BASE_URL}/plan-de-voirie-passages-pietons/${OPTS} -O input/plan-de-voirie-passages-pietons.geojson
# aires mixtes véhicules et piétons
wget ${BASE_URL}/plan-de-voirie-aires-mixtes-vehicules-et-pietons/${OPTS} -O input/plan-de-voirie-aires-mixtes-vehicules-et-pietons.geojson
# emprises des espaces verts
wget ${BASE_URL}/plan-de-voirie-emprises-espaces-verts/${OPTS} -O input/plan-de-voirie-emprises-espaces-verts.geojson
# ilots directionnels
wget ${BASE_URL}/plan-de-voirie-ilots-directionnels/${OPTS} -O input/plan-de-voirie-ilots-directionnels.geojson
# terre-pleins
wget ${BASE_URL}/plan-de-voirie-terre-pleins/${OPTS} -O input/plan-de-voirie-terre-pleins.geojson
# voies en escalier
wget ${BASE_URL}/plan-de-voirie-voies-en-escalier/${OPTS} -O input/plan-de-voirie-voies-en-escalier.geojson
# arrondissements
wget ${BASE_URL}/arrondissements/${OPTS} -O input/arrondissements.geojson

# reproject everything to lambert93 (EPSG:2154)
mkdir input_l93
# trottoirs
ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f GeoJSON input_l93/plan-de-voirie-trottoirs-emprises_l93.geojson input/plan-de-voirie-trottoirs-emprises.geojson
# passages piétons
ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f GeoJSON input_l93/plan-de-voirie-passages-pietons_l93.geojson input/plan-de-voirie-passages-pietons.geojson
# aires mixtes véhicules et piétons
ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f GeoJSON input_l93/plan-de-voirie-aires-mixtes-vehicules-et-pietons_l93.geojson input/plan-de-voirie-aires-mixtes-vehicules-et-pietons.geojson
# emprises des espaces verts
ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f GeoJSON input_l93/plan-de-voirie-emprises-espaces-verts_l93.geojson input/plan-de-voirie-emprises-espaces-verts.geojson
# ilots directionnels
ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f GeoJSON input_l93/plan-de-voirie-ilots-directionnels_l93.geojson input/plan-de-voirie-ilots-directionnels.geojson
# terre-pleins
ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f GeoJSON input_l93/plan-de-voirie-terre-pleins_l93.geojson input/plan-de-voirie-terre-pleins.geojson
# voies en escalier
ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f GeoJSON input_l93/plan-de-voirie-voies-en-escalier_l93.geojson input/plan-de-voirie-voies-en-escalier.geojson
# arrondissements
ogr2ogr -s_srs EPSG:4326 -t_srs EPSG:2154 -f GeoJSON input_l93/arrondissements_l93.geojson input/arrondissements.geojson

# rasterize
gdal_rasterize -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-ilots-directionnels_l93.geojson plan-de-voirie-ilots-directionnels.tif
gdal_rasterize -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-aires-mixtes-vehicules-et-pietons_l93.geojson plan-de-voirie-aires-mixtes-vehicules-et-pietons.tif
gdal_rasterize -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-emprises-espaces-verts_l93.geojson plan-de-voirie-emprises-espaces-verts.tif
gdal_rasterize -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-passages-pietons_l93.geojson plan-de-voirie-passages-pietons.tif
gdal_rasterize -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-terre-pleins_l93.geojson plan-de-voirie-terre-pleins.tif
gdal_rasterize -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-trottoirs-emprises_l93.geojson plan-de-voirie-trottoirs-emprises.tif
gdal_rasterize -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-voies-en-escalier_l93.geojson plan-de-voirie-voies-en-escalier.tif

gdal_rasterize -at -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-ilots-directionnels_l93.geojson plan-de-voirie-ilots-directionnels.tif
gdal_rasterize -at -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-aires-mixtes-vehicules-et-pietons_l93.geojson plan-de-voirie-aires-mixtes-vehicules-et-pietons.tif
gdal_rasterize -at -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-emprises-espaces-verts_l93.geojson plan-de-voirie-emprises-espaces-verts.tif
gdal_rasterize -at -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-passages-pietons_l93.geojson plan-de-voirie-passages-pietons.tif
gdal_rasterize -at -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-terre-pleins_l93.geojson plan-de-voirie-terre-pleins.tif
gdal_rasterize -at -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-trottoirs-emprises_l93.geojson plan-de-voirie-trottoirs-emprises.tif
gdal_rasterize -at -burn 255 -a_nodata 0.0 -tr 1.0 1.0 -te 651000 6859000 654000 6862000 -ot Byte -of GTiff input_l93/plan-de-voirie-voies-en-escalier_l93.geojson plan-de-voirie-voies-en-escalier.tif

# merge
gdal_merge.py -o all_layers.tif plan-de-voirie-ilots-directionnels.tif plan-de-voirie-aires-mixtes-vehicules-et-pietons.tif  plan-de-voirie-emprises-espaces-verts.tif plan-de-voirie-passages-pietons.tif plan-de-voirie-terre-pleins.tif plan-de-voirie-trottoirs-emprises.tif plan-de-voirie-voies-en-escalier.tif
gdal_merge.py -o all_except_green.tif plan-de-voirie-ilots-directionnels.tif plan-de-voirie-aires-mixtes-vehicules-et-pietons.tif plan-de-voirie-passages-pietons.tif plan-de-voirie-terre-pleins.tif plan-de-voirie-trottoirs-emprises.tif plan-de-voirie-voies-en-escalier.tif

# get the activity data
#https://geodatamine.fr/data/shop_craft_office/-71525?format=csv&aspoint=true&metadata=true
# transform to gpkg
#ogr2ogr -f GPKG shops.gpkg data.csv -oo X_POSSIBLE_NAMES=X -oo Y_POSSIBLE_NAMES=Y -s_srs 'EPSG:4326' -t_srs EPSG:2154
