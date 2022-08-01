package org.cloudbus.res.model.storage;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.beans.ConstructorProperties;

@Data
@EqualsAndHashCode(callSuper = true)
public class Battery extends EnergyStorage {
    @ConstructorProperties({"capacity", "currentEnergy"})
    public Battery(double capacity, double currentEnergy) {
        super(StorageType.BATTERY, capacity, currentEnergy);
    }
}
