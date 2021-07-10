package org.cloudbus.res.dataproviders.res;

import lombok.Data;
import org.cloudbus.res.config.AppConfig;
import org.cloudbus.res.model.datacenter.CloudDatacenter;
import org.cloudbus.res.model.datacenter.Datacenter;
import org.cloudbus.res.model.datacenter.EdgeDatacenter;

import java.beans.ConstructorProperties;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class RESResponse {
    private LocalDateTime simulationDate;
    private List<Datacenter> datacenters;

    @ConstructorProperties({"simulationDate", "edgeDatacenters", "cloudDatacenters"})
    public RESResponse(String simulationDate, List<EdgeDatacenter> edgeDatacenters, List<CloudDatacenter> cloudDatacenters) {
        this.simulationDate = LocalDateTime.parse(simulationDate, AppConfig.FORMATTER);
        this.datacenters = Stream.of(edgeDatacenters, cloudDatacenters)
                .flatMap(Collection::stream)
                .map(Datacenter::enrich)
                .collect(Collectors.toList());
    }
}