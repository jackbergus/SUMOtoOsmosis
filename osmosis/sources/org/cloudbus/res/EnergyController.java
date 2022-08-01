package org.cloudbus.res;

import lombok.Data;
import org.cloudbus.res.model.PowerGrid;
import org.cloudbus.res.model.RenewableEnergySource;
import org.cloudbus.res.model.datacenter.Datacenter;
import org.cloudbus.res.model.storage.EnergyStorage;

import java.time.LocalDateTime;
import java.util.List;

/**
 * EnergyController is class that provides data about edgeDatacenter's characteristics
 * including the policy which manages energy by using it's properties (powerGrids, energyStorages etc.)
 */
@Data
public class EnergyController {
    protected String edgeDatacenterId;
    protected List<RenewableEnergySource> energySources;
    protected List<EnergyStorage> energyStorages;
    protected List<PowerGrid> powerGrids;
    protected EnergyManagementPolicy policy;
    protected Double utilization;

    protected LocalDateTime simulationCurrentTime;

    public static EnergyController fromDatacenter(Datacenter datacenter) {
        EnergyController ec = new EnergyController(
                datacenter.getName(),
                datacenter.getEnergySources(),
                datacenter.getEnergyStorage(),
                datacenter.getPowerGrid(),
                datacenter.getEnergyManagementPolicy(),
                datacenter.getUtilization()
        );

        return ec;
    }

    private EnergyController(String edgeDatacenterId, List<RenewableEnergySource> energySources, List<EnergyStorage> energyStorages, List<PowerGrid> powerGrids, EnergyManagementPolicy policy, double utilization) {
        this.edgeDatacenterId = edgeDatacenterId;
        this.energySources = energySources;
        this.energyStorages = energyStorages;
        this.powerGrids = powerGrids;
        this.policy = policy;
        this.utilization = utilization;
    }

    public void setCurrentTime(LocalDateTime simulationCurrentTime){
        this.simulationCurrentTime = simulationCurrentTime;
    }

    public double getRESCurrentPower(long timestamp) {
        policy.setEnergyController(this);
        return policy.getRESCurrentPower(timestamp);
    }

    public double getRESCurrentPower(LocalDateTime time) {
        policy.setEnergyController(this);
        return policy.getRESCurrentPower(time);
    }

    public double getRESCurrentPower() {
        policy.setEnergyController(this);
        return policy.getRESCurrentPower();
    }

    public double getRESMaximumPower(){
        policy.setEnergyController(this);
        return policy.getRESMaximumPower();
     }
}
