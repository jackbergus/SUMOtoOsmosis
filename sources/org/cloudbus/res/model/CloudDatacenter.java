package org.cloudbus.res.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.cloudbus.res.model.storage.EnergyStorage;
import org.cloudbus.res.policies.VmAllocationPolicy;

import java.util.List;

@Data
public class CloudDatacenter {
    public String name;
    public String type;
    public VmAllocationPolicy vmAllocationPolicy;
    @JsonProperty("EnergyStorage")
    public List<EnergyStorage> energyStorage;
    @JsonProperty("PowerGrid")
    public List<PowerGrid> powerGrid;
    @JsonProperty("EnergySources")
    public List<RenewableEnergySource> energySources;
}
