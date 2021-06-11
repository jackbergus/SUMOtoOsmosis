package org.cloudbus.res.model.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Battery extends EnergyStorage {

    public Battery(@JsonProperty("capacity") double capacity, @JsonProperty("currentEnergy") double currentEnergy) {
        super(StorageType.BATTERY, capacity, currentEnergy);
    }
}
