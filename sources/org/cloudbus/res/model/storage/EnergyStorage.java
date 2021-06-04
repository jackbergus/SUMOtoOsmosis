package org.cloudbus.res.model.storage;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents ESU - energy storage unit, the place where excess of energy is being stored
 */
@Data
@AllArgsConstructor
public abstract class EnergyStorage {
    private StorageType type;
    private double capacity;
    private double currentEnergy;
}
