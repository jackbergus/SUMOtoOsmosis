package org.cloudbus.res;
import lombok.NoArgsConstructor;
import org.cloudbus.res.model.RenewableEnergySource;

import java.time.LocalDateTime;

@NoArgsConstructor
public class OnGridPolicy extends EnergyManagementPolicy {
    @Override
    public void update() {

    }

    @Override
    public double getRESCurrentPower() {
        double power=0.0;
        for(RenewableEnergySource source:energyController.energySources){
            power += source.getEnergyData().getCurrentPower(energyController.simulationCurrentTime);
        }
        return power;
    }

    @Override
    public double getRESCurrentPower(long timestamp) {
        double power=0.0;
        for(RenewableEnergySource source:energyController.energySources){
            power += source.getEnergyData().getCurrentPower(energyController.simulationCurrentTime);
        }
        return power;
    }

    @Override
    public double getRESCurrentPower(LocalDateTime time) {
        double power=0.0;
        for(RenewableEnergySource source:energyController.energySources){
            power += source.getEnergyData().getCurrentPower(time);
        }
        return power;
    }

    @Override
    public double getRESMaximumPower() {
        double power=0.0;
        for(RenewableEnergySource source:energyController.energySources){
            power += source.getPeakPower();
        }
        return power;
    }
}
