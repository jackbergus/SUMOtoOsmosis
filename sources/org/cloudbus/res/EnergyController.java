package org.cloudbus.res;

import lombok.Data;
import org.cloudbus.res.model.PowerGrid;
import org.cloudbus.res.model.RenewableEnergySource;
import org.cloudbus.res.model.datacenter.Datacenter;
import org.cloudbus.res.model.storage.EnergyStorage;
import org.cloudbus.res.policies.EnergyManagementPolicy;

import java.util.List;

/**
 * EnergyController is class that provides data about edgeDatacenter's characteristics
 * including the policy which manages energy by using it's properties (powerGrids, energyStorages etc.)
 */
@Data
public class EnergyController {
    private String edgeDatacenterId;
    private List<RenewableEnergySource> energySources;
    private List<EnergyStorage> energyStorages;
    private List<PowerGrid> powerGrids;
    private EnergyManagementPolicy policy;

    public static EnergyController fromDatacenter(Datacenter datacenter) {
        return new EnergyController(
                datacenter.getName(),
                datacenter.getEnergySources(),
                datacenter.getEnergyStorage(),
                datacenter.getPowerGrid(),
                datacenter.getEnergyManagementPolicy()
        );
    }

    private EnergyController(String edgeDatacenterId, List<RenewableEnergySource> energySources, List<EnergyStorage> energyStorages, List<PowerGrid> powerGrids, EnergyManagementPolicy policy) {
        this.edgeDatacenterId = edgeDatacenterId;
        this.energySources = energySources;
        this.energyStorages = energyStorages;
        this.powerGrids = powerGrids;
        this.policy = policy;
    }
}
