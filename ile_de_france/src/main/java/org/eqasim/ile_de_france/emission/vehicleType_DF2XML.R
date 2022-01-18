# convert vehicle type from csv/xlsx to xml format
# add Hbefa vehicle attributes
# author: Biao Yin , Nov. 2021



library(XML)
library(readr)
library(readxl)

# data preparation 
persons <- read_delim("MATSIM/Project/R/marjolaine/0.1pct/persons.csv", 
                      ";", escape_double = FALSE, trim_ws = TRUE)
persons_1 <- persons[, c("hts_id","person_id")]
persons_1 <- persons_1[order(persons_1$hts_id, decreasing = FALSE), ]

Parc_auto <- read_excel("MATSIM/Project/R/marjolaine/Parc_auto.xlsx")
Parc_auto_1 <- Parc_auto[, c("EGT_id","subsegment")]
Parc_auto_1  <-  Parc_auto_1 [order(Parc_auto_1$EGT_id, decreasing = FALSE), ]

persons_parc_auto <- cbind(persons_1, Parc_auto_1) 
persons_parc_auto <- persons_parc_auto[, c('person_id', "subsegment")]


veh_type <- levels(factor(persons_parc_auto$subsegment))
hbefa_info <- data.frame("HbefaVehicleCategory" = "PASSENGER_CAR",
                         "HbefaTechnology" = c("petrol (4S)", "diesel"),
                         "HbefaSizeClass" = c(">=2L", "<1,4L"))

hbefa_info2 <- data.frame(NULL, row.names = NULL)
for (i in 1:length(veh_type)) {
  if (grepl("petrol", veh_type[i],  fixed = TRUE)) {
    hbefa_info2 <- rbind(hbefa_info2, cbind(hbefa_info[1, ], "HbefaEmissionsConcept"=veh_type[i]))
  } else{
    hbefa_info2 <- rbind(hbefa_info2, cbind(hbefa_info[2, ], "HbefaEmissionsConcept"=veh_type[i]))
  }
}

xml <- xmlTree("vehicleDefinitions", namespaces= list("http://www.matsim.org/files/dtd", xsi="http://www.w3.org/2001/XMLSchema-instance"), attrs = c('xsi:schemaLocation' = "http://www.matsim.org/files/dtd http://www.matsim.org/files/dtd/vehicleDefinitions_v2.0.xsd"))

# add hbefa vechile attributes info
for (i in 1:length(veh_type)) {
  xml$addTag("vehicleType", close=FALSE, attrs=c(id = veh_type[i]))
  
  xml$addTag("length",  attrs=c(meter= "7.5"))
  xml$addTag("width",  attrs=c(meter= "1.0"))
  
  xml$addTag("engineInformation", close=FALSE)
  xml$addTag("attributes", close=FALSE)
  
  pos <- which( hbefa_info2$HbefaEmissionsConcept ==  veh_type[i], arr.ind=TRUE)
  for (j in 1:ncol(hbefa_info2)) {
    xml$addTag("attribute", hbefa_info2[pos, names(hbefa_info2)[j]], close=FALSE, attrs=c(name= names(hbefa_info2)[j], class = "java.lang.String"))
    xml$closeTag()
  }

  
  xml$closeTag()
  xml$closeTag()
  
  xml$closeTag()
}

# add vehicle type ID info: attention run 5 min
for (i in 1:nrow(persons_parc_auto[, ])) {
  xml$addTag("vehicle", close=FALSE, attrs=c(id= persons_parc_auto[i, ]$person_id, type=persons_parc_auto[i, ]$subsegment))
  xml$closeTag()
}
xml$closeTag()

# save file
cat(saveXML(xml, indent=TRUE, encoding = "UTF-8", prefix = '<?xml version="1.0" encoding="UTF-8"?>\n'), file = "C:/Users/biao.yin/Documents/MATSIM/Project/R/marjolaine/output_vehicles_modified.xml")


