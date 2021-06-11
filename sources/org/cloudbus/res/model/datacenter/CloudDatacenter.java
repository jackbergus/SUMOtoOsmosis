package org.cloudbus.res.model.datacenter;

import lombok.Data;
import org.cloudbus.res.model.PowerGrid;
import org.cloudbus.res.model.RenewableEnergySource;
import org.cloudbus.res.model.storage.EnergyStorage;
import org.cloudbus.res.policies.EnergyManagementPolicy;
import java.util.List;

@Data
public class CloudDatacenter {
    private String name;
    private String type;
    private EnergyManagementPolicy energyManagementPolicy;
    private List<EnergyStorage> energyStorage;
    private List<PowerGrid> powerGrid;
    private List<RenewableEnergySource> energySources;
}
