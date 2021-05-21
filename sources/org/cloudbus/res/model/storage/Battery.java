package org.cloudbus.res.model.storage;

public class Battery extends EnergyStorage {

    public Battery(double capacity, double currentEnergy) {
        super(StorageType.BATTERY, capacity, currentEnergy);
    }
}
