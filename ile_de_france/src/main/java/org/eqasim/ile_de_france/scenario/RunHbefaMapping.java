package org.eqasim.ile_de_france.scenario;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class RunHbefaMapping {
    //final static String inputFilePath = "E:/lvmt_BY/simulation_output/marjolaine/output_IdF_egt_5pct_2018";
    final static String inputFilePath = "F:/lannesm/output_IdF_25pct_2018";

    static public void main(String[] args) {

        Config config = ConfigUtils.createConfig();
		config.network().setInputFile(inputFilePath+ "./output_network.xml.gz");
        // Load scenario and set up events manager
        Scenario scenario = ScenarioUtils.loadScenario(config);

        //network mapping: way 1-default function
        OsmHbefaMapping abc = OsmHbefaMapping.build();
        Network network = scenario.getNetwork();
        abc.addHbefaMappings(network);
        new NetworkWriter(network).write(inputFilePath + "./hbefa_network.xml.gz");

    }

}
