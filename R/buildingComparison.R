library(ggplot2)
library(dplyr)
library(tidyr)
library(sf)

pathOutGraph <- ""
pathData <- ""

# generation of comparison map between INSEE and IGN data
library(cartography)
irisStat <- st_read(paste0(pathData,"ICI/IrisStat.gpkg"))
irisStat$diffSimCount <- irisStat$nbSimulatedHousing - irisStat$nbTotalHousing
colors <- carto.pal(pal1 = "green.pal",n1= 3, pal2="red.pal", n2=3, transparency = TRUE)
choroLayer(x = irisStat , var = "diffSimCount", method = "jenks", nclass = 6, col = colors, legend.pos = "right", legend.values.rnd = 0
           , legend.title.txt = "Difference entre le nombre de logements\ndes bâtiments provenant de l'IGN et\nles logements par IRIS de l'INSEE",legend.title.cex = 0.55, legend.values.cex= 0.45, legend.horiz = FALSE )




# generation of comparison graph between classes 

statBuilding <- read.csv(paste0(pathData,"ICI/statBuilding.csv"))

bplot <- statBuilding %>% pivot_longer(c("totalHousing","totalHousingSimulated"),names_to = "Source", values_to = "NumberOfHousing") 
pgen <- ggplot(bplot, aes(fill=Source,y=NumberOfHousing, x=IrisName)) + 
  geom_bar(position="dodge", stat="identity") + 
  theme(axis.text.x = element_text(angle = 90))  
ggsave(pgen, filename = "statot.svg",path=pathOutGraph, device = "svg", width = 200, height = 150,  units = "mm")

bplot <- statBuilding %>% pivot_longer(c("inf30sqmReadjusted","inf30sqmSimulated"),names_to = "Source", values_to = "NumberOfHousinginf30sqm") 
pgen <- ggplot(bplot, aes(fill=Source,y=NumberOfHousinginf30sqm, x=IrisName)) + 
  geom_bar(position="dodge", stat="identity") + 
  theme(axis.text.x = element_text(angle = 90))  
ggsave(pgen, filename = "inf30sqm.svg",path=pathOutGraph, device = "svg", width = 200, height = 150,  units = "mm")




bplot <- statBuilding %>% pivot_longer(c("X30_40sqmReadjusted","X30_40sqmSimulated"),names_to = "Source", values_to = "NumberOfHousing30_40sqm") 
pgen <- ggplot(bplot, aes(fill=Source,y=NumberOfHousing30_40sqm, x=IrisName)) + 
  geom_bar(position="dodge", stat="identity") + 
  theme(axis.text.x = element_text(angle = 90))  
ggsave(pgen, filename = "30_40sqm.svg",path=pathOutGraph, device = "svg", width = 200, height = 150,  units = "mm")


bplot <- statBuilding %>% pivot_longer(c("X40_60sqmReadjusted","X40_60sqmSimulated"),names_to = "Source", values_to = "NumberOfHousing40_60sqm") 
pgen <- ggplot(bplot, aes(fill=Source,y=NumberOfHousing40_60sqm, x=IrisName)) + 
  geom_bar(position="dodge", stat="identity") + 
  theme(axis.text.x = element_text(angle = 90))  
ggsave(pgen, filename = "40_60sqm.svg",path=pathOutGraph, device = "svg", width = 200, height = 150,  units = "mm")




bplot <- statBuilding %>% pivot_longer(c("X60_80sqmReadjusted","X60_80sqmSimulated"),names_to = "Source", values_to = "NumberOfHousing60_80sqm") 
pgen <- ggplot(bplot, aes(fill=Source,y=NumberOfHousing60_80sqm, x=IrisName)) + 
  geom_bar(position="dodge", stat="identity") + 
  theme(axis.text.x = element_text(angle = 90))  
ggsave(pgen, filename = "60_80sqmReadjusted.svg",path=pathOutGraph, device = "svg", width = 200, height = 150,  units = "mm")




bplot <- statBuilding %>% pivot_longer(c("X80_100sqmReadjusted","X80_100sqmSimulated"),names_to = "Source", values_to = "NumberOfHousing80_100sqm") 
pgen <- ggplot(bplot, aes(fill=Source,y=NumberOfHousing80_100sqm, x=IrisName)) + 
  geom_bar(position="dodge", stat="identity") + 
  theme(axis.text.x = element_text(angle = 90))  
ggsave(pgen, filename = "80_100sqmReadjusted.svg",path=pathOutGraph, device = "svg", width = 200, height = 150,  units = "mm")




bplot <- statBuilding %>% pivot_longer(c("X100_120sqmReadjusted","X100_120sqmSimulated"),names_to = "Source", values_to = "NumberOfHousing100_120sqm") 
pgen <- ggplot(bplot, aes(fill=Source,y=NumberOfHousing100_120sqm, x=IrisName)) + 
  geom_bar(position="dodge", stat="identity") + 
  theme(axis.text.x = element_text(angle = 90))  
ggsave(pgen, filename = "100_120sqmReadjusted.svg",path=pathOutGraph, device = "svg", width = 200, height = 150,  units = "mm")




bplot <- statBuilding %>% pivot_longer(c("Sup120sqmReadjusted","Sup120sqmSimulated"),names_to = "Source", values_to = "NumberOfHousingSup120sqm") 
pgen <- ggplot(bplot, aes(fill=Source,y=NumberOfHousingSup120sqm, x=IrisName)) + 
  geom_bar(position="dodge", stat="identity") + 
  theme(axis.text.x = element_text(angle = 90))  
ggsave(pgen, filename = "Sup120sqmReadjusted.svg",path=pathOutGraph, device = "svg", width = 200, height = 150,  units = "mm")



# correlation coefficient between number of POI/working place and room left in buildings
library(dplyr)
bat <- st_read(paste0(pathData,"ICI/building.gpkg"))
batHousing <- filter(bat, bat$roomLeft >0)
cor(batHousing$nbWorkingPlace, batHousing$roomLeft)
# it's bad : no correlation (not abnormal)

# robustness of empty room
bVar <- st_read(paste0(pathData,"ICI/buildingVariability.gpkg"))
colorsBVar <- carto.pal(pal1 = "red.pal",n1= 5)
choroLayer(x = bVar , var = "variationCoefficient", method = "jenks", nclass = 5, col = colorsBVar, legend.pos = "right", legend.values.rnd = 0
           , legend.title.txt = "Difference entre le nombre de logements\ndes bâtiments provenant de l'IGN et\nles logements par IRIS de l'INSEE",legend.title.cex = 0.55, legend.values.cex= 0.45, legend.horiz = FALSE )
# correlation between variability and small building
cor(x=bVar$variationCoefficient, y=st_area(bVar$geom)) # return NA

