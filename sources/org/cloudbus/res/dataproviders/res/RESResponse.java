package org.cloudbus.res.dataproviders.res;

import lombok.Data;
import org.cloudbus.res.config.AppConfig;
import org.cloudbus.res.model.datacenter.CloudDatacenter;
import org.cloudbus.res.model.datacenter.EdgeDatacenter;

import java.beans.ConstructorProperties;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class RESResponse {
    private LocalDateTime simulationDate;
    private List<EdgeDatacenter> edgeDatacenters;
    private List<CloudDatacenter> cloudDatacenters;

    @ConstructorProperties({"simulationDate", "edgeDatacenters", "cloudDatacenters"})
    public RESResponse(String simulationDate, List<EdgeDatacenter> edgeDatacenters, List<CloudDatacenter> cloudDatacenters) {
        this.simulationDate = LocalDateTime.parse(simulationDate, AppConfig.FORMATTER);
        this.edgeDatacenters = edgeDatacenters;
        this.cloudDatacenters = cloudDatacenters;
    }
}