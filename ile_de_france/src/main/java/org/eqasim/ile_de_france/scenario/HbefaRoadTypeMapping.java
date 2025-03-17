package org.eqasim.ile_de_france.scenario;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emissions.EmissionUtils;

/*
*
 * Created by molloyj on 01.12.2017.
*/

abstract class HbefaRoadTypeMapping {

    public void addHbefaMappings(Network network) {
        for (Link link : network.getLinks().values()) {
            String hbefaString = determineHebfaType(link);
            if (hbefaString != null) {
                //EmissionUtils.setHbefaRoadType(link, hbefaString);// should comment in matsim version 12.0
                setHbefaRoadType(link, hbefaString);
            }

        }
    }

    protected abstract String determineHebfaType(Link link);

    static void setHbefaRoadType(Link link, String type) {
        if (type != null) {
            link.getAttributes().putAttribute("hbefa_road_type", type);
        }

    }

}

