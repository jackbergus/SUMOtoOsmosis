package org.cloudbus.res.model;

import lombok.Data;
import org.cloudbus.res.dataproviders.EnergyData;

/**
 * Abstract class that provides template for renewable energy sources which dataCenter can contain.
 * The getCurrentPower method is responsible for getting information about how much power/energy can energy source
 * give us at certain point of time.
 */
@Data
public abstract class RenewableEnergySource {
    private Location location;
    private EnergySourceType energySourceType;
    private EnergyData energyData;
    // change input depends on energy data implementations
    int getCurrentPower(long timestamp) {
        return energyData.getCurrentPower(timestamp);
    }
}
