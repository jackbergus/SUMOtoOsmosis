package org.cloudbus.res.model.storage;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public abstract class EnergyStorage {
    private StorageType type;
    private double capacity;
    private double currentEnergy;
}
