package org.cloudbus.res.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.cloudbus.res.model.storage.EnergyStorage;
import org.cloudbus.res.policies.EnergyAllocationPolicy;
import java.util.List;

@Data
public class EdgeDatacenter {
    private String name;
    private String type;
    @JsonProperty("EnergyAllocationPolicy")
    private EnergyAllocationPolicy energyAllocationPolicy;
    @JsonProperty("EnergyStorage")
    private List<EnergyStorage> energyStorage;
    @JsonProperty("PowerGrid")
    private List<PowerGrid> powerGrid;
    @JsonProperty("EnergySources")
    private List<RenewableEnergySource> energySources;
}
