package org.cloudbus.res.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.cloudbus.res.model.storage.EnergyStorage;
import org.cloudbus.res.policies.VmAllocationPolicy;

import java.util.List;

@Data
public class CloudDatacenter {
    private String name;
    private String type;
    private VmAllocationPolicy vmAllocationPolicy;
    @JsonProperty("EnergyStorage")
    private List<EnergyStorage> energyStorage;
    @JsonProperty("PowerGrid")
    private List<PowerGrid> powerGrid;
    @JsonProperty("EnergySources")
    private List<RenewableEnergySource> energySources;
}
