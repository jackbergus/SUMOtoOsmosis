package org.cloudbus.res.dataproviders.pvgis;

import org.cloudbus.res.model.CloudDatacenter;
import org.cloudbus.res.model.EdgeDatacenter;

import java.util.List;

public class PVGisResponse {
    public String simulationDate;
    public List<EdgeDatacenter> edgeDatacenters;
    public List<CloudDatacenter> cloudDatacenters;
}
