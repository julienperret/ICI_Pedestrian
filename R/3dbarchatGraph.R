library(ggplot2)
#library(latticeExtra)

conservation_status <- c(
  JOHV = "Jours ouvrés hors vacances scolaires",
  JOVS = "Jours ouvrés vacances scolaires",
  SAHV = "Samedi hors vacances scolaires",
  DIJFP = "Dimanche et jour feriée",
  SAVS = "Samedi vacances scolaires"
)
path_to_Data_dir <-  "./paris/transport/stat/"

subdirs <-  list.files(path_to_Data_dir, full.names = F)[-1]
for (current_file in subdirs){
  filename = basename(paste0(current_file,".png"))
  tab <- read.csv(paste0(path_to_Data_dir,current_file))
  plot <- ggplot(tab, aes(x=StartHour, y=NumberOfValidation))+
  geom_bar(stat='identity', fill="forest green")+
  #scale_x_discrete(labels=~Hour)+
  theme(axis.text.x = element_text(angle=45))+
  facet_wrap(~TypeOfDay,  ncol=1,labeller = labeller(TypeOfDay = conservation_status)
  )
 direct <- paste0(path_to_Data_dir,"graph")
 dir.create(direct, showWarnings = FALSE)
  ggsave(path = direct,filename = filename, plot)#, device = format, width = 20, height = 10, units = "cm")
}


#cloud(Number.of.validaiton~Hour+Type.of.day, tab, panel.3d.cloud=panel.3dbars, col.facet='grey', 
 #     xbase=0.1, ybase=0.1, scales=list(arrows=FALSE, col=1), 
  #    par.settings = list(axis.line = list(col = "transparent")), 
   #   perspective=TRUE
    #  )
