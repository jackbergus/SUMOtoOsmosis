package org.cloudbus.res.dataproviders.res;

import org.cloudbus.res.model.CloudDatacenter;
import org.cloudbus.res.model.EdgeDatacenter;

import java.util.List;

public class RESResponse {
    public String simulationDate;
    public List<EdgeDatacenter> edgeDatacenters;
    public List<CloudDatacenter> cloudDatacenters;
}
