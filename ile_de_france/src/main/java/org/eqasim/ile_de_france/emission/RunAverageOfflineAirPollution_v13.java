package org.eqasim.ile_de_france.emission;


import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaRoadTypeSource;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.NonScenarioVehicles;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.*;
import java.io.File;

public class RunAverageOfflineAirPollution_v13 {

	final static String scenarioID = "ile_de_france_1pm";
	final static String inputFilePath = "./ile_de_france/src/main/java/org/eqasim/ile_de_france/emission/input";
	final static String outputPath = "./simulation_output/" + scenarioID;
//	static final String configFile = outputPath + "./output_config.xml";
	static final String eventsFile = outputPath + "./output_events.xml.gz";
	static final String hbefaFileCold =  inputFilePath+ "./EFA_ColdStart_Subsegm_2021ParcAuto_LCA_filter.csv";
	static final String hbefaFileWarm =  inputFilePath+ "./EFA_HOT_Subsegm_2021ParcAuto_LCA_filter.csv";

	static final String emissionEventOutputFileName = outputPath + "./emissions_average.xml.gz";


	static public void main(String[] args) {		// Create config group for emissions
		EmissionsConfigGroup eConfig = new EmissionsConfigGroup();
		eConfig.setWritingEmissionsEvents(true);
		eConfig.setAverageWarmEmissionFactorsFile(hbefaFileWarm);
		eConfig.setAverageColdEmissionFactorsFile(hbefaFileCold);
		eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);
		eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
		eConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.directlyTryAverageTable);
		eConfig.setEmissionsComputationMethod(EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction); // this is added; default is Averagespeed.

		// Load ile-de-France config file and add emissions group
//		Config config = ConfigUtils.loadConfig(configFile);
//		config.network().setInputFile("output_network.xml.gz");
//		config.plans().setInputFile("output_plans.xml.gz");
//		config.facilities().setInputFile(null);
//		config.households().setInputFile(null);
//		config.transit().setTransitScheduleFile(null);
//		config.transit().setVehiclesFile(null);

        // or create a new and simple config file
		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(inputFilePath + "./output_vehicles_modified.xml"); // need change the "defaultVehicleType" to "car" in output_vehicles.file" or see new method for output_vehicle.xml
//		config.network().setInputFile(outputPath+ "./hbefa_network.xml.gz");
		config.network().setInputFile(outputPath+ "./output_network.xml.gz");
		config.plans().setInputFile(outputPath + "./output_plans.xml.gz");
//		config.global().setCoordinateSystem("EPSG:2154");
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(2);
		config.addModule(eConfig);

		// Load scenario and set up events manager
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		//network mapping: way 1-default function
//		OsmHbefaMapping abc = OsmHbefaMapping.build();
//		Network network = scenario.getNetwork();
//		abc.addHbefaMappings(network);
//      new NetworkWriter(network).write(scenarioDirectory+ "./hbefa_network.xml.gz");

		// network settings: way 2: self-definition
		for (Link link : scenario.getNetwork().getLinks().values()) {
			double freespeed;
			freespeed = link.getFreespeed();
			if(freespeed <= 8.34){ //30kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/Access/30");
			} else if(freespeed <= 13.89){ //50kmh
				double lanes = link.getNumberOfLanes();
				if(lanes <= 1.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Local/50");
				} else if(lanes <= 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Distr/50");
				} else if(lanes > 2.0){
					link.getAttributes().putAttribute("hbefa_road_type", "URB/Trunk-City/50");
				} else{
					throw new RuntimeException("NoOfLanes not properly defined");
				}
			} else if(freespeed <= 19.45){ //70kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-City/70");
			} else if(freespeed <= 25){ //90kmh
				link.getAttributes().putAttribute("hbefa_road_type", "URB/MW-Nat./90");
			} else if(freespeed <= 30.6){ //110kmh
				link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/110");
			} else if(freespeed > 30.6){ //faster
				link.getAttributes().putAttribute("hbefa_road_type", "RUR/MW/130");
			} else{
				throw new RuntimeException("Link not considered...");
			}
		}

        // vehicle settings if without vehicle type information from the input vehicle type file, and the settings is cooresponding to network modes settings
		//mode car
		/*Id<VehicleType> carVehicleTypeId = Id.create("car", VehicleType.class);
		VehicleType carVehicleType = scenario.getVehicles().getVehicleTypes().get(carVehicleTypeId);
		EngineInformation carEngineInformation = carVehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory( carEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
		VehicleUtils.setHbefaTechnology( carEngineInformation, "average" );
		VehicleUtils.setHbefaSizeClass( carEngineInformation, "average" );
		VehicleUtils.setHbefaEmissionsConcept( carEngineInformation, "average" );*/

         //add other vehicle settings: mode car_passenger
	/*	Id<VehicleType> carPassengerVehicleTypeId = Id.create("car_passenger", VehicleType.class);
		VehicleType carPassengerVehicleType = scenario.getVehicles().getVehicleTypes().get(carPassengerVehicleTypeId);
		EngineInformation carPassengerEngineInformation = carPassengerVehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory( carPassengerEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
		VehicleUtils.setHbefaTechnology( carPassengerEngineInformation, "average" );
		VehicleUtils.setHbefaSizeClass( carPassengerEngineInformation, "average" );
		VehicleUtils.setHbefaEmissionsConcept( carPassengerEngineInformation, "average" );
*/
		// From here everything is as in the offline emissions contrib example
		// This prepares the emissions module



		AbstractModule module = new AbstractModule(){
			@Override
			public void install(){
				bind( Scenario.class ).toInstance( scenario );
				bind( EventsManager.class ).toInstance( eventsManager );
				bind( EmissionModule.class ) ;
			}
		};

		com.google.inject.Injector injector = Injector.createInjector(config, module );

		// Here we get the emissions module
		EmissionModule emissionModule = injector.getInstance(EmissionModule.class);

		// Here we define where we want to write the new events file with emission events
		EventWriterXML emissionEventWriter = new EventWriterXML( emissionEventOutputFileName );
		emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

		// Here we use the events reader to read in old events
		eventsManager.initProcessing();
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile( eventsFile);
		eventsManager.finishProcessing();

		emissionEventWriter.closeFile();

		new MatsimVehicleWriter( scenario.getVehicles() ).writeFile( outputPath+ "./vehicles_types.xml.gz" );
	}
}
