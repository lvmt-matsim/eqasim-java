package org.eqasim.ile_de_france.emission;

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
//package org.matsim.ruhrgebiet.analysis;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.analysis.spatial.Grid;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionGridAnalyzer;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author amit, ihab
 */

public class GenerateAirPollutionSpatialPlots {
    private static final Logger log = Logger.getLogger(GenerateAirPollutionSpatialPlots.class);

/*    //smaller BoundingBox:
    private static final double xMin = 651683.10;
    private static final double yMin = 6868359.63;
    private static final double xMax = 652560.27;
    private static final double yMax = 6869288.39;*/

    private static final double gridSize = 1000.;
    private static final double smoothingRadius = 2000.;
    private static final double scaleFactor = 100.;

    final static String scenarioID = "ile_de_france_1pm";
    final static String outputPath = "./simulation_output/" + scenarioID;
//    final static String runFilePath = "./ile_de_france/src/main/java/org/eqasim/ile_de_france";

    private GenerateAirPollutionSpatialPlots() {

    }

    public static void main(String[] args) {

        GenerateAirPollutionSpatialPlots plots = new GenerateAirPollutionSpatialPlots();

//        JCommander.newBuilder().addObject(plots).build().parse(args);

        plots.writeEmissions();
    }

    private void writeEmissions() {

        final String configFile = outputPath + "/output_config.xml";
        final String events = outputPath + "./emissions.xml.gz";
        final String outputFile = outputPath + "./emissions_plot_d1000_20km.csv";

        Config config = ConfigUtils.loadConfig(configFile);
//        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(null);
        config.transit().setTransitScheduleFile(null);
        config.facilities().setInputFile(null);
		config.households().setInputFile(null);
        config.transit().setVehiclesFile(null);
        config.vehicles().setVehiclesFile(null);
        config.network().setInputFile("output_network.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);


        double binSize = 3600*3; // make the bin size bigger than the scenario has seconds
        Network network = scenario.getNetwork();

        EmissionGridAnalyzer analyzer = new EmissionGridAnalyzer.Builder()
                .withGridSize(gridSize)
                .withTimeBinSize(binSize)
                .withNetwork(network)
                .withBounds(createBoundingBox())
                .withSmoothingRadius(smoothingRadius)
                .withCountScaleFactor(scaleFactor)
                .withGridType(EmissionGridAnalyzer.GridType.Hexagonal)
                .build();

        TimeBinMap<Grid<Map<Pollutant, Double>>> timeBins = analyzer.process(events);
        //analyzer.processToJsonFile(events, outputFile + ".json");

        log.info("Writing to csv...");
        writeGridToCSV(timeBins, outputFile);
    }

    private void writeGridToCSV(TimeBinMap<Grid<Map<Pollutant, Double>>> bins, String outputPath) {

        var pollutants = Pollutant.values();

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(outputPath), CSVFormat.TDF)) {

            //print header with all possible pollutants
            printer.print("timeBinStartTime");
            printer.print("x");
            printer.print("y");

            for (var p : pollutants) {
                printer.print(p.toString());
            }
            printer.println();

            //print values if pollutant was not present just print 0 instead
            for (TimeBinMap.TimeBin<Grid<Map<Pollutant, Double>>> bin : bins.getTimeBins()) {
                final double timeBinStartTime = bin.getStartTime();
                for (Grid.Cell<Map<Pollutant, Double>> cell : bin.getValue().getCells()) {

                    printer.print(timeBinStartTime);
                    printer.print(cell.getCoordinate().x);
                    printer.print(cell.getCoordinate().y);
                    for (var p : pollutants) {
                        if (cell.getValue().containsKey(p)) {
                           printer.print(cell.getValue().get(p));
                        } else {
                           printer.print(0);
                        }
                    }

                    printer.println();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Geometry createBoundingBox() {
  /*      return new GeometryFactory().createPolygon(new Coordinate[]{
                new Coordinate(xMin, yMin), new Coordinate(xMax, yMin),
                new Coordinate(xMax, yMax), new Coordinate(xMin, yMax),
                new Coordinate(xMin, yMin)
        });*/
//        //same kind of BoundingBox but using my own shapeFile as the BoundingBox
//        String areaShapeFile = "C:\\Users\\biao.yin\\Documents\\MATSIM\\Project\\qgis\\LesLumièresPleyel\\NoCarZone.shp";
        String areaShapeFile = "C:\\Users\\biao.yin\\Documents\\MATSIM\\Project\\lvmt-matsim\\gis\\paris_20km.shp";  // BYIN: issue: some areas with emission values Nan!!
        Collection<SimpleFeature> features = (new ShapeFileReader()).readFileAndInitialize(areaShapeFile);

        Map<String, Geometry> zoneGeometries = new HashMap<>();
        for (SimpleFeature feature : features) {
            zoneGeometries.put((String)feature.getAttribute("scenario"),(Geometry)feature.getDefaultGeometry());
        }
//        Geometry areaGeometry = zoneGeometries.get(("Pleyel"));
        Geometry areaGeometry = zoneGeometries.get(("paris_20km"));;
        return areaGeometry;

    }
}
