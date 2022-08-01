package org.cloudbus.res.model.storage;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cloudbus.res.model.pvpanel.PVInstallation;

/**
 * Represents ESU - energy storage unit, the place where excess of energy is being stored
 */
@Data
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Battery.class, name = "BATTERY")
})
public abstract class EnergyStorage {
    private StorageType type;
    private double capacity;
    private double currentEnergy;

    public EnergyStorage(StorageType type, double capacity, double currentEnergy) {
        this.type = type;
        this.capacity = capacity;
        this.currentEnergy = currentEnergy;
    }
}
