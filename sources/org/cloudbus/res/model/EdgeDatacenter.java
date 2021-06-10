package org.cloudbus.res.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.cloudbus.res.model.storage.EnergyStorage;
import org.cloudbus.res.policies.EnergyAllocationPolicy;
import java.util.List;

@Data
public class EdgeDatacenter {
    public String name;
    public String type;
    @JsonProperty("EnergyAllocationPolicy")
    public EnergyAllocationPolicy energyAllocationPolicy;
    @JsonProperty("EnergyStorage")
    public List<EnergyStorage> energyStorage;
    @JsonProperty("PowerGrid")
    public List<PowerGrid> powerGrid;
    @JsonProperty("EnergySources")
    public List<EnergySource> energySources;
}
