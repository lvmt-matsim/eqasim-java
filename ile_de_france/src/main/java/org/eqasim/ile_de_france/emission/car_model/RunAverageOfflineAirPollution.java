package org.eqasim.ile_de_france.emission.car_model;


import org.eqasim.ile_de_france.scenario.OsmHbefaMapping;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

public class RunAverageOfflineAirPollution {
	final static String inputHbefaPath = "./ile_de_france/src/main/java/org/eqasim/ile_de_france/emission/input";
	//final static String inputFilePath = "G:/lvmt_mlannes/simulation_Biao/emission_average_IDF_100pct/60iter";
	//final static String inputFilePath = "E:/lvmt_BY/simulation_output/marjolaine/output_IdF_egt_5pct_2018";
	final static String inputFilePath = "F:/lannesm/output_IdF_25pct_2018";

	static final String eventsFile = inputFilePath + "./output_events.xml.gz";

	static final String hbefaFileCold =  inputHbefaPath+ "./2022_IDF_EFA_ColdStart_Vehcat_Average_OnlyCar_Marjolaine.csv";
	static final String hbefaFileWarm =  inputHbefaPath+ "./2022_IDF_EFA_HOT_Vehcat_Average_OnlyCar_Marjolaine.csv";


	static final String emissionEventOutputFileName = inputFilePath + "./emissions_average_default_vehicle.xml.gz";


	static public void main(String[] args) {		// Create config group for emissions
		EmissionsConfigGroup eConfig = new EmissionsConfigGroup();
		eConfig.setWritingEmissionsEvents(true);
		eConfig.setAverageWarmEmissionFactorsFile(hbefaFileWarm);
		eConfig.setAverageColdEmissionFactorsFile(hbefaFileCold);
		eConfig.setNonScenarioVehicles(NonScenarioVehicles.ignore);
		eConfig.setHbefaRoadTypeSource(HbefaRoadTypeSource.fromLinkAttributes);
		eConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.directlyTryAverageTable);
		eConfig.setEmissionsComputationMethod(EmissionsConfigGroup.EmissionsComputationMethod.StopAndGoFraction); // this is added (more computation); default is Averagespeed.

        // or create a new and simple config file
		Config config = ConfigUtils.createConfig();
		config.vehicles().setVehiclesFile(inputFilePath + "./output_vehicles.xml.gz"); // see new method for output_vehicle.xml in matsim simulation
		config.network().setInputFile(inputFilePath+ "./hbefa_network.xml.gz");
//		config.network().setInputFile(inputFilePath+ "./output_network.xml.gz");
		config.plans().setInputFile(inputFilePath + "./output_plans.xml.gz");
//		config.global().setCoordinateSystem("EPSG:2154");
		config.parallelEventHandling().setNumberOfThreads(null);
		config.parallelEventHandling().setEstimatedNumberOfEvents(null);
		config.global().setNumberOfThreads(2);
		config.addModule(eConfig);

		// Load scenario and set up events manager
		Scenario scenario = ScenarioUtils.loadScenario(config);
		EventsManager eventsManager = EventsUtils.createEventsManager();

		//network mapping: way 1-default function (see RunHbefaMapping)
//		OsmHbefaMapping abc = OsmHbefaMapping.build();
//		Network network = scenario.getNetwork();
//		abc.addHbefaMappings(network);
//        new NetworkWriter(network).write(inputFilePath + "./hbefa_network.xml.gz");

        // vehicle settings if without vehicle type information from the input vehicle type file, and the settings is cooresponding to network modes settings
		// check the output_vehicles.xml to set below the same the vehicle type
		Id<VehicleType> carVehicleTypeId = Id.create("default_car", VehicleType.class);
		VehicleType carVehicleType = scenario.getVehicles().getVehicleTypes().get(carVehicleTypeId);
		EngineInformation carEngineInformation = carVehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory( carEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
		VehicleUtils.setHbefaTechnology( carEngineInformation, "average" );
		VehicleUtils.setHbefaSizeClass( carEngineInformation, "average" );
		VehicleUtils.setHbefaEmissionsConcept( carEngineInformation, "average" );

		Id<VehicleType> carPassengerVehicleTypeId = Id.create("default_car_passenger", VehicleType.class);
		VehicleType carPassengerVehicleType = scenario.getVehicles().getVehicleTypes().get(carPassengerVehicleTypeId);
		EngineInformation carPassengerEngineInformation = carPassengerVehicleType.getEngineInformation();
		VehicleUtils.setHbefaVehicleCategory( carPassengerEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
		VehicleUtils.setHbefaTechnology( carPassengerEngineInformation, "average" );
		VehicleUtils.setHbefaSizeClass( carPassengerEngineInformation, "average" );
		VehicleUtils.setHbefaEmissionsConcept( carPassengerEngineInformation, "average" );
		//mode car
//		Id<VehicleType> carVehicleTypeId = Id.create("car", VehicleType.class);
//		VehicleType carVehicleType = scenario.getVehicles().getVehicleTypes().get(carVehicleTypeId);
//		EngineInformation carEngineInformation = carVehicleType.getEngineInformation();
//		VehicleUtils.setHbefaVehicleCategory( carEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
//		VehicleUtils.setHbefaTechnology( carEngineInformation, "average" );
//		VehicleUtils.setHbefaSizeClass( carEngineInformation, "average" );
//		VehicleUtils.setHbefaEmissionsConcept( carEngineInformation, "average" );

         //add other vehicle settings: mode car_passenger
//		Id<VehicleType> carPassengerVehicleTypeId = Id.create("car_passenger", VehicleType.class);
//		VehicleType carPassengerVehicleType = scenario.getVehicles().getVehicleTypes().get(carPassengerVehicleTypeId);
//		EngineInformation carPassengerEngineInformation = carPassengerVehicleType.getEngineInformation();
//		VehicleUtils.setHbefaVehicleCategory( carPassengerEngineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
//		VehicleUtils.setHbefaTechnology( carPassengerEngineInformation, "average" );
//		VehicleUtils.setHbefaSizeClass( carPassengerEngineInformation, "average" );
//		VehicleUtils.setHbefaEmissionsConcept( carPassengerEngineInformation, "average" );

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

		//new MatsimVehicleWriter( scenario.getVehicles() ).writeFile( inputFilePath+ "./vehicles_types.xml.gz" );
	}
}
