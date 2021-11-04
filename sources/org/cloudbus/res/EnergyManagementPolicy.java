package org.cloudbus.res;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "className",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = OnGridPolicy.class, name = "OnGridPolicy"),
        @JsonSubTypes.Type(value = GridOnlyPolicy.class, name = "GridOnlyPolicy")
})
public abstract class EnergyManagementPolicy {
    protected String className;

    public void setEnergyController(EnergyController energyController) {
        this.energyController = energyController;
    }

    protected EnergyController energyController;

    public abstract void update();

    public abstract double getRESCurrentPower();

    public abstract double getRESCurrentPower(long timestamp);

    public abstract double getRESCurrentPower(LocalDateTime time);

    public abstract double getRESMaximumPower();
}
