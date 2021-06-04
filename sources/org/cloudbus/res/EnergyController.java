package org.cloudbus.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.cloudbus.res.model.PowerGrid;
import org.cloudbus.res.model.RenewableEnergySource;
import org.cloudbus.res.model.storage.EnergyStorage;
import org.cloudbus.res.policies.EnergyManagementPolicy;

import java.util.List;

/**
 * EnergyController is class that provides data about edgeDatacenter's characteristics
 * including the policy which manages energy by using it's properties (powerGrids, energyStorages etc.)
 */
@Data
@AllArgsConstructor
public class EnergyController {
    private int edgeDatacenterId;
    private List<RenewableEnergySource> energySources;
    private List<EnergyStorage> energyStorages;
    private List<PowerGrid> powerGrids;
    private EnergyManagementPolicy policy;
}
