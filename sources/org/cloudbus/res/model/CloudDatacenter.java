package org.cloudbus.res.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cloudbus.res.model.storage.EnergyStorage;
import org.cloudbus.res.policies.VmAllocationPolicy;

import java.util.List;

public class CloudDatacenter {
    public String name;
    public String type;
    public VmAllocationPolicy vmAllocationPolicy;
    @JsonProperty("EnergyStorage")
    public List<EnergyStorage> energyStorage;
    @JsonProperty("PowerGrid")
    public List<PowerGrid> powerGrid;
    @JsonProperty("EnergySources")
    public List<EnergySource> energySources;
}
